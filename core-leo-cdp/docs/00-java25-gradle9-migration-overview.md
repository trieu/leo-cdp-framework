# Migration Plan — Java 11 → Java 25 (LTS) & Gradle 6.9.4 → Gradle 9.x

**Status:** PLAN (not started)
**Scope:** `core-leo-cdp` module only (other monorepo modules are independent)
**Date:** 2026-06-06
**Plan documents:**

| Doc | Content |
|---|---|
| [00-java25-gradle9-migration-overview.md](00-java25-gradle9-migration-overview.md) | This file — goals, phases, risk register, timeline |
| [01-gradle-9-migration.md](01-gradle-9-migration.md) | `build.gradle` rewrite, plugin replacements, wrapper, config cache |
| [02-java-25-migration.md](02-java-25-migration.md) | JDK toolchain, runtime JVM flags, API/bytecode strategy |
| [03-dependency-compatibility-matrix.md](03-dependency-compatibility-matrix.md) | Per-dependency compatibility & upgrade table |
| [04-runtime-and-deployment-changes.md](04-runtime-and-deployment-changes.md) | Dockerfile, CI workflow, devops shell scripts |
| [05-testing-and-rollout-plan.md](05-testing-and-rollout-plan.md) | Validation gates, soak testing, rollback |

---

## 1. Why migrate

- **Java 11** Premier support ended (Sep 2023); Corretto 11 is in maintenance. **Java 25 is the current LTS** (GA Sep 2025, Premier support to ~2030) — virtual threads, generational ZGC, compact object headers (`-XX:+UseCompactObjectHeaders`, a real memory win for a profile-heavy CDP), FFM API.
- **Gradle 6.9.4** (2022) cannot run on JDK ≥ 17, blocks modern plugin versions, has no configuration cache, and forces the legacy `maven` plugin workarounds.
- Security posture: staying current unblocks future dependency CVE fixes (many libs have dropped Java 11 support in current lines).

## 2. Current state (verified by code audit, 2026-06-06)

| Aspect | Current |
|---|---|
| Language/bytecode | Java 11 (`sourceCompatibility = 11`), Amazon Corretto 11 in prod & Docker |
| Build | Gradle **6.9.4**, system-installed, **no wrapper in repo** |
| Legacy plugins | `maven` (removed in Gradle 7), `org.gradlewebtools.minify` **1.3.2** (latest is **2.1.1**) |
| Gradle-9-breaking script constructs | `apply plugin: 'maven'` + `uploadArchives`, `baseName` in 5 Jar tasks, `${buildDir}`, config-time resolution of `runtimeClasspath` in `getClasspathStringJars()`, config-time `copy {}` in `CopyDevOpsScriptToBUILD`, Groovy `Date.format()` |
| Biggest runtime risk | **Vert.x 3.8.5 → Netty 4.1.44 (Jan 2020)**: pre-dates JDK strong encapsulation and `sun.misc.Unsafe` restrictions |
| Vendored jars (`ext-lib/`) | `rfx-core-1.0.jar` (bytecode 55/Java 11, links against Vert.x 3 + Netty + Jedis + Kafka APIs, **no source in repo**), `query-builder-2.1.1.jar` (bytecode 52), `zalo-java-sdk` (bytecode 51) |
| Source audit | **Clean**: no `sun.misc.*`, `jdk.internal.*`, `javax.xml.bind`, Nashorn, `SecurityManager`, `finalize()`, or deprecated boxing constructors anywhere in `src/`. Only `com.sun.management.OperatingSystemMXBean` (`SystemSnapshot.java:7`) — a *supported* exported JDK API, no change needed. Vendored jars are equally clean of JDK-internal references. |
| CI | `.github/workflows/ci-cd.yml` pins Corretto 11 + Gradle 6.9.4 |

**Good news:** the application source itself is remarkably migration-friendly. The work is concentrated in (a) the build script, (b) old dependencies (Vert.x/Netty above all), and (c) deployment plumbing.

## 3. Target state

| Aspect | Target |
|---|---|
| JDK | Amazon Corretto **25** everywhere (build, CI, Docker, prod) |
| Gradle | **9.x latest (≥ 9.1.0 — 9.0.0 only supports up to Java 24; Java 25 requires 9.1.0+)**, committed `gradlew` wrapper |
| Bytecode | Staged: `release = 11` first → raise to `25` in Phase 4 |
| JVM runtime flags | `--sun-misc-unsafe-memory-access=allow`, `--enable-native-access=ALL-UNNAMED`, targeted `--add-opens` (see doc 02) |
| Publishing | `maven` plugin / `uploadArchives` deleted (dead code — `flatDir 'repos'` is unused) |
| JS minification | minify plugin 2.1.1 if Gradle-9 compatible, else custom task using the already-present `closure-compiler` dependency |

## 4. Phased roadmap

Strategy: **decouple the two risky axes.** Never change the build tool and the runtime JDK in the same step. Bytecode target is raised last, because it is the only irreversible step for deployed artifacts.

### Phase 0 — Baseline & safety net (≈ 2–3 days)
1. Add the Gradle wrapper **at 6.9.4** first (`gradle wrapper --gradle-version 6.9.4`) and switch CI/Docker to `./gradlew` — makes every later version bump a one-line, revertible change.
2. Record a baseline: `gradle AutoBuildForDeployment` output tree, jar manifests, minified JS checksums, CI green run.
3. Tag the repo (`pre-jdk25-migration`).

