# 03 — Dependency Compatibility Matrix (Java 25)

Part of the [Java 25 / Gradle 9 migration plan](00-java25-gradle9-migration-overview.md). Informs **Phases 2 and 4**.

Policy: **minimum-change in Phases 1–3** (only bump what blocks JDK 25), **opportunistic modernization in Phase 4**. Every "Phase 2 bump" below must individually pass the staging gate.

## 1. The critical path: Vert.x / Netty

| | |
|---|---|
| Current | `io.vertx:vertx-{core,web,web-client,codegen,auth-jwt}:3.8.5` → transitively **Netty ≈ 4.1.44.Final (Jan 2020)** |
| Problem | Netty 4.1.x relies on `sun.misc.Unsafe` + reflection into `java.nio` internals. On JDK 24/25 this produces JEP 498/JEP 472 warnings and, without flags, degraded buffer handling. Netty is only Unsafe-free from **4.2.3+** (MemorySegment-based). 4.1.44 additionally predates many JDK-16+ fixes (≥ 4.1.66 recommended for modern JDKs). |
| Blocker for real fix | Vert.x 4/5 is the proper answer, **but** `ext-lib/rfx-core-1.0.jar` is compiled against Vert.x 3 APIs and **its source is not in the repo**. Vert.x 4 migration is a separate project (out of scope, see R3). |

### Contingency ladder (try in order during Phase 2)

1. **Flags only** (doc 02 §3) on Vert.x 3.8.5 — cheapest; may be fully sufficient. Validate with k6.
2. **Bump Vert.x 3.8.5 → 3.9.16** (final 3.x, drop-in API) — brings Netty to a much newer 4.1.x line with the JDK-16+ fixes. ⚠ Known pitfall: Vert.x 3.9.x is incompatible with hand-pinned Netty 4.1.60 (CVE-2021-21295 header-validation changes caused `ClassCastException`); use the Netty version 3.9.16 itself declares, don't force-pin blindly. Must also smoke-test `rfx-core` paths (it links `io.vertx.core` + `io.netty.handler`).
3. **Vert.x 3.9.16 + careful Netty 4.1.1xx pin** — only if a specific Netty CVE/bug requires it; full regression of HTTP routing, WebSocket (`WebSocketDataHandler`), JWT auth.
4. **Vert.x 4/5 migration** — separate project; prerequisite: recover/rebuild `rfx-core` from source.

Decision gate: if step 1 passes the Phase 2 exit criteria, ship it and schedule step 2 as ordinary maintenance.

## 2. Full dependency table

Legend — **Action:** ✅ keep as-is · 🔵 bump in Phase 2 (JDK-25 enabling) · 🟡 bump in Phase 4 (modernization) · 🔴 critical path (§1)

