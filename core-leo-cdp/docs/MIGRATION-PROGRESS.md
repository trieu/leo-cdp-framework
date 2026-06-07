# Migration Progress Report — Java 25 / Gradle 9

Tracks execution of the [migration plan](00-java25-gradle9-migration-overview.md). Updated as work lands.
Companion documents: [MIGRATION-EXECUTION-REPORT.md](MIGRATION-EXECUTION-REPORT.md) (full narrative) · [PERFORMANCE-TEST-REPORT-JDK25.md](PERFORMANCE-TEST-REPORT-JDK25.md) (detailed perf + memory study, 1M+ requests).

**Last updated:** 2026-06-06
**Working branch:** `liem/java25-gradle9-migration` (off `liem/setup-local`)
**Execution environment:** local Windows dev box (Microsoft OpenJDK 21 default; JDK 25.0.2 at `~/.jdks/ms-25.0.2`; JDK 11 + Gradle 6.9.4 provisioned for baseline)

## Status dashboard

| Phase | Step | Status | Gate | Notes |
|---|---|---|---|---|
| 0 | 0a — Provision JDK 11 + Gradle 6.9.4 | ✅ done | — | Temurin 11.0.31 + Gradle 6.9.4 portable under `C:\Users\dvtliem\AI\tools\` |
| 0 | 0b — Baseline build + artifact capture | ✅ done | — | `AutoBuildForDeployment` green in 8m30s; artifacts in `docs/migration-baseline/gradle694-*` (bytecode major 55 confirmed) |
| 0 | 0c — Wrapper @6.9.4, branch, tag | ✅ done | — | Tag `pre-jdk25-migration` |
| 1 | 1a — Gradle 7.6.4 (`maven` plugin removal) | ✅ done | — | Commit `d31df54`; also fixed I2 (typed-attribute collision) |
| 1 | 1b — Gradle 8.14.x (`baseName`, `buildDir`) | ✅ done | — | Commit `0ba9cf9`; build green in 10m51s |
| 1 | 1c — Gradle 9.1.0 full rewrite | ✅ done | **G1 PASS** | Commit `ab69e4f`. Build green on 9.1.0 + JDK 25 daemon (4m52s). G1: tree+deps identical to baseline; bytecode major 55; Class-Path intact (lazy doFirst); manifest diff = intended Main-Class trim only. ⚠ 4/19 minified JS differ (Closure bump in plugin 2.1.1) → **QA before CDN push** (R6). 2 warnings remain (`Task.project` at execution time — config-cache stretch goal, Gradle 10 horizon) |
| 2 | Code: JVM flags, Dockerfile, CI | ✅ done | — | Shared `jvm-params.sh` sourced by 3 start scripts; Dockerfile → corretto:25 + wrapper + `JDK_JAVA_OPTIONS`; ci-cd.yml → JDK 25 + wrapper + bytecode-55 guard step; build.sh → `./gradlew` |
| 2 | Local JDK-25 boot smoke | ✅ done | — | `leo-main-starter` boots on JDK 25 (and 11) with compat flags; fails only at missing `leocdp-metadata.properties` — expected without a deploy environment |
| 2 | R6 — minified-JS QA (static) | ✅ done | — | Tracker `leo.observer.min.js` **byte-identical** (the build never re-minifies it — `minifyJsLeoObserver` is a separate task; QA needed only before a future CDN re-publish). The 4 changed admin files: parse-clean (`node --check`), semantic diff = **2 real lines** (equivalent regex→`RegExp()` Closure transform + stale-artifact catch-up "AI ChatBot"→"AI Assistant"); the rest is line endings |
| 2 | G2-local — Corretto-25 container boot + UI | ✅ done | — | New Dockerfile built end-to-end (`jdk25-local`, 752MB); `leocdp-admin` swapped to it: Corretto **25.0.3**, `JDK_JAVA_OPTIONS` picked up, **zero** JDK-25 failure signatures through boot; Vert.x/Netty serving on :9070; login-page console identical to Corretto-11 reference (same 2 pre-existing errors). **R1 contingency-ladder step 1 (flags only) validated at boot.** Rollback: `LEOCDP_TAG=5f688f0 docker compose up -d leocdp-admin` |
| 2 | G2-local — k6 A/B (Corretto 11 vs 25) | ✅ parity (noise) | — | Initial single run suggested −15% RPS/+75% p95 on 25; **interleaved 3-round × 2-side protocol (150 s steady, ~210k reqs, 0 errors) disproved it** — same-side variance ±40%, and by medians JDK 25 is slightly ahead (422.7 vs 383.4 rps; p95 303 vs 355 ms). Verdict: parity within local noise (I6); the precise ±10% gate runs on staging hardware. Harness: `tests_with_k6/admin_http_ab_test.js`; raw data `tests_with_k6/out/round*-jdk*.json`. Full narrative: [MIGRATION-EXECUTION-REPORT.md](MIGRATION-EXECUTION-REPORT.md) §7 |
| 2 | G2-local — k6 A/B: JDK 25 + `-XX:+UseCompactObjectHeaders` vs JDK 11 | ✅ 25+COH leads | — | Second interleaved 3-round batch (quieter host, tighter variance): **medians 504.5 vs 470.2 rps (+7%), p95 144 vs 184 ms (−22%)** in favor of 25+COH; 0 errors (one round 0.38%, below 1% threshold). Same direction as the plain-25 batch → two independent batches both lean JDK 25. COH flag verified active via `jcmd VM.flags`; JDK 11 cannot run this flag at all. Memory (identical load): **JDK 11 = 412.7 MiB, JDK 25 = 289.2 MiB (−30%)**, 25+COH = 297.3 MiB (COH neutral at small live-set; real test = data pipeline on staging). Raw: `tests_with_k6/out/round*-jdk{11b,25coh}.json`. Verdict: [MIGRATION-EXECUTION-REPORT.md](MIGRATION-EXECUTION-REPORT.md) §7.3 |
| 2 | G2-local — authenticated UI walkthrough + soak | ⬜ pending | — | Needs admin creds (+captcha); soak monitor optional |
| 2 | **Staging soak (72 h) + k6 — the real gate** | ⛔ blocked: needs staging env | **G2** | Local results above de-risk but do not replace it |
| 3 | Prod canary rollout | ⛔ blocked: after G2 | **G3** | |
| 4 | Bytecode → 25 (`options.release = 25`) — **Wave 0 executed on this branch** | ✅ done (branch) | **G4** | Commit `2e47813`: release=25, CI guard 69, compile green zero source changes, `jdk25-bc69` image boots clean. **k6 batch 3: bc69 swept all 3 rounds vs bc55 on the same JVM** (+8% rps, −23% med latency, memory parity). Branch artifacts now need Java 25 runtime; bc-55 rollback anchor = `d64612b`. Code modernization waves 1–3 (records/idioms/virtual threads): [06-java25-code-modernization-plan.md](06-java25-code-modernization-plan.md) |
| 4 | Wave 1 — mechanical 21+/25 idioms (OpenRewrite, curated) | ✅ done | W1 | Commit `c6b5c5a`: 89 files (+241/−197) — instanceof patterns, `getFirst/getLast`, unnamed `_`, text blocks, `Path.of`, `@Serial`. **Rejected** from the composite: instance-main rewrite, `IO.println` churn, wrapper auto-bump (rationale in `rewrite-init.gradle`). Compile + boot smoke green |
| 4 | Wave 2a — Gson 2.13.2 + first records | ✅ done | — | Commit `16546b9` (+21/−126): Gson bump (record-serde enabler; usage surface audited clean), `SentimentAnalysisResult/Params` + `TouchpointFlowReportCacheKey` → records. Finding: record candidates scarcer than planned — most small classes are enums/services or serialization-bound (no-go per triage); Wave 2b = per-class review of remaining nested carriers |
| 4 | Consolidation — image rebuild with Waves 1+2a + verification | 🔄 in progress | — | `jdk25-bc69` rebuilding from `34a6f20`; then container swap, boot-log scrub, Playwright login/console check, one k6 sanity round vs the bc69 band (~440–550 rps) |
| 4 | Wave 2b — remaining record candidates · Wave 3 — virtual threads | ⬜ pending | W2/W3 | Per docs/06; Wave 3 targets `ScheduledJobStarter`/connector blocking paths, never the Vert.x event loop |

Legend: ✅ done · 🔄 in progress · ⬜ pending · ⛔ blocked (external dependency)

## Gate results

### G1 — Build equivalence (Gradle 9 vs 6.9.4 baseline)
_not yet run_

### G2 — Staging on Corretto 25
_blocked: requires staging environment with ArangoDB/Redis/Kafka/PostgreSQL_

### G3 / G4
_blocked on G2/G3_

## Decisions & deviations log

| Date | Decision | Why |
|---|---|---|
| 2026-06-06 | Use portable Temurin 11 zip + Gradle 6.9.4 dist instead of system installs | No admin footprint; baseline tooling is throwaway after Phase 1 |
| 2026-06-06 | Run Gradle 9 daemon on local JDK 21 (not 25) for Phase 1; JDK 25 used for runtime smoke and toolchain | Gradle 9 needs ≥17; bytecode pinned by `options.release=11` regardless of daemon JDK |

## Issues encountered

| # | Issue | Resolution |
|---|---|---|
| I1 | Committed minified JS under `common-resources-min/` was **stale** vs current source: baseline rebuild produced real content diffs (beyond the volatile version header) in `leocdp.chatbot.js` (2 lines), `leocdp.core-admin.js` (1), `leocdp.finance.js` (1), `leocdp.router.js` (4). Pre-existing condition — someone edited source JS without re-running minify. | Working-tree churn reverted; G1 compares **post-build vs post-build** state (`gradle694-js-sha256-noversion.txt`), so the gate is unaffected. Flag to maintainers: recommit fresh minified JS after migration lands. |
| I2 | **Gradle 7.x hard failure (undocumented in plan):** the Gradle-6 string-typed `org.gradle.jvm.environment` Guava workaround collides with Gradle 7+'s built-in typed `TargetJvmEnvironment` ("Cannot have two attributes with the same name but different types"). | Replaced with the typed API (doc 01 §3.9) at Phase 1a instead of 1c. Commit `d31df54`. |
| I3 | **Gradle 9 hard failure:** `attributes()` inside `configurations.all` is now illegal on declarable configurations (`compileOnly` etc.) — "Method call not allowed ... permitted usage(s): Declarable". | Gated the attribute block on `it.canBeResolved` (initially also consumable; tightened to resolvable-only after `:archives` deprecation) in `configurations.configureEach`; `exclude` stays unconditional. |
| I4 | `./gradlew wrapper --gradle-version 9.1.0` failed under JDK 25 ("Unsupported class file major version 69") because the **old** Gradle 8.14.3 executes the wrapper task and caps at Java 24. | Ran the bump under default JDK 21, then switched the daemon to JDK 25 for Gradle 9.1.0. |
| I5 | Docker build failed in <1s at the `./gradlew` step: **"xargs is not available"** — the gradlew launcher needs `xargs`, and minimal AL2023-based `amazoncorretto:25` ships no `findutils` (the old corretto:11 Dockerfile's `yum install unzip tar gzip` had masked this). | `RUN yum install -y findutils` added to the build stage. |
