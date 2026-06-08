# 04 — Runtime & Deployment Changes (Docker, CI, devops scripts)

Part of the [Java 25 / Gradle 9 migration plan](00-java25-gradle9-migration-overview.md). Executes alongside **Phases 1–3**.

## 1. Dockerfile (`core-leo-cdp/Dockerfile`)

Current: `amazoncorretto:11` build + runtime stages, hand-downloaded Gradle 6.9.4.

### Phase 1 change (Gradle 9, Java 11 bytecode)
```dockerfile
# build stage — wrapper replaces the manual Gradle download
FROM amazoncorretto:25 AS build          # Gradle 9 needs JDK 17+ to RUN; bytecode stays 11 via options.release
WORKDIR /src
COPY . .
RUN ./gradlew AutoBuildForDeployment --no-daemon \
      -PbuildOutputFolderPath=/dist -PstaticOutputFolderPath=/static -PbuildVersion=docker
```
Delete the `yum install unzip` + `curl gradle-6.9.4-bin.zip` lines entirely — the wrapper downloads its own pinned distribution (keep `tar gzip` if anything else needs them; the wrapper only needs an unzip-capable JDK, which it has built-in via its own bootstrap).

### Phase 2 change (runtime on Corretto 25)
```dockerfile
FROM amazoncorretto:25
WORKDIR /app
COPY --from=build /dist /app
EXPOSE 9070 9080
# JAVA25_COMPAT_FLAGS from doc 02 §3
ENTRYPOINT ["java", \
  "--sun-misc-unsafe-memory-access=allow", \
  "--enable-native-access=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", \
  "--add-opens=java.base/java.util=ALL-UNNAMED", \
  "--add-opens=java.base/java.nio=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "-Dio.netty.tryReflectionSetAccessible=true", \
  "-jar", "leo-main-starter-docker.jar"]
```

Also update `devops-script/docker-leocdp/` and the repo-root `docker-compose` deployment (commit `876d6af`) if they reference the Java 11 image, and the GHCR-published image notes.

> Tip: put the flags in `JDK_JAVA_OPTIONS` env (picked up automatically by the `java` launcher and shared by all five starters) instead of repeating them per ENTRYPOINT:
> `ENV JDK_JAVA_OPTIONS="--sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED ..."`

## 2. CI — `.github/workflows/ci-cd.yml`

| Line (current) | Phase | Change |
|---|---|---|
| `distribution: corretto` / `java-version: '11'` | 1 | `java-version: '25'` (Gradle 9 daemon needs ≥17; bytecode still 11 — `options.release` guarantees it regardless of CI JDK) |
| `gradle-version: '6.9.4'` + the comment about no wrapper / legacy maven plugin | 0–1 | Delete `gradle-version` — `gradle/actions/setup-gradle@v4` auto-uses the committed wrapper. Update the stale comment. |
| `run: gradle compileJava test` etc. | 0 | `./gradlew ...` |
| Docker build/test stages | 2 | Inherit Dockerfile changes; JUnit XML export path unchanged |

Add one **cheap guard job** in Phase 1 so the "bytecode stays 11" invariant is enforced, not assumed:

```yaml
- name: Verify class-file target is Java 11 (major 55)
  run: |
    javap -verbose -cp build/classes/java/main leotech.starter.MainHttpStarter | grep "major version: 55"
```
(Flip to `major version: 69` in Phase 4.)

## 3. Devops shell scripts — `devops-script/shell-script-starter/*.sh`

Files: `start-admin.sh`, `start-observer.sh`, `start-data-connector-jobs.sh`, `setup-new-leocdp.sh` (+ `shell-scripts/*` deploy helpers).

Current line 13 in each:
```bash
JVM_PARAMS="-Xms256m -Xmx1G -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"
```

Phase 2 replacement (rationale per flag in doc 02 §3):
```bash
# Memory sizing unchanged; obsolete flags dropped; JDK-25 compatibility flags added
JAVA25_COMPAT_FLAGS="--sun-misc-unsafe-memory-access=allow \
 --enable-native-access=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.base/java.nio=ALL-UNNAMED \
 --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
 -Dio.netty.tryReflectionSetAccessible=true"
JVM_PARAMS="-Xms256m -Xmx1G -XX:+DisableExplicitGC -XX:+UseNUMA $JAVA25_COMPAT_FLAGS"
```

Recommendation: source the flag block from one shared file (e.g. `shell-script-starter/jvm-params.sh`) so five scripts can't drift.

## 4. Host provisioning

- `devops-script/script-installation/` (incl. anything like `install-java.sh` referenced by CI comments): switch package from Corretto 11 to **Corretto 25** (`java-25-amazon-corretto` / corretto yum repo).
- Phase 2/3 hosts must have **both** JDKs installed during the rollout window; start scripts pick via explicit `JAVA_HOME`/full path so per-service rollback (doc 00 Phase 3) is a one-line change.
- `build.sh` (interactive first-time setup): change `gradle` invocation to `./gradlew`; add a JDK-25 presence check.

## 5. Documentation to update when phases land

| File | Update |
|---|---|
| `CLAUDE.md` | Build section: Gradle 9.x + wrapper (`./gradlew`), Java 25; remove "no gradlew wrapper" and "legacy maven plugin" caveats |
| `NOTES-FOR-NEW-SETUP.md`, `NOTES-FOR-UPGRADE.md`, `NOTES-FOR-DEV.md`, `README.md` | JDK/Gradle prerequisites, new JVM flags |
| `setup-leocdp-metadata-document.md` | Only if it mentions Java version |
| `ChangeLog.md` | Entry per phase |

## 6. Static CDN output (R6 reminder)

`CopyPublicFolderToSTATIC` feeds the jsDelivr-published `leo-cdp-static-files` repo. After the Gradle-9/minify-plugin change, **do not push** the static output until minified JS has been checksum-compared or functionally QA'd (doc 01 §3.8) — every customer site loads `leo.observer.min.js` from there.
