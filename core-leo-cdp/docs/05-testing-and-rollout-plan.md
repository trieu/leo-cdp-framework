# 05 — Testing, Validation & Rollout Plan

Part of the [Java 25 / Gradle 9 migration plan](00-java25-gradle9-migration-overview.md). Defines the **exit gates** referenced by every phase.

## 1. Constraint: the test suite is mostly manual integration probes

Per CLAUDE.md, `src/test/java/test/**` is largely runnable utilities with `main()` methods requiring **live ArangoDB + Redis** — `gradle test` alone is *not* a sufficient gate. The plan therefore leans on: (a) build-output diffing, (b) an E2E smoke script against a provisioned staging stack, (c) the `tests_with_k6/` load suite, (d) staged soak.

> Side-task worth doing during Phase 0: tag the few true unit tests (e.g. `test.cdp.profile.ProfileMergeServiceTest`) with a JUnit `@Tag("unit")` so CI can run a deterministic subset — this directly fills CI's existing `TODO: gradle integrationTest once tests are split` line.

## 2. Phase 0 — Baseline capture (everything later is diffed against this)

```bash
# on Gradle 6.9.4 / Corretto 11
./gradlew AutoBuildForDeployment
```
Capture into `docs/migration-baseline/` (or CI artifacts):
- [ ] `find <buildOutput> -type f | sort` — output tree listing
- [ ] `unzip -p <each starter jar> META-INF/MANIFEST.MF` — manifests (Main-Class, Class-Path)
- [ ] `ls <buildOutput>/deps | sort` — runtime jar set
- [ ] `sha256sum public/js/leo-observer/*.js resources/app-templates/leocdp-admin/common-resources-min/**/*.js` — minified JS checksums
- [ ] Boot each starter on Corretto **11**, save first 200 log lines — including any `Illegal reflective access` warnings (these predict the JDK-25 `--add-opens` list)
- [ ] k6 run on staging → store latency p50/p95/p99, RPS, error rate, RSS, GC pause stats

## 3. Per-phase gates

### Gate G1 (after Gradle 9 rewrite — doc 01 §5)
Build equivalence on unchanged runtime:
- [ ] Output tree, deps set, manifests match baseline (modulo timestamp in `Implementation-Version`)
- [ ] Minified JS: byte-identical, or diff reviewed + tracker QA (R6)
- [ ] All five starters boot on **Corretto 11** and serve their health endpoints
- [ ] `./gradlew test` runs same test count as baseline; `--warning-mode all` deprecation-free
- [ ] CI green with wrapper

### Gate G2 (staging on Corretto 25 — doc 02 §6)
Functional E2E checklist (maps to the five starters + cross-module flow):

| Area | Check |
|---|---|
| Admin (`MainHttpStarter`) | Login (incl. Keycloak SSO path), profile list/search, segment build (query-builder AQL), campaign CRUD, dashboard/Analytics360 renders |
| CLI mode | `setup-system-with-password`, `upgrade-system`, `upgrade-index-database` against a scratch ArangoDB |
| Observer (`DataObserverStarter`) | `leo.observer.js` event POST → event visible in ArangoDB; cookie/UUID handling; geo lookup (GeoIP2 mmdb load) |
| Scheduler (`ScheduledJobStarter`) | Quartz jobs fire (segment refresh, scoring); data-connector job completes |
| Data processing (`DataProcessingStarter`) | Kafka produce/consume incl. snappy/zstd-compressed payloads |
| Uploader (`UploadFileHttpStarter`) | Multipart upload, image thumbnail (thumbnailator/jai-imageio), QR code gen |
| Cross-module | Redis Pub/Sub `agent_pubsub_queue` message triggers Airflow DAG; result lands in PostgreSQL/pgvector |
| Outbound TLS | SMTP send, Google Sheets/Drive/GCS connector, Zalo webhook (TLS 1.3 defaults — doc 02 §4) |
| Log scrub (72 h) | zero `InaccessibleObjectException`, `IllegalAccessError`, `UnsupportedOperationException` from Unsafe paths; native-access warnings only for known/flagged libs |

Performance:
- [ ] k6 suite within ±10% of baseline (p95 latency, RPS, error rate)
- [ ] RSS + direct-buffer usage stable over soak (`jcmd <pid> VM.native_memory summary` trend)
- [ ] GC pause profile comparable (G1 default on both; don't tune until after parity is shown)

### Gate G3 (production, per service)
- [ ] Canary order: uploader → scheduler → data-processing → observer → admin (blast-radius ascending)
- [ ] 24 h bake per service before the next
- [ ] Dashboards: error rate, event-ingest throughput, segment-job duration, heap/RSS
- [ ] **Rollback recipe** (pre-staged on every host): stop service → switch `JAVA_HOME`/start-script path back to Corretto 11 → start. Valid because bytecode is still 11 until Phase 4.

### Gate G4 (bytecode 25 — Phase 4)
- [ ] `javap` major-version check flips 55 → 69 in CI (doc 04 §2)
- [ ] Repeat G1 build-equivalence + G2 smoke (staging) + G3 canary order
- [ ] ⚠ Rollback now requires **rebuilding** with `options.release = 11` (single-line revert + CI run) — document this in the release notes; keep the last Java-11-bytecode release tagged and its artifacts archived for fast re-deploy

## 4. Issue triage playbook (Phase 2 soak)

| Symptom | Likely cause | First response |
|---|---|---|
| `java.lang.UnsupportedOperationException ... sun.misc.Unsafe` | JEP 498 enforcement / `--sun-misc-unsafe-memory-access` missing | Verify flag reached the JVM (`jcmd VM.flags` / ps); check `JDK_JAVA_OPTIONS` |
| `InaccessibleObjectException` / `IllegalAccessError` at boot | Missing `--add-opens` for some lib | Add the module/package from the stack trace to the shared flag file; record in doc 02 |
| Netty direct-buffer OOM or `maxDirectMemory` complaints under load | Old Netty fallback allocation path | Confirm `-Dio.netty.tryReflectionSetAccessible=true` + nio opens; if persists → contingency ladder step 2 (Vert.x 3.9.16) |
| `WARNING: A restricted method ... has been called` (JNI) | JEP 472, sqlite/snappy/zstd/netty-native | Cosmetic on 25 if `--enable-native-access=ALL-UNNAMED` set; otherwise add it |
| TLS handshake failures on a connector | JDK 25 disabled legacy protocol/cipher | Identify endpoint; prefer fixing the remote; last resort scoped `jdk.tls.disabledAlgorithms` override |
| Date/locale formatting diffs in exports | CLDR/tzdata updates | Pin expected formats in code (`Locale` explicit), not JVM defaults |

## 5. Reporting

- One tracking issue per phase with its gate checklist pasted in.
- `ChangeLog.md` entry per landed phase.
- Final close-out: update CLAUDE.md + NOTES-* docs (doc 04 §5), delete `docs/migration-baseline/` artifacts or archive them, retag `post-jdk25-migration`.
