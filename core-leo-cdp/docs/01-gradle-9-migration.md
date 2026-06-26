# 01 — Gradle 6.9.4 → 9.x Migration (build script rewrite)

Part of the [Java 25 / Gradle 9 migration plan](00-java25-gradle9-migration-overview.md). Executes in **Phase 1**.

## 0. Version choice

- **Target Gradle 9.1.0 or later** (latest 9.x at execution time; 9.5.x as of mid-2026).
  **Gradle 9.0.0 is NOT enough** — it supports running/compiling up to Java 24 only; Java 25 support (daemon + toolchains) landed in **9.1.0**.
- Gradle 9 requires **JDK 17+** to run the build itself. Plan: run the Gradle daemon on Corretto 25, emit Java 11 bytecode via `options.release = 11` until Phase 4.

## 1. Add the wrapper (Phase 0, before any upgrade)

There is currently **no `gradlew` in the repo** — CI and the Dockerfile download Gradle 6.9.4 by hand. Fix that first so every later bump is one committed change:

```bash
gradle wrapper --gradle-version 6.9.4   # Phase 0
# later, per upgrade step:
./gradlew wrapper --gradle-version 7.6.4
./gradlew wrapper --gradle-version 8.14.3
./gradlew wrapper --gradle-version 9.1.0   # or latest 9.x
```

Commit `gradlew`, `gradlew.bat`, `gradle/wrapper/**`. Update `build.sh`, CI, and the Dockerfile to call `./gradlew` (doc 04).

## 2. Stepwise upgrade path

Do **not** jump 6.9.4 → 9.x directly. At each stop run:

```bash
./gradlew AutoBuildForDeployment --warning-mode all
./gradlew test --warning-mode all
```

| Stop | What breaks there (this project specifically) |
|---|---|
| 7.6.4 | `maven` plugin removed → `apply plugin: 'maven'` + `uploadArchives` fail. Fix: delete both (see §3.1). |
| 8.14.x | `baseName` removed from Jar tasks → `archiveBaseName`. `Project.buildDir` deprecated. |
| 9.x | `buildDir` accessor removed → `layout.buildDirectory`. Groovy 4 script runtime. Configuration cache becomes the promoted default workflow (not mandatory, but config-time anti-patterns now warn loudly). |

## 3. Required `build.gradle` changes (file: `core-leo-cdp/build.gradle`)

### 3.1 Delete the legacy `maven` plugin and `uploadArchives` (lines 83, 239–245)

```groovy
// DELETE:
apply plugin: 'maven'
...
uploadArchives {
    repositories { flatDir { dirs 'repos' } }
}
```

`uploadArchives` publishes to a local `repos/` flat dir that nothing consumes — it is dead code. **Do not** replace with `maven-publish` unless a publishing need actually exists.

### 3.2 Java compatibility → `release` (line 93)

```groovy
// BEFORE
sourceCompatibility = 11

// AFTER (Phase 1 — JDK 25 daemon, Java 11 bytecode)
java {
    // toolchain optional; `release` alone is enough when daemon JDK ≥ target
}
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release = 11          // flips to 25 in Phase 4
}
```

`options.release` cross-compiles against the real JDK 11 API signatures (safer than the old `sourceCompatibility`/`targetCompatibility` pair, which links against JDK 25 APIs). In Phase 4 add an explicit toolchain:

```groovy
java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}
```

### 3.3 Jar tasks: `baseName` → `archiveBaseName` (5 occurrences: lines 345, 366, 387, 408, 429)

```groovy
// BEFORE
baseName = 'leo-main-starter'
// AFTER
archiveBaseName = 'leo-main-starter'
```

### 3.4 `${buildDir}` → `layout.buildDirectory` (lines 43–44)

```groovy
// BEFORE
project.ext.buildOutputFolderPath  = resolveProperty("buildOutputFolderPath",  "${buildDir}/dist-release")
// AFTER
project.ext.buildOutputFolderPath  = resolveProperty("buildOutputFolderPath",  layout.buildDirectory.dir("dist-release").get().asFile.path)
```

### 3.5 Manifest `Class-Path`: stop resolving `runtimeClasspath` at configuration time (lines 322–332)

`getClasspathStringJars()` calls `configurations.runtimeClasspath.files` while the script is being evaluated — a Gradle 9 deprecation and a configuration-cache blocker. Make the manifest value a provider:

```groovy
def classpathStringJars = providers.provider {
    def jarPaths = configurations.runtimeClasspath.files.collect { "deps/${it.name}" }
    "." + ' ; ' + jarPaths.join(' ; ')
}

// in each Jar task:
manifest {
    attributes(
        'Implementation-Title': 'MainHttpStarter',
        'Main-Class': 'leotech.starter.MainHttpStarter',   // NOTE: also trim the stray leading space!
        'Class-Path': classpathStringJars.get()             // or use doFirst { manifest.attributes['Class-Path'] = ... }
    )
}
```

> ⚠ While here, note the existing manifests have `'Main-Class': ' leotech.starter...'` with a **leading space** — Corretto tolerates it but it should be cleaned up, with a before/after `java -jar` smoke test.

Same applies to `Implementation-Version: getImplementationVersion()` — see §3.7.

### 3.6 `CopyDevOpsScriptToBUILD` runs `copy {}` at configuration time (lines 553–580)

