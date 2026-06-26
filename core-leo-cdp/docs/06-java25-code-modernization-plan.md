# 06 — Java 25 Code Modernization Plan (bytecode 25, records, 21+ idioms)

Extends the [migration plan](00-java25-gradle9-migration-overview.md): this is **Phase 4 expanded into waves**, covering the bytecode-target flip *and* source modernization (records, pattern matching, text blocks, virtual threads).

**Branch policy:** executed on `liem/java25-gradle9-migration`. From Wave 0 onward this branch produces **class-file major 69 (Java 25)** artifacts — it can no longer run on Corretto 11. Rollback anchor: commit `d64612b` (last bytecode-55 state); rollback = revert the Wave-0 commit and rebuild.

---

## Wave 0 — Bytecode flip + build verification + perf comparison (≈ ½ day)

1. `build.gradle`: `options.release = 11 → 25` (one line).
2. CI guard flips: `major version: 55` → `major version: 69` (`.github/workflows/ci-cd.yml`).
3. Full build; verify major 69 via the baseline script; G1-style output diff (tree/deps/manifests must stay identical — only class-file contents change).
4. Build container image `jdk25-bc69`; boot smoke; **k6 batch 3**: `jdk25-local` (bytecode 55) vs `jdk25-bc69` (bytecode 69), same Corretto 25 JVM, interleaved 3-round protocol + memory snapshot → appended to [PERFORMANCE-TEST-REPORT-JDK25.md](PERFORMANCE-TEST-REPORT-JDK25.md).

> Expectation set honestly: javac 25 emits *nearly identical* bytecode for 11-era source — measurable deltas, if any, will be small. The flip's real value is **unlocking the language level** for Waves 1–3. The interesting perf deltas arrive with records (allocation/footprint) and virtual threads (blocking-path throughput).

**Exit gate W0:** build green · bytecode 69 verified · output-tree diff clean · boot + login-page smoke on the new image · k6 batch 3 recorded.

## Triage matrix — what may become a `record` and what must NOT

The blanket rule "turn POJOs into records" **would break LEO CDP**. Records are shallowly immutable, have no no-arg constructor, no setters, and expose `name()` accessors instead of `getName()`. Three subsystems depend on exactly those properties:

| Subsystem | Dependency on POJO shape | Consequence |
|---|---|---|
| **ArangoDB driver 6.25** (`cdp/model/**` persisted documents) | VPACK mapper instantiates via no-arg ctor + setters/fields | Record entities fail deserialization → **persisted entities stay classes** until driver 7.x (Jackson-serde, record-capable) |
| **Gson 2.9.1** (API payloads, `leo-observer` event JSON) | Field reflection + no-arg ctor (Unsafe allocation) | Gson supports records only from **2.10+** → bump to 2.11+ is a **prerequisite** for any record that crosses a Gson boundary |
| **Handlebars 4.3.1 / Mustache** (admin templates) | Resolves `{{name}}` via `getName()`/field | Record `name()` accessors are invisible to older resolvers → **template-rendered models stay classes** (or upgrade + verify resolver) |
| **MVEL 2.5** expressions, Quartz job data | Property/getter conventions | Same caution as templates |
| Anything used as key in `HashMap`/`HashSet` with identity semantics | records redefine `equals`/`hashCode` by components | behavioral change — review per class |

### ✅ Record candidates (convert in Wave 2)
- `cdp/query/filters/*Filter` request-shaping objects **if** constructed from HTTP params and never persisted *(verify Gson/Jackson boundary first)*
- Internal value types: ID pairs, date ranges, scoring tuples, `(key,value)` carriers in `domain/**` service internals
- Method-return aggregates currently using small mutable classes or `Object[]`/`Map` hacks
- New code: records by default for immutable data

### ❌ Not candidates
- `cdp/model/**` ArangoDB documents (Profile, TrackingEvent, Segment, Campaign, assets…) — mutable lifecycle (`set…` during enrichment/merge) **and** driver-6 serde
- Anything rendered by handlebars/mustache templates
- Classes Gson-deserialized until the Gson bump lands
- Quartz `JobDataMap`-bound beans

## Wave 1 — Mechanical 21+/25 idioms via OpenRewrite (≈ 2–4 days, low risk)

