# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

LEO CDP is an open-source, self-hosted AI-first Customer Data Platform. This repo is a **monorepo of loosely-coupled modules**; the heart is `core-leo-cdp` (a Java 11 / Vert.x backend). The other top-level folders are companion services and assets:

- `core-leo-cdp/` — the main backend: event ingestion, customer profiles, segmentation, campaigns, admin web app. **Most work happens here.**
- `airflow-ai-agent/` — Python / Apache Airflow 2.11 DAGs triggered by Redis Pub/Sub; runs AI workflows (Gemini, LangChain) that read/write CDP data.
- `core-customer360/` — PostgreSQL 16 + pgvector + PostGIS schema (`*.sql`) for the golden-record / Customer 360 graph model. SQL + docs, no application code.
- `chrome-ext/` — browser-extension build of the `leo.observer` client tracking SDK.
- `tests_with_k6/` — k6 load/performance tests against the running CDP.
- `web_tools/`, `documents/` — standalone HTML utilities and reference docs.

There is no single build that spans all modules — each is built and run independently.

## core-leo-cdp: build & run

Build is **Gradle 6.9.4** (legacy; uses the old `maven` and `org.gradlewebtools.minify` plugins). There is **no `gradlew` wrapper** — a system `gradle` 6.9.x must be on PATH. Source/target is **Java 11** (Amazon Corretto in production).

```bash
cd core-leo-cdp
gradle AutoBuildForDeployment      # default task: minify JS, build JARs, copy deps/configs/resources
# or, interactive first-time setup that writes leocdp-build.properties then builds:
./build.sh
```

`leocdp-build.properties` (gitignored, created by `build.sh` or by hand) defines `buildOutputFolderPath`, `staticOutputFolderPath`, and `buildVersion`. The build copies runtime JARs into `<buildOutput>/deps/` and writes manifests with a `Class-Path` pointing there — the deployed app runs from that output folder, **not** from a single fat Jar.

Key Gradle tasks (`gradle tasks` to list all):
- `BuildAllJavaJars` — builds the five starter JARs only.
- `minifyJsAdminResource`, `minifyJsLeoObserver` — minify admin UI JS and the client tracker (`public/js/leo-observer-src` → `public/js/leo-observer`). Minified JS is committed and CDN-published, so **re-run the minify task after editing source JS**.

### Five entry points (starters)

`core-leo-cdp` produces several deployable JARs from one codebase, each with a different `Main-Class` in `leotech.starter`:

| Starter | Role |
|---|---|
| `MainHttpStarter` | Admin web app + main API (default service) |
| `DataObserverStarter` | Event/tracking ingestion endpoint (`leo.observer.js` posts here) |
| `ScheduledJobStarter` | Quartz scheduled jobs (segmentation refresh, scoring, connectors) |
| `DataProcessingStarter` | Kafka-based data pipeline processing |
| `UploadFileHttpStarter` | File upload service |

`MainHttpStarter` also doubles as the **system admin CLI**. With no args it starts the HTTP worker; with args it runs management commands:

```bash
java -jar leo-main-starter-<ver>.jar setup-system-with-password <password>  # first-time bootstrap
java -jar leo-main-starter-<ver>.jar upgrade-system                         # upgrade core
java -jar leo-main-starter-<ver>.jar upgrade-system-and-reset-configs
java -jar leo-main-starter-<ver>.jar upgrade-index-database
java -jar leo-main-starter-<ver>.jar <httpRouterKey>                        # start a specific worker
```

In production, the `devops-script/shell-script-starter/*.sh` scripts (`start-admin.sh`, `start-observer.sh`, `start-data-connector-jobs.sh`) launch these JARs with a `HTTP_ROUTER_KEY` argument.

### Tests

JUnit 5 + AssertJ + Selenium + datafaker (`gradle test`). **Caveat:** `src/test/java/test/**` is mostly *manual* runnable utilities and integration probes (e.g. `test.cdp.DataSampleSetup`, `GenerateCdpTestData`, `SystemUserResetPassword`) with `main()` methods, not pure unit tests — most require a live ArangoDB + Redis and will not pass in isolation. Run a single test class via your IDE or `gradle test --tests "test.cdp.profile.ProfileMergeServiceTest"`.

## core-leo-cdp: architecture