### Phase 1 — Gradle 6.9.4 → 9.x, still producing Java 11 bytecode (≈ 1–1.5 weeks)
- Step through majors to surface deprecations: `6.9.4 → 7.6.4 → 8.14.x → 9.x`, running `./gradlew AutoBuildForDeployment --warning-mode all` at each stop. The 7.x stop is where the `maven`-plugin removal and `baseName` removal bite; fix there.
- Full `build.gradle` rewrite details in **doc 01**.
- Gradle 9 itself requires JDK 17+ to run → run the daemon on JDK 25, compile with `options.release = 11` (no JDK 11 toolchain needed).
- **Exit gate:** byte-identical-equivalent build output vs Phase 0 baseline (same jars, same `Class-Path` manifests, same deps/ content, JS minification output reviewed).

### Phase 2 — Runtime JDK 25, Java 11 bytecode (≈ 1–2 weeks incl. soak)
- Run all five starters on Corretto 25 with the flag set from **doc 02** in a staging environment.
- This is where Vert.x 3.8.5/Netty 4.1.44 is proven or disproven. Contingency ladder (doc 03 §Vert.x): flags → bump Vert.x 3.9.16 → pin newer Netty 4.1.12x → (last resort, separate project) Vert.x 4/5 migration, blocked on `rfx-core` source.
- Update Dockerfile, CI, devops start scripts (**doc 04**).
- **Exit gate:** k6 load suite (`tests_with_k6/`) results within tolerance of Java 11 baseline; 72 h staging soak with no Netty/buffer/reflection errors in logs.

### Phase 3 — Production rollout on JDK 25 (≈ 1 week, service-by-service)
- Rollout order (lowest blast radius first): `UploadFileHttpStarter` → `ScheduledJobStarter` → `DataProcessingStarter` → `DataObserverStarter` → `MainHttpStarter`.
- Rollback per service = restart on Corretto 11 (bytecode is still 11 — instant rollback).

### Phase 4 — Raise bytecode to Java 25 + modernization (≈ 1 week + ongoing)
- Flip `options.release = 25`, drop Java-11 compatibility. Only after Phase 3 has soaked.
- Opportunistic dependency bumps (doc 03), enable `-XX:+UseCompactObjectHeaders`, evaluate virtual threads for `ScheduledJobStarter`/connector jobs (NOT for the Vert.x event loop — Vert.x 3 predates Loom integration).
- Adopt records/pattern-matching/text blocks gradually in new code only.

## 5. Risk register (top items)

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | **Vert.x 3.8.5 / Netty 4.1.44 fails or degrades on JDK 25** (Unsafe restrictions, `java.nio` reflection) | Medium-High | High | JVM flag set (doc 02); contingency ladder in doc 03; Phase 2 soak before prod |
| R2 | `org.gradlewebtools.minify` 2.1.1 incompatible with Gradle 9 or output differs from 1.3.2 | Medium | Medium | Fallback custom task using existing `closure-compiler` dep; checksum-compare minified JS (it is committed + CDN-published via jsDelivr — diffs are user-visible) |
| R3 | `rfx-core-1.0.jar` has no source in repo; blocks any Vert.x 4/5 or Jedis-major future upgrade | High (already true) | Medium (only for Phase-4+ ambitions) | Locate/recover rfx source (it links Vert.x 3 APIs); treat Vert.x 4 migration as a separate future project |
| R4 | Gradle Groovy 3→4 breaks script idioms (`Date.format()`, dynamic props) | Medium | Low | Replace with `java.time` in build script (doc 01); caught at Phase 1 exit gate |
| R5 | Old deps misbehave on 25 only under load (Kafka snappy/zstd JNI, sqlite-jdbc, Gson reflection) | Low-Medium | Medium | `--enable-native-access`/`--add-opens` flags; k6 load test gate; targeted bumps in doc 03 |
| R6 | CDN consumers break from re-minified `leo.observer.min.js` (new Closure Compiler version) | Low | High (every tracked site) | Diff + manual QA of observer script on a test site before pushing static output |

## 6. Effort summary

| Phase | Calendar estimate |
|---|---|
| 0 — Baseline | 2–3 days |
| 1 — Gradle 9 | 1–1.5 weeks |
| 2 — JDK 25 runtime (staging) | 1–2 weeks |
| 3 — Prod rollout | 1 week |
| 4 — Bytecode 25 + modernization | 1 week + ongoing |
| **Total** | **≈ 4–6 weeks elapsed** (single engineer, part-time interleaved) |

## 7. Out of scope

- Vert.x 4/5 migration (separate project; tracked as R3 prerequisite work).
- Other monorepo modules (`airflow-ai-agent`, `core-customer360`, `chrome-ext`) — unaffected by JVM/Gradle changes.
- Switching build to Kotlin DSL (optional nicety; Groovy DSL remains fully supported in Gradle 9).

## Sources

- [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [Gradle 9.1.0 Release Notes — Java 25 support](https://docs.gradle.org/9.1.0/release-notes.html)
- [Upgrading to Gradle 9.0.0](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html)
- [Netty: Java 24 and sun.misc.Unsafe](https://netty.io/wiki/java-24-and-sun.misc.unsafe.html)
- [gradle-minify-plugin (Plugin Portal)](https://plugins.gradle.org/plugin/org.gradlewebtools.minify)