The four `copy { ... }` blocks inside the task body execute during **every** build's configuration phase, regardless of which task runs. Rewrite as a real Copy task:

```groovy
tasks.register('CopyDevOpsScriptToBUILD', Copy) {
    group = 'Deployment Preparation'
    description = 'Copies DevOps and installation scripts to the build folder.'
    into project.ext.buildOutputFolderPath
    into('devops-script/docker-arangodb')        { from 'devops-script/docker-arangodb' }
    into('devops-script/docker-kafka')           { from 'devops-script/docker-kafka' }
    into('devops-script/kafka-docker-production'){ from 'devops-script/kafka-docker-production' }
    into('devops-script/script-installation')    { from 'devops-script/script-installation' }
}
```

(Equivalent cleanups: `copyDocuments` currently re-targets `into` three times — last one wins in newer Gradle; restructure with `from(...)` multiple times + single `into`.)

### 3.7 Groovy 4: replace `Date.format()` (lines 48–51)

Gradle 9 bundles Groovy 4, where `java.util.Date.format(String)` lives in the optional `groovy-dateutil` module — do not rely on it. Use `java.time`:

```groovy
ext.getImplementationVersion = {
    def ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern('yyyy-MM-dd-HH-mm-ss'))
    return "${project.version}_${ts}"
}
```

> Note: a timestamp in the manifest makes builds non-reproducible and breaks configuration-cache reuse. Acceptable for now (matches current behavior); consider sourcing it from `-PbuildTimestamp` later.

### 3.8 Minify plugin: 1.3.2 → 2.1.1 (lines 69–85, 258–315)

```groovy
// buildscript classpath BEFORE
classpath "org.gradle-webtools.minify:gradle-minify-plugin:1.3.2"
// AFTER — prefer the plugins DSL:
plugins {
    id 'java'
    id 'eclipse'
    id 'org.gradlewebtools.minify' version '2.1.1'
}
```

**Verification step (cannot be assumed):** the plugin is low-traffic; confirm 2.1.1 applies cleanly on Gradle 9 and that `JsMinifyTask { srcDir / dstDir / options { originalFileNames } }` API is unchanged. Two outcomes:

- ✅ Works → checksum-compare `public/js/leo-observer/*.js` and `resources/.../common-resources-min/*.js` against the Phase 0 baseline. The minified output is **committed and CDN-published via jsDelivr** — any diff needs functional QA of the tracker (risk R6).
- ❌ Broken → **fallback:** drop the plugin and write a small custom task that shells out to the **`com.google.javascript:closure-compiler` dependency already on the classpath** (line 204), e.g. a `JavaExec`-based task per directory with the same version-header `doLast`. This removes the third-party-plugin risk permanently.

### 3.9 Guava variant workaround can use the typed API now (lines 104–117)

The string-based attribute hack was needed because `TargetJvmEnvironment` didn't exist in Gradle 6.9.4. On Gradle 9, replace with:

```groovy
configurations.all {
    exclude module: 'ch.qos.logback'
    attributes {
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                  objects.named(TargetJvmEnvironment, TargetJvmEnvironment.STANDARD_JVM))
    }
}
```

(Keep the explanatory comment; update it.)

### 3.10 Eager `task foo(type: X)` → `tasks.register` (whole file, optional but recommended)

The eager syntax still works on Gradle 9, so this is not blocking — but converting the ~15 tasks to `tasks.register('name', Type) { ... }` enables configuration avoidance and is a prerequisite for a clean configuration-cache run. Do it as a mechanical follow-up commit inside Phase 1.

### 3.11 Tests

Gradle 9 no longer auto-detects JUnit platform in all cases — be explicit, and keep the manual-utility caveat from CLAUDE.md in mind (most `test.**` classes need live ArangoDB/Redis):

```groovy
test {
    useJUnitPlatform()
}
// junit-jupiter-engine alone is enough today, but add the launcher explicitly on Gradle 9:
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

## 4. Configuration cache (stretch goal, not a gate)

After §3.5/§3.6/§3.10, try `./gradlew AutoBuildForDeployment --configuration-cache`. Known remaining blockers: the timestamp in `getImplementationVersion()` and `Exec` tasks (`runDeployShellScript`). Treat as best-effort; do **not** gate the migration on it.

## 5. Phase 1 exit checklist

- [ ] `./gradlew AutoBuildForDeployment` green on Gradle 9.x, JDK 25 daemon, `--warning-mode all` shows no deprecations
- [ ] Output tree diff vs Phase 0 baseline: same jar names, same `deps/` contents, manifests equivalent (`Class-Path`, `Main-Class`)
- [ ] `java -jar leo-main-starter-<ver>.jar` boots on **Corretto 11** (bytecode still 11 — proves nothing regressed for current prod)
- [ ] Minified JS byte-identical OR diff reviewed + tracker QA'd (R6)
- [ ] `./gradlew test` runs the same test set as before
- [ ] `build.sh` updated to call `./gradlew`
- [ ] `eclipse` plugin still generates a usable project (`./gradlew eclipse`)

## Sources

- [Upgrading to Gradle 9.0.0](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html)
- [Gradle 9.1.0 Release Notes](https://docs.gradle.org/9.1.0/release-notes.html)
- [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [gradle-minify-plugin — Plugin Portal](https://plugins.gradle.org/plugin/org.gradlewebtools.minify) / [GitHub](https://github.com/gradle-webtools/gradle-minify-plugin)