| Dependency | Current | JDK 25 status | Action | Notes |
|---|---|---|---|---|
| `io.vertx:*` | 3.8.5 | ⚠ via old Netty | 🔴 | See §1 |
| `ext-lib/rfx-core-1.0.jar` | vendored, bytecode 55 | Runs on 25; clean of internal APIs | ✅ (watch) | Links Vert.x 3 / Netty / Jedis / Kafka APIs; pins those libs' API levels. Recover source (R3). |
| `ext-lib/query-builder-2.1.1.jar` | vendored, bytecode 52 | OK | ✅ | Pure-Java AQL rule parser |
| `ext-lib/zalo-java-sdk-4.0-SNAPSHOT.jar` | vendored, bytecode 51 | OK | ✅ | Uses only `javax.crypto`/`javax.net` — stable APIs |
| `com.arangodb:arangodb-java-driver` | 6.25.0 | OK (plain HTTP/VST + Jackson) | ✅ → 🟡 7.x | 7.x is the maintained line; API changes in serde — Phase 4+, own mini-project |
| `redis.clients:jedis` | 7.0.0 | OK (current) | ✅ | Already modern |
| `org.apache.kafka:kafka-{clients,streams}` | 3.5.2 | Works; JNI codecs (snappy/zstd) want `--enable-native-access` | 🟡 3.9.x | 3.5 EOL; 3.9 is drop-in for clients API |
| Jackson (`core`,`databind`,`annotations`,`module-scala_2.13`) | 2.14.2 | OK at runtime | 🟡 2.19+ | 2.14 EOL; bump for CVE hygiene. Keep scala module version-aligned. |
| `com.google.code.gson:gson` | 2.9.1 | Needs `--add-opens java.base/java.util` etc. for some JDK types | 🟡 2.11+ | 2.11+ removes most illegal-reflection on JDK types |
| Log4j2 (`api`,`core`,`slf4j2-impl`) | 2.23.1 | OK | ✅ → 🟡 2.25+ | Fine as-is |
| `org.slf4j:slf4j-api` | 2.0.13 | OK | ✅ | |
| `org.quartz-scheduler:quartz` | 2.3.2 | OK | 🟡 2.5.x | 2.5 requires Java 11+, fixes CVE-2023-39017-adjacent issues |
| `com.sun.mail:javax.mail` | 1.6.2 | OK (self-contained `javax.mail` impl) | ✅ → 🟡 Jakarta Mail 2 | Jakarta rename = `jakarta.mail` package change → touches `leotech.system` email code; Phase 4+ only |
| `javax.servlet:javax.servlet-api` | 3.1.0 | OK (compile-only surface for zalo-sdk) | ✅ | Don't move to jakarta — vendored zalo-sdk references `javax.servlet` |
| `joda-time` | 2.12.2 | OK | ✅ | Long-term: `java.time`, not a migration item |
| MySQL / PostgreSQL / SQLite JDBC | 9.5.0 / 42.7.10 / 3.51.0.0 | OK (current); sqlite is JNI → native-access flag | ✅ | |
| `org.jdbi:jdbi3-*` | 3.49.6 | OK (current) | ✅ | |
| OkHttp | 4.12.0 | OK | ✅ | |
| Apache `httpclient` | 4.5.13 | OK | 🟡 5.x or consolidate on OkHttp | Low priority |
| Guava (transitive) + variant pin | via google libs | OK | ✅ | Replace string-attribute hack with typed API (doc 01 §3.9) |
| Google API/OAuth/GCS clients | 2022–2023 vintage | OK on 25 | 🟡 BOM refresh | Network-facing: TLS verify in Phase 2 |
| `closure-compiler` | v20210302 | OK (build-time JS minify + runtime use) | 🟡 latest | Newer versions change minified output → R6 QA |
| `handlebars` 4.3.1, `mustache` 0.9.6, `snakeyaml` 1.33, `mvel2` 2.5.2.Final | | OK; MVEL does runtime codegen/reflection — covered by `--add-opens` | ✅ | snakeyaml 2.x is an API break — only with code changes, Phase 4+ |
| `commons-*` (text 1.14, lang3 3.12, validator 1.7, net 3.6, io **2.5**) | | OK | 🟡 commons-io 2.5 → 2.16+ | 2.5 is 2016-era; CVE-2024-47554 fixed in 2.14+ — cheap, do in Phase 2 |
| `univocity-parsers` 2.9.1, `fastexcel` 0.19.0, `thumbnailator` 0.4.20, `libphonenumber` 9.x, `geoip2` 3.0.2, `qrcodegen`, `jai-imageio`, `TextImageGen`, `junidecode`, `slugify`, `friendly-id`, `openlocationcode`, `commons-math3`, `nanocaptcha`, `contiperf` | | All pure-Java, OK | ✅ | `jai-imageio` registers ImageIO SPIs — verify TIFF/encoding paths in Phase 2 smoke |
| `owasp-java-html-sanitizer` 20240325.1, `jsoup` 1.21.1 | | OK (current) | ✅ | |
| JUnit 5.10.3 / AssertJ 3.26.3 / Selenium 4.22 / datafaker 1.5.0 | | OK on 25 | 🟡 JUnit 5.12+, datafaker 2.x | Add `junit-platform-launcher` (doc 01 §3.11) |

## 3. Transitive-risk watchlist (verify in Phase 2 logs)

- **netty-transport-native** (via Vert.x): JNI → native-access warnings.
- **snappy-java / zstd-jni** (via kafka-clients): extracts native libs to tmp; JNI warnings; verify on the prod OS/glibc.
- **conscrypt / BoringSSL** — not present (good).
- Anything logging `Illegal reflective access` at boot on 11 today is a guaranteed `--add-opens` candidate on 25 — capture the JDK-11 boot warnings during Phase 0 baseline as a checklist.

## 4. Dependency-bump batches

| Batch | Contents | Phase |
|---|---|---|
| B1 (enabling) | commons-io 2.16.x; *(conditional)* Vert.x 3.9.16 if ladder step 2 triggers | 2 |
| B2 (hygiene) | Jackson 2.19.x, Gson 2.11+, Kafka 3.9.x, Quartz 2.5.x, Log4j2 2.25.x, JUnit 5.12+ | 4 |
| B3 (projects) | ArangoDB driver 7.x · Jakarta Mail · snakeyaml 2 · httpclient 5 · Vert.x 4/5 | Post-migration, each its own task |

## Sources

- [Netty: Java 24 and sun.misc.Unsafe](https://netty.io/wiki/java-24-and-sun.misc.unsafe.html)
- [Vert.x 3.9 / Netty 4.1.60 incompatibility (eclipse-vertx/vert.x#3865)](https://github.com/eclipse-vertx/vert.x/issues/3865)
- [Eclipse Vert.x 3.9.16 release](https://vertx.io/blog/eclipse-vert-x-3-9-16/)
- [Netty requirements for 4.x](https://netty.io/wiki/requirements-for-4.x.html)
