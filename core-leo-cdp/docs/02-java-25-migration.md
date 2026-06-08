# 02 — Java 11 → Java 25 (LTS) Migration

Part of the [Java 25 / Gradle 9 migration plan](00-java25-gradle9-migration-overview.md). Executes in **Phases 2–4**.

## 1. Strategy: runtime first, bytecode last

Two independent dials, moved separately:

1. **Runtime JDK** (Phase 2–3): run the existing Java-11 bytecode on Corretto 25. Fully reversible — rollback = restart the same jars on Corretto 11.
2. **Bytecode/language target** (Phase 4): `options.release = 11 → 25`. Irreversible for deployed artifacts (jars no longer load on Java 11), so it goes last.

The vendored `ext-lib/` jars (bytecode majors 51/52/55 = Java 7/8/11) load fine on a Java 25 JVM — class-file compatibility is forward; only the *minimum* class-file version matters.

## 2. Source-code audit result (done 2026-06-06)

Scanned `src/main` + `src/test` + all three `ext-lib` jars for everything removed or restricted between Java 11 and Java 25:

| Pattern | Result |
|---|---|
| `sun.misc.*`, `jdk.internal.*` | **none** (source and vendored jars) |
| `javax.xml.bind` (JAXB) | none |
| Nashorn / `jdk.nashorn` / `javax.script` | none |
| `SecurityManager` / `AccessController` (JEP 486: permanently disabled in 24) | none |
| `Thread.stop()` (throws UOE since 20) | none |
| `finalize()` overrides | none |
| Deprecated boxing ctors (`new Integer(...)` etc.) | none |
| `com.sun.management.OperatingSystemMXBean` | 1 use — `src/main/java/leotech/system/domain/SystemSnapshot.java:7`. **Supported exported API** (module `jdk.management`); no change needed. |

**Conclusion: zero source changes required to *run* on Java 25.** All Java-25 risk lives in dependencies (doc 03) and JVM startup flags (below).

## 3. Required JVM flags on JDK 25 (Phase 2)

Old Netty (via Vert.x 3.8.5), Gson, Kafka clients and JNI-using libs (sqlite-jdbc, kafka compression codecs) predate the JDK's integrity-by-default tightening. Standard flag set for **all five starters**:

```bash
JAVA25_COMPAT_FLAGS="\
 --sun-misc-unsafe-memory-access=allow \
 --enable-native-access=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.base/java.nio=ALL-UNNAMED \
 --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
 -Dio.netty.tryReflectionSetAccessible=true"
```

Why each:

| Flag | Reason |
|---|---|
| `--sun-misc-unsafe-memory-access=allow` | JEP 498 (JDK 24+): Netty 4.1.x uses `sun.misc.Unsafe` memory access; without the flag you get startup warnings now and `UnsupportedOperationException` in a future JDK. Netty is Unsafe-free only from **4.2.3+** (MemorySegment). |
| `--enable-native-access=ALL-UNNAMED` | JEP 472 (JDK 24+ warnings): JNI usage by netty-native, sqlite-jdbc, snappy/zstd (Kafka), conscrypt-ish libs. |
| `--add-opens java.base/java.nio + sun.nio.ch` | Netty 4.1.44 direct-buffer cleaner reflection (`DirectByteBuffer`), `PlatformDependent0`. |
| `--add-opens java.base/java.lang(+reflect) + java.util` | Gson 2.9.1 / Jackson / MVEL reflective access to JDK types; Kafka serializers. |
| `-Dio.netty.tryReflectionSetAccessible=true` | Lets old Netty actually attempt the (now-opened) reflective direct-buffer path instead of falling back to slower allocation. |

**Tuning flags audit** (current `JVM_PARAMS` in `devops-script/shell-script-starter/*.sh`):

| Current flag | On JDK 25 |
|---|---|
| `-server` | Obsolete no-op — drop |
| `-XX:+UseCompressedOops` | Default below 32 GB heap — drop (harmless) |
| `-XX:+TieredCompilation` | Default — drop |
| `-XX:+DisableExplicitGC` | Still valid — **keep** (Netty calls `System.gc()` via cleaner paths; verify no direct-buffer OOM under k6 load with this enabled + old Netty) |
| `-XX:+UseNUMA` | Still valid — keep if hosts are NUMA |
| *(add in Phase 4)* `-XX:+UseCompactObjectHeaders` | JEP 519, product flag in 25, off by default. 4–8 byte/object savings — meaningful for millions of in-memory profile/event objects. Enable after soak. |

