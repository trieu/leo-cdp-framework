# Multi-stage build for the LEO CDP core backend (MainHttpStarter / admin worker).
# NOTE: starting point - the Gradle build pulls the JS-minify plugin over the network,
# and integration tests still need a real leocdp-metadata.properties, so expect one
# iteration before the image boots cleanly.

# ---- build stage ----
FROM amazoncorretto:11 AS build
RUN yum install -y unzip tar gzip && \
    curl -sL https://services.gradle.org/distributions/gradle-6.9.4-bin.zip -o /tmp/g.zip && \
    unzip -q /tmp/g.zip -d /opt && ln -s /opt/gradle-6.9.4/bin/gradle /usr/bin/gradle
WORKDIR /src
COPY . .
# Output goes to /dist with deps/ alongside; JAR manifest Class-Path points to ./deps
RUN gradle AutoBuildForDeployment --no-daemon \
      -PbuildOutputFolderPath=/dist -PstaticOutputFolderPath=/static -PbuildVersion=docker

# ---- test stage ----
# Runs the JUnit suite as part of the image build. Tolerant of failures (|| true) so the
# JUnit XML is always produced and can be exported — many test.** classes need a live
# ArangoDB/Redis and won't pass inside an isolated build (see CLAUDE.md). The workflow
# surfaces pass/fail from the exported reports.
FROM build AS test
RUN gradle test --no-daemon \
      -PbuildOutputFolderPath=/dist -PstaticOutputFolderPath=/static -PbuildVersion=docker || true
RUN mkdir -p /test-results && \
    cp -r /src/build/test-results/. /test-results/ 2>/dev/null || true

# ---- test-results export stage ----
# `scratch` so `docker buildx build --target test-export --output type=local` exports
# ONLY the JUnit XML, not a whole image filesystem.
FROM scratch AS test-export
COPY --from=test /test-results /

# ---- runtime stage ----
FROM amazoncorretto:11
WORKDIR /app
COPY --from=build /dist /app
# 9070 = admin MainHttpRouter, 9080 = ObserverHttpRouter (see configs/http-routing-configs.json)
EXPOSE 9070 9080
ENTRYPOINT ["java", "-jar", "leo-main-starter-docker.jar"]
