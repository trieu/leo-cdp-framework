# Run LEO CDP via docker-compose (GHCR image)

Runs the **admin worker** of `core-leo-cdp` plus its dependencies (ArangoDB + Redis)
from the published image `ghcr.io/trieu/leo-cdp-framework`.

> **Why the mounts?** The published image ships **only the runnable JARs** — it contains
> no `configs/` and no `leocdp-metadata.properties`. So this compose mounts a runtime
> config set over `/app` and injects the ArangoDB credentials via env vars. Everything in
> this folder *is* that config set; edit it for your environment.

## Files

| File | Purpose |
|---|---|
| `docker-compose.yml` | arangodb + redis + leocdp-admin services |
| `.env` | `LEOCDP_TAG` (image tag to run) and `ARANGO_ROOT_PASSWORD` |
| `leocdp-metadata.properties` | master config — `mainDatabaseConfig=SYSTEM_ENV_VARS`, routing keys, `messageQueueType=local` |
| `configs/` | full config set the app reads at runtime (routing, redis, UA `regexes.yaml`, etc.) |

Container-specific values already set for you:
- `configs/http-routing-configs.json` → worker host **`0.0.0.0`** (the repo sample uses
  `cdpsys.admin`, which won't bind in a container).
- `configs/redis-configs.json` → host **`redis`**, port **6379**, no auth (matches the redis service).
- DB creds come from `ARANGODB_*` env in `docker-compose.yml` (`mainDatabaseConfig=SYSTEM_ENV_VARS`).

## Steps

```bash
cd core-leo-cdp/devops-script/docker-leocdp

# 1. Create your env file and edit it — set ARANGO_ROOT_PASSWORD and a real LEOCDP_TAG.
#    cp sample.env .env
#    NOTE: ':latest' only exists after a release on `main`. For a feature-branch build,
#    use the commit short-SHA tag, e.g. LEOCDP_TAG=5f688f0.

# 2. Start the dependencies and wait for ArangoDB to accept connections.
docker compose up -d arangodb redis
until curl -fs -o /dev/null -w '%{http_code}' http://localhost:8529/_api/version | grep -qE '^[0-9]'; do sleep 2; done

# 3. First-time bootstrap — creates collections + the super-admin account.
#    (Replace the password.) The super-admin is admin@example.com (from leocdp-metadata.properties).
docker compose run --rm leocdp-admin setup-system-with-password 'YourStrongPassword'

# 4. Start the admin worker.
docker compose up -d leocdp-admin

# 5. Open the admin UI.
#    http://localhost:9070/login   (login: admin@example.com / the password from step 3)
docker compose logs -f leocdp-admin
```

Health check: `curl http://localhost:9070/ping` → `200`.

## Run the other workers (optional)

The image bundles all four starters. Override the entrypoint jar to run a different worker
(each is its own compose service / command):

| Worker | Jar | Default port |
|---|---|---|
| Admin (default) | `leo-main-starter-docker.jar` | 9070 |
| Event Observer | `leo-observer-starter-docker.jar` | 9080 |
| Scheduled Jobs | `leo-scheduler-starter-docker.jar` | — |
| Data Processing | `leo-data-processing-starter-docker.jar` | — |

e.g. add a service with `entrypoint: ["java","-jar","leo-observer-starter-docker.jar"]`,
the same mounts/env, and publish `9080:9080`.

## Known issues (image `5f688f0`)

- **`setup-system-with-password` exits with code 1** after creating all collections and the
  super-admin, because the default-data seeder builds the default *Journey Map* from a single
  touchpoint hub while the builder requires ≥2 (`JourneyMapManagement.initDefaultSystemData`
  → `JourneyMap.setTouchpointHubsForJourneyMap` throws "touchpointHubs.size must be larger or
  equals 2"). The system is still usable (admin login works); only the default journey map is
  not seeded. Fix is a one-line source change (add a second default hub) + image rebuild.
- **`:latest` tag** is only published by the release job on `main`; feature-branch builds are
  tagged with the commit SHA only.
