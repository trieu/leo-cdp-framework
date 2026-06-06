# Multi-stage build for the LEO CDP core backend (MainHttpStarter / admin worker).
# NOTE: starting point - the Gradle build pulls the JS-minify plugin over the network,
# and integration tests still need a real leocdp-metadata.properties, so expect one
# iteration before the image boots cleanly.

# ---- build stage ----
# JDK 25 runs the Gradle 9 daemon; bytecode target is pinned by options.release in
# build.gradle (11 until migration Phase 4). The committed gradlew wrapper downloads
# its own pinned Gradle distribution - no manual install needed.
FROM amazoncorretto:25 AS build
WORKDIR /src
COPY . .
# Output goes to /dist with deps/ alongside; JAR manifest Class-Path points to ./deps
RUN ./gradlew AutoBuildForDeployment --no-daemon \
      -PbuildOutputFolderPath=/dist -PstaticOutputFolderPath=/static -PbuildVersion=docker

# ---- test stage ----
# Runs the JUnit suite as part of the image build. Best-effort (`|| true`) so the
# build doesn't fail in this stage; JUnit XML is exported when available. Many
# test.** classes need a live ArangoDB/Redis and won't pass inside an isolated build
# (see CLAUDE.md). The workflow surfaces pass/fail from whatever reports are exported.
FROM build AS test
RUN ./gradlew test --no-daemon \
      -PbuildOutputFolderPath=/dist -PstaticOutputFolderPath=/static -PbuildVersion=docker || true
RUN mkdir -p /test-results && \
    cp -r /src/build/test-results/. /test-results/ 2>/dev/null || true

# ---- test-results export stage ----
# `scratch` so `docker buildx build --target test-export --output type=local` exports
# ONLY the JUnit XML, not a whole image filesystem.
FROM scratch AS test-export
COPY --from=test /test-results /

# ---- runtime stage ----
FROM amazoncorretto:25
WORKDIR /app
COPY --from=build /dist /app
# JDK-25 compat flags for old Netty (Vert.x 3) / Gson / JNI users - rationale in
# docs/02-java-25-migration.md §3. JDK_JAVA_OPTIONS is read by the java launcher,
# so the same flags apply if the ENTRYPOINT jar is overridden with another starter.
ENV JDK_JAVA_OPTIONS="--sun-misc-unsafe-memory-access=allow \
 --enable-native-access=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.base/java.nio=ALL-UNNAMED \
 --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
 -Dio.netty.tryReflectionSetAccessible=true"
# 9070 = admin MainHttpRouter, 9080 = ObserverHttpRouter (see configs/http-routing-configs.json)
EXPOSE 9070 9080
ENTRYPOINT ["java", "-jar", "leo-main-starter-docker.jar"]
