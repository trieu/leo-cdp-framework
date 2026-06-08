# Branch Change Report — `liem/java25-gradle9-migration` vs `main`

**Generated:** 2026-06-08 · **Merge-base:** `b8dbcb2` · **Head:** `8eb802b`
**Scope:** `197 files changed, +15,935 / −479` across 59 commits.

A one-page summary of everything this branch changes relative to `main`. Detail lives in [MIGRATION-PROGRESS.md](MIGRATION-PROGRESS.md), [MIGRATION-EXECUTION-REPORT.md](MIGRATION-EXECUTION-REPORT.md), and [PERFORMANCE-TEST-REPORT-JDK25.md](PERFORMANCE-TEST-REPORT-JDK25.md).

## What this branch does

Migrates `core-leo-cdp` from **Java 11 + Gradle 6.9.4** to **Java 25 (LTS) + Gradle 9.1.0**, then modernizes the source to Java 21+/25 idioms — keeping the build green and the app verified against a live database at every step.

## Headline changes

| Area | `main` | this branch |
|---|---|---|
| Build tool | Gradle 6.9.4 (system install, no wrapper) | **Gradle 9.1.0** (committed `gradlew` wrapper) |
| Runtime / build JDK | Corretto 11 | **Corretto 25** |
| Bytecode target | Java 11 (major 55) | **Java 25 (major 69)** (`options.release = 25`) |
| Legacy `maven` plugin + `uploadArchives` | present | **removed** (gone in Gradle 7) |
| JS minify plugin | 1.3.2 | 2.1.1 |
| Gson | 2.9.1 | **2.13.2** (record-capable serde) |
| JVM run flags | `-server`, CompressedOops… | JDK-25 set (`--sun-misc-unsafe-memory-access=allow`, `--enable-native-access`, targeted `--add-opens`) |

## Key Performance Indicators (KPI)

Baseline = `main` (Corretto 11, bytecode 55). Target = this branch (Corretto 25, bytecode 69). Runtime numbers are medians of interleaved k6 A/B rounds on the admin worker (~1.4M total requests); full method + raw data in [PERFORMANCE-TEST-REPORT-JDK25.md](PERFORMANCE-TEST-REPORT-JDK25.md).

| KPI | Baseline (JDK 11) | This branch (JDK 25) | Δ | Goal |
|---|---|---|---|---|
| Throughput (req/s, median) | 383–470 | 423–505 | **+7 … +10 %** | ↑ |
| Latency p95 | 184–355 ms | 144–303 ms | **−15 … −22 %** | ↓ |
| Latency median | 73–108 ms | 59–90 ms | **≈ −20 %** | ↓ |
| Container RSS (same load) | 412.7 MiB | 289.2 MiB | **−30 %** | ↓ |
| Error rate (≈1.4M reqs) | 0 % | 0 % | **=** | 0 % |
| Bytecode-69 vs -55 (same JVM) | — | +8.3 % RPS / −23 % med lat | swept 3/3 rounds | ↑ |
| Build status | green | green | maintained | green |
| CI pipeline | green | **green** | maintained | green |
| Unit tests | 12/12 | **12/12** | = | 100 % |
| Integration tests (live DB) | (not run) | **60/66** | migration paths 100 % | ↑ |
| Class-file equivalence (gate G1) | — | tree+deps identical | pass | pass |
| Image size | 670 MB | 752 MB | +12 % | (acceptable) |
| Java LTS currency | 11 (2018) | 25 (2025) | +14 yrs | current LTS |

**Headline:** JDK 25 is **faster on every measured axis and uses ~30 % less memory** for the same workload — the memory cut is the most reproducible win and directly lowers container memory limits. The only KPI moving the "wrong" way is image size (+82 MB from the newer base), which is expected and acceptable.

> Caveat: runtime KPIs are laptop-grade (Rancher Desktop/WSL2); the formal ±10 % certification belongs to staging hardware (gate G2). Direction was consistent across two independent batches and never favored JDK 11.

## Change groups

- **Build (`build.gradle`, +164/−52):** wrapper to 9.1.0; `options.release=25`; typed `TargetJvmEnvironment`; `archiveBaseName`; `layout.buildDirectory`; lazy manifest `Class-Path`; Groovy-4 `java.time` timestamp; configuration-role-safe attribute pin; `useJUnitPlatform()` + unit/integration split (`integrationTest`, `seedDefaultData` tasks).
- **Source modernization (78 `src/main/java` files):** Wave 1 mechanical idioms via curated OpenRewrite (instanceof patterns, `getFirst/getLast`, text blocks, `Path.of`, `@Serial`); Wave 2 records (`SentimentAnalysis*`, `TouchpointFlowReportCacheKey`, `ProductImportingEvent`); Wave 3 virtual threads on 5 blocking export/query fan-outs.
- **Deployment (`Dockerfile` +61, `shell-script-starter/*`, CI `ci-cd.yml` +337):** Corretto-25 images, `JDK_JAVA_OPTIONS`, shared `jvm-params.sh`, `findutils` + retried/ordered wrapper-download layer, CI on JDK 25 with a bytecode-69 guard step.
- **Tests (19 `src/test` files):** unit/integration split, `@BeforeAll` static fix, `@TestMethodOrder` fixes, self-seeded `TestNotification`.
- **Docs & evidence:** `docs/00–06` plan, three reports, `migration-baseline/` (G1 artifacts), `tests_with_k6/` harness + raw A/B/memory data.

## Verification (local)

- **Build:** green on Gradle 9.1.0 / JDK 25; gate G1 output byte-equivalent to the 6.9.4 baseline.
- **CI:** **green** (`docker` job builds end-to-end + publishes JUnit report).
- **Performance vs JDK 11:** **+7–10% throughput, −15–22% p95, −30% memory**, 0 errors over ~1.4M k6 requests; bytecode-69 swept all rounds vs bytecode-55.
- **Tests:** unit **12/12 green** (CI gate); integration **60/66** against live ArangoDB — all migration-sensitive paths green.

## Not done / out of scope (tracked in MIGRATION-PROGRESS.md)

- 6 integration tests red — **pre-existing test defects** (null DAO save, missing teardown/409, unsatisfiable `≥10` assertion, hardcoded IDs, Redis timing), not migration regressions.
- Staging gate **G2** (72 h soak + Kafka/PG/Airflow + scale data) and the authenticated admin-UI walkthrough — require environments unavailable locally.
- Branch artifacts now **require a Java 25 runtime**; last Java-11-bytecode commit = `d64612b`.
