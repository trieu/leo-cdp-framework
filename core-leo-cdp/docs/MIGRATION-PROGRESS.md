# Migration Progress Report — Java 25 / Gradle 9

Tracks execution of the [migration plan](00-java25-gradle9-migration-overview.md). Updated as work lands.

**Last updated:** 2026-06-06
**Working branch:** `liem/java25-gradle9-migration` (off `liem/setup-local`)
**Execution environment:** local Windows dev box (Microsoft OpenJDK 21 default; JDK 25.0.2 at `~/.jdks/ms-25.0.2`; JDK 11 + Gradle 6.9.4 provisioned for baseline)

## Status dashboard

| Phase | Step | Status | Gate | Notes |
|---|---|---|---|---|
| 0 | 0a — Provision JDK 11 + Gradle 6.9.4 | ✅ done | — | Temurin 11.0.31 + Gradle 6.9.4 portable under `C:\Users\dvtliem\AI\tools\` |
| 0 | 0b — Baseline build + artifact capture | ✅ done | — | `AutoBuildForDeployment` green in 8m30s; artifacts in `docs/migration-baseline/gradle694-*` (bytecode major 55 confirmed) |
| 0 | 0c — Wrapper @6.9.4, branch, tag | ✅ done | — | Tag `pre-jdk25-migration` |
| 1 | 1a — Gradle 7.6.4 (`maven` plugin removal) | ⬜ pending | — | |
| 1 | 1b — Gradle 8.14.x (`baseName`, `buildDir`) | ⬜ pending | — | |
| 1 | 1c — Gradle 9.x full rewrite | ⬜ pending | **G1** | minify plugin 2.1.1 verify; output diff vs baseline |
| 2 | Code: JVM flags, Dockerfile, CI | ⬜ pending | — | |
| 2 | Local JDK-25 boot smoke | ⬜ pending | — | Boot starters on 25; expect clean JVM init up to DB connect |
| 2 | **Staging soak (72 h) + k6** | ⛔ blocked: needs staging env | **G2** | Cannot run from this box |
| 3 | Prod canary rollout | ⛔ blocked: after G2 | **G3** | |
| 4 | Bytecode → 25 (`options.release = 25`) | ⛔ blocked: after G3 soak | **G4** | One-line flip prepared in build.gradle comment |

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