### HTTP layer (Vert.x 3.8.5)

Workers are configured **declaratively** in `configs/http-routing-configs.json`: each named key (e.g. `localLeoMainAdminWorker`, `localLeoObserverWorker`) maps to a host, port, and a `classNameHttpRouter`. The starter is given a router key and looks up its config. Multiple admin/observer workers run on adjacent ports for horizontal scaling.

Routers live in `leotech.starter.router` (`MainHttpRouter`, `AdminHttpRouter`, `ObserverHttpRouter`, `CdpPublicApiRouter`, `UploaderHttpRouter`) and extend `leotech.system.common.BaseWebRouter` → `BaseHttpRouter`. The routing pattern: a static `ROUTE_REGISTRY` `Map<urlPrefix, Function<BaseWebRouter, Handler>>` dispatches a request prefix (e.g. `/cdp/profile`, `/cdp/segment`) to a handler factory. To add an endpoint, register a prefix → handler in the router and implement the handler in `leotech.cdp.handler.admin` (or `.api`).

### Domain layering (`leotech.cdp`)

Classic three-layer split — when changing a feature you usually touch all three:

- `cdp/model/**` — entity/data classes (profiles, segments, events, campaigns, assets, journeys). Persisted as ArangoDB documents.
- `cdp/dao/**` — persistence. `*DaoUtil` / `*Dao` classes wrap ArangoDB access; `AbstractCdpDatabaseUtil` is the base. AQL query templates live in `resources/database/database-query-template.aql`.
- `cdp/domain/**` — business logic services named `*Management` (e.g. `ProfileDataManagement`, `SegmentDataManagement`, `IdentityResolutionManagement`, `CampaignManagement`). HTTP handlers call these.

`leotech.web` handles public content delivery; `leotech.system` holds cross-cutting infrastructure (config loading, base router classes, email, templating, utils).

### Data stores

- **ArangoDB 3.11** — primary multi-model store (documents + graph + search) for profiles, events, relationships, identity resolution.
- **PostgreSQL 16 + pgvector** — golden-record master profiles and vector/semantic search (see `core-customer360`).
- **Redis** — caching, real-time persona lookup, and the Pub/Sub channel (`agent_pubsub_queue`) that triggers Airflow AI DAGs.
- **Kafka** — event streaming for the data-processing pipeline.

### Configuration

Runtime config is **file-based**, loaded relative to the working directory:
- `leocdp-metadata.properties` — master config (`SystemMetaData` reads it; defines `httpRoutingConfigAdmin`, `httpRoutingConfigObserver`, etc.).
- `configs/` — `http-routing-configs.json`, `redis-configs.json`, `scheduled-jobs-configs.json`, `app-metadata-configs.json`, `kafka-*.properties`, `log4j.xml`.

ArangoDB connection can alternatively be injected via **environment variables** (`ARANGODB_HOST/PORT/USERNAME/PASSWORD/DATABASE`) when `dbId == SYSTEM_ENV_VARS` — used for Docker/K8s deployments (see `NOTES-FOR-ARANGODB.md`).

### Local / vendored libraries

`core-leo-cdp/ext-lib/*.jar` are vendored dependencies pulled in via `fileTree` in `build.gradle` — they are **not on Maven Central**:
- `rfx-core-1.0.jar` — the `rfx.core.*` package (e.g. `rfx.core.util.StringUtil`) is from here, not a public dependency. Heavily used throughout.
- `query-builder-2.1.1.jar` — `com.itfsw.query.builder` AQL rule parser (powers segment query building).
- `zalo-java-sdk` — Zalo (Vietnamese messaging) integration.

Logging is forced to **Log4j2 via SLF4J**; logback is explicitly excluded in `build.gradle` (`configurations.all { exclude module: 'ch.qos.logback' }`).

## Cross-module integration

The client tracker `leo.observer.js` (built from `core-leo-cdp/public/js/leo-observer-src`) posts events to a `DataObserverStarter` worker → stored in ArangoDB → Airflow DAGs (triggered via Redis Pub/Sub with `{"dag_id":..., "params":{...}}`) run AI enrichment → results land in PostgreSQL/pgvector for Customer 360 and segmentation. Keep this flow in mind when a change spans tracking, ingestion, and AI processing.