The exact flag set must be wired into `devops-script/shell-script-starter/*.sh`, the Dockerfile `ENTRYPOINT`, and any systemd units (doc 04). Log scrubbing during Phase 2 soak: grep for `WARNING: A terminally deprecated method`, `WARNING: A restricted method`, `InaccessibleObjectException`, `IllegalAccessError` — each hit either gets a flag or a dependency bump.

## 4. Behavioral changes to verify (no code change expected, test anyway)

| Change (JDK 11 → 25) | Where it could bite here |
|---|---|
| Default GC tweaks across 11→25 (G1 improvements, region sizing) | Re-baseline k6 latency/throughput; don't carry over hand-tuned GC flags blindly |
| `Locale`/CLDR data updates, `java.time` tzdata | Date formatting in reports/exports (fastexcel, handlebars templates), Vietnamese locale handling (junidecode/slugify paths) |
| TLS defaults (TLS 1.3 first, weak ciphers removed) | Outbound connectors: Zalo SDK, Google APIs, SMTP (`javax.mail`), webhooks. Old endpoints needing TLS1.0/1.1 will fail — audit `jdk.tls.disabledAlgorithms` only if a connector breaks. |
| Stricter `URL`/URI parsing; `URL` ctors deprecated (21+) | Compile-time warnings only in Phase 4; tracking/webhook URL parsing tests |
| String concat / hashing perf changes | None expected; covered by k6 |
| JIT/`ThreadLocal`/virtual-thread-adjacent changes | None — app uses platform threads + Vert.x event loop |

## 5. Phase 4 — raising the bytecode target to 25

1. Flip `options.release = 25` (and add the JDK 25 toolchain block, doc 01 §3.2).
2. Expect **new warnings, not errors**: deprecated `new URL(...)` (use `URI.create(...).toURL()`), possible `this-escape` lint. Fix opportunistically; gate only on `-Werror`-clean for *new* warnings introduced by the bump if desired.
3. Contextual keywords (`record`, `sealed`, `yield`, `var`) as identifiers: audit shows no conflicts in `leotech.**`.
4. Re-run the full Phase 0 baseline comparison; deploy through staging exactly like Phase 2.
5. Update `README`/`NOTES-FOR-NEW-SETUP.md`/`NOTES-FOR-UPGRADE.md`: minimum runtime is now Java 25.

### Then (optional, ongoing modernization)
- **Virtual threads** for `ScheduledJobStarter` connector jobs and blocking DAO calls (ArangoDB/JDBC/Jedis are blocking I/O — classic Loom win). **Not** for Vert.x event-loop code (Vert.x 3 has no Loom integration; don't block event loops, same rule as today).
- Records for DTO/filter classes, pattern matching in handler dispatch, text blocks for AQL templates currently in strings.
- `-XX:+UseCompactObjectHeaders` rollout (§3).

## 6. Phase 2/3 exit checklists

**Phase 2 (staging on Corretto 25):**
- [ ] All five starters boot with `$JAVA25_COMPAT_FLAGS`; zero `InaccessibleObjectException`/`IllegalAccessError` in 72 h soak
- [ ] Event ingestion path E2E: `leo.observer.js` → ObserverStarter → ArangoDB → Redis Pub/Sub → Airflow trigger observed
- [ ] k6 suite within ±10% of Java 11 baseline (latency p95, throughput, RSS)
- [ ] Direct-buffer / native-memory stable (`jcmd VM.native_memory`, no growth trend)
- [ ] Quartz jobs fire on schedule; Kafka pipeline consumes with snappy/zstd payloads; file upload (multipart) OK
- [ ] Email send (javax.mail TLS), Google API connectors, Zalo webhook verified

**Phase 3 (prod):** service-by-service rollout in blast-radius order (uploader → scheduler → data-processing → observer → admin), 24 h bake each, rollback = restart on Corretto 11.

## Sources

- [Netty: Java 24 and sun.misc.Unsafe](https://netty.io/wiki/java-24-and-sun.misc.unsafe.html)
- [JEP 498: Warn upon Use of Memory-Access Methods in sun.misc.Unsafe](https://openjdk.org/jeps/498)
- [JEP 472: Prepare to Restrict the Use of JNI](https://openjdk.org/jeps/472)
- [JEP 519: Compact Object Headers](https://openjdk.org/jeps/519)