Tooling: **OpenRewrite** `rewrite-migrate-java` (`UpgradeToJava25` / `Java21BestPractices`-style recipes) via the Gradle plugin; then `git diff` review in PR-sized chunks per package.

| Idiom | Where it pays in this codebase |
|---|---|
| Pattern matching for `instanceof` | handler dispatch, `EventObserverUtil`, payload parsing |
| `switch` expressions (+ pattern switch where closed sets exist) | event-type routing, status mapping |
| **Text blocks** | inline AQL fragments, JSON templates, HTML email snippets (`resources/database/database-query-template.aql` stays a file) |
| `String.isBlank/strip/formatted`, `Stream.toList()` | utils everywhere (`StringUtil` call sites) |
| `var` for obvious locals | per team taste — keep conservative |
| Sequenced collections (`getFirst/getLast/reversed`) | list-handling in `domain/**` |
| `Objects.requireNonNullElse`, `Map.of/List.of` | config/defaults code |

Rules: zero behavior change; one package-cluster per PR; `./gradlew test` + admin boot smoke per PR. **Exit gate W1:** all PRs merged, build + smoke green, no new warnings.

## Wave 2 — Records + sealed types for the triaged candidates (≈ 1 week)

1. Prerequisite: **Gson → 2.11+** (also closes a B2 hygiene item), regression on the observer event-ingestion JSON path.
2. Convert ✅ candidates package-by-package; each conversion must check: serialization boundary (Gson/Jackson/VPACK), template usage, equals/hashCode semantics change, mutation sites (compiler will reveal).
3. `sealed interface` where hierarchies are closed (e.g., event-category or channel-type hierarchies in `cdp/model` *behavioral* interfaces — not the persisted documents themselves).
4. **Exit gate W2:** tests green, observer event E2E (post → ArangoDB) verified, admin UI smoke, k6 re-run (records should *reduce* allocation; watch memory snapshot).

## Wave 3 — Virtual threads for blocking paths (≈ 3–5 days, measured)

- Targets: `ScheduledJobStarter` connector jobs, `DataProcessingStarter` batch steps, blocking DAO fan-outs (ArangoDB/JDBC/Jedis are blocking clients — classic Loom fit).
- Mechanism: `Executors.newVirtualThreadPerTaskExecutor()` at the job/batch orchestration layer; pinning check with `-Djdk.tracePinnedThreads=full` (old `synchronized` blocks in rfx-core/jedis paths may pin — JEP 491 in 24+ largely mitigates).
- **Hard rule:** never on Vert.x 3.x event-loop code; Vert.x 3 predates Loom integration.
- **Exit gate W3:** job throughput A/B (connector batch wall-clock), no pinning storms, k6 + memory unchanged on HTTP path.

## Wave 4 — Library modernization enabling deeper records (tie-in to doc 03 batch B2/B3)

ArangoDB driver 7.x (record-capable serde) → unlocks records for *some* persisted value sub-objects; Jackson 2.19+; handlebars upgrade + record-accessor verification. Each its own task; out of scope here.

## Risks specific to this modernization

| Risk | Mitigation |
|---|---|
| Record `equals/hashCode` semantic shifts break set/map logic | per-class review at conversion; tests |
| Gson bump changes JSON edge behavior (nulls, lenient parsing) | observer-event golden-file regression before/after |
| OpenRewrite recipe over-reach | recipes run per-package, PR review, no auto-merge |
| Bytecode 69 blocks emergency rollback to Corretto 11 | accepted (G4 reality); keep `d64612b` + `jdk25-local` image archived |
| rfx-core (binary-only) interactions with modernized call sites | API surface unchanged — source-level idioms don't affect linkage; records passed *into* rfx APIs keep working (they're just final classes) |

## Sequence & estimate

| Wave | Effort | Gate |
|---|---|---|
| 0 — flip + perf batch 3 | ½ day | W0 (today) |
| 1 — mechanical idioms | 2–4 days | W1 |
| 2 — records + sealed | 1 week (incl. Gson bump) | W2 |
| 3 — virtual threads | 3–5 days | W3 |
| 4 — library-enabled deep records | separate tasks | — |
