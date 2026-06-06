# GitHub Actions Workflows

CI/CD automation for the **LEO CDP framework**. The pipeline covers the full path
from a developer commit through validation, AI-assisted review, owner approval, and
the published Docker release.

Everything lives in a **single workflow**, [`ci-cd.yml`](ci-cd.yml), scoped to
`core-leo-cdp` (the Java 11 / Vert.x backend) — the only module in this monorepo with
an automated build. Each pipeline stage is a separate **job** in that file, gated by an
`if:` on the triggering event.

---

## The flow

![CI/CD pipeline flow](flow.png)

> The diagram source is [flow.excalidraw](flow.excalidraw). Open it in the
> [Excalidraw VS Code extension](https://marketplace.visualstudio.com/items?itemName=pomdtr.excalidraw-editor)
> (or [excalidraw.com](https://excalidraw.com)) to edit, then **export as PNG** back
> to `flow.png` to keep this README in sync.

A vertical numbered flow, with each step mapped to the `ci-cd.yml` **job** that powers it:

| # | Stage | `ci-cd.yml` job |
|---|---|---|
| 1 | Jira Task | — |
| 2 | Developer | — |
| 3 | GitHub Repository (`leo-cdp-framework`) | — |
| 4 | DEV Branch — commit & push | — |
| 5 | Automated CI Validation → FAIL → Alert Email (red ✕) / PASS ↓ | `validate` + `docker` → `alert-on-failure` |
| 6 | Pull Request DEV → MAIN (+ `@gemini-cli` interactive aside) | `gemini-review` (+ `gemini`) |
| 7 | Project Owner — review / approve / merge | — |
| 8 | Build Agent / CI Runner — build, version tag, Trivy scan | `detect` → `release` |
| 9 | Public Docker Repo — GHCR + Docker Hub | `release` |
| 10 | Email Notification | `release` |
| 11 | Development Team — stakeholders | — |

The pipeline follows eleven stages, **Jira → GitHub → CI Validation → Approval →
Docker Build → Release Notification**:

1. **Jira Task** — a product-backlog item / feature request.
2. **Developer** — picks up the task and implements the feature in `core-leo-cdp`.
3. **GitHub Repository** — `leo-cdp-framework`; `dev` is the integration branch, `main`
   is the release branch.
4. **DEV Branch** — the developer commits & pushes code, which triggers CI.
5. **Automated CI Validation** — job **`validate`** (build + unit tests, code quality,
   integration tests against ArangoDB + Redis service containers, pinned to the same
   versions the [`devops-script/docker-arangodb`](../../core-leo-cdp/devops-script/docker-arangodb)
   compose uses for production parity) and job **`docker`** (JUnit inside the
   Docker build, image → GHCR tagged with the commit SHA) run on every push to `dev`,
   on PRs into `main`, and on a nightly cron.
   - **FAIL →** job **`alert-on-failure`** emails an alert to the dev team (bug report).
   - **PASS →** continue to the pull request.
6. **Pull Request `DEV → MAIN`** — opening it triggers job **`gemini-review`**, which
   posts an automatic AI code review (Google Gemini). *(Separately, job **`gemini`**
   lets anyone mention `@gemini-cli` in any issue or PR comment for an interactive
   assistant.)*
7. **Project Owner** — reviews, approves, and merges to `main`. The merge triggers the
   release.
8. **Build Agent / CI Runner** — jobs **`detect`** + **`release`** derive a version tag,
   build the Docker image, and run a Trivy security scan (gated on HIGH/CRITICAL).
9. **Public Docker Repo** — publishes the image to GHCR (always) and Docker Hub
   (if credentials are configured), tagged `:<version>` and `:latest`.
10. **Email Notification** — job **`release`** emails the release notification.
11. **Development Team** — Product Owner and stakeholders pull & deploy the released
    image.

---

## The jobs (all in [`ci-cd.yml`](ci-cd.yml))

| Job | Runs when (`if:`) | What it does |
|---|---|---|
| `validate` | push to `dev`, PR into `main`, nightly cron (`18:00 UTC`), manual | Build + unit tests, code quality, integration tests (ArangoDB 3.11.14 + Redis 7.4 service containers, pinned to the `devops-script/docker-arangodb` versions for production parity). |
| `alert-on-failure` | `validate` failed | Emails the dev team (bug report). |
| `docker` | any push, PR, manual | Builds the Docker image running the JUnit suite inside the multi-stage build and publishes the JUnit report. On a **push** (any branch) it pushes the runtime image to GHCR tagged with the commit SHA (full + short); on a **PR** it builds only (no push), validating the Dockerfile. |
| `detect` | push to `main`, published Release | Derives the version tag (`YYYY.MM.DD-<shortsha>`) and detects whether Docker Hub creds exist. |
| `release` | after `detect` | Build, Trivy scan (fails on HIGH/CRITICAL), push `:version` + `:latest` to GHCR and optionally Docker Hub, then email stakeholders. |
| `ai-check` | any PR / `@gemini-cli` comment event | Gate: outputs whether `GEMINI_API_KEY` is set so the AI jobs skip cleanly when it isn't. |
| `gemini-review` | every PR (same-repo and fork) | Automatic AI code review via the Gemini CLI `code-review` extension. |
| `gemini` | `@gemini-cli` in an issue/PR comment, review, or new issue | Runs Google's Gemini CLI to answer the request. Restricted to repo owners/members/collaborators. |

> All jobs share one workflow, so a single trigger (e.g. a `push`) starts the workflow
> and each job's `if:` decides whether it actually runs. Unlike separate files, GitHub
> **path filters can't be per-job**, so the old `core-leo-cdp/**` filter is dropped —
> event/branch gating in each `if:` replaces it.

---

## Setup

### 1. Repository secrets

Add these under **Settings → Secrets and variables → Actions**. The pipeline degrades
gracefully — workflows skip the steps whose secrets are missing rather than failing.

| Secret | Used by | Required? | Notes |
|---|---|---|---|
| `GEMINI_API_KEY` | `gemini-review`, `gemini` | For AI jobs | Free key from [Google AI Studio](https://aistudio.google.com/apikey). Without it, both Gemini jobs skip cleanly. |
| `SMTP_HOST` | `alert-on-failure`, `release` | For email | SMTP server hostname. |
| `SMTP_PORT` | `alert-on-failure`, `release` | For email | e.g. `465` or `587`. |
| `SMTP_USER` | `alert-on-failure`, `release` | For email | SMTP username. |
| `SMTP_PASS` | `alert-on-failure`, `release` | For email | SMTP password / app password. |
| `DEV_TEAM_EMAILS` | `alert-on-failure` | For failure alerts | Comma-separated recipients of CI-failure alerts. |
| `STAKEHOLDER_EMAILS` | `release` | For release notices | Comma-separated recipients of release notifications. |
| `DOCKER_USERNAME` | `release` | Optional | Enables Docker Hub publishing. Omit to publish to GHCR only. |
| `DOCKER_PASSWORD` | `release` | Optional | Docker Hub access token. |

> `GITHUB_TOKEN` is provided automatically by GitHub Actions — no setup needed. It
> authenticates the GHCR push and lets the Gemini workflows comment on PRs/issues.

### 2. Permissions

- **GHCR publishing** needs `packages: write` (declared at the top of `ci-cd.yml`).
  Confirm **Settings → Actions → General → Workflow permissions** allows write access,
  or that the default `GITHUB_TOKEN` has package scope.
- **Docker Hub** image name defaults to `tantrieuf31/leocdp-free-trial-edition`. To
  publish under your own namespace when you fork, set the repository **variable**
  `DOCKER_IMAGE` (Settings → Secrets and variables → Actions → **Variables**) — the
  `DOCKERHUB_IMAGE` env in [`ci-cd.yml`](ci-cd.yml) reads `${{ vars.DOCKER_IMAGE }}`
  and falls back to the default when unset. `DOCKER_USERNAME` must own / have push
  rights to whichever namespace you point it at.

### 3. Branch protection (recommended)

Protect `main` and require these job status checks before merge:
- **validate**
- **docker**
- **gemini-review**

---

## Usage

### Day-to-day

- **Develop on `dev`.** Every push runs CI validation and a commit-tagged image build.
  Watch the run under the **Actions** tab; a failing `validate` job emails the dev team.
- **Open a PR `dev → main`.** Gemini posts an automated review; the validation and
  docker build run as PR checks.
- **Ask `@gemini-cli` for help** by mentioning it in any PR or issue comment (e.g.
  *"@gemini-cli why is this test flaky?"* or `@gemini-cli /review` to re-run the review).
- **Merge to `main`** (owner only) to cut a release — the image is version-tagged,
  scanned, published, and stakeholders are emailed.

### Manual runs

The workflow exposes **`workflow_dispatch`** — trigger it from **Actions → CI/CD →
Run workflow**. It accepts a `platforms` input (`linux/amd64` or
`linux/amd64,linux/arm64`) used by the `release` job for multi-arch builds.

### Pulling a published image

```bash
# Latest release — GHCR (always published)
docker pull ghcr.io/<owner>/leo-cdp-framework:latest

# Latest release — Docker Hub (mirror, only if DOCKER_USERNAME/PASSWORD are set)
docker pull tantrieuf31/leocdp-free-trial-edition:latest

# A specific commit (from the `docker` job, GHCR only)
docker pull ghcr.io/<owner>/leo-cdp-framework:<short-sha>
```

Replace `<owner>` with the GitHub org/user that owns this repository. The `release`
job tags both registries with `:<version>` (`YYYY.MM.DD-<shortsha>`) and `:latest`.

### Deploying the image

The published image runs the same starter JARs the deploy scripts under
[`core-leo-cdp/devops-script/`](../../core-leo-cdp/devops-script/) launch — each worker
is started with an `HTTP_ROUTER_KEY` argument:

- `shell-script-starter/setup-new-leocdp.sh` — first-time bootstrap
  (`java -jar leo-main-starter-<ver>.jar setup-system-with-password <password>`).
- `shell-script-starter/start-admin.sh` — admin workers (`admin1`/`admin2`/`admin3`).
- `shell-script-starter/start-observer.sh` — event ingestion worker.
- `shell-script-starter/start-data-connector-jobs.sh` — scheduled connector jobs.
- `script-installation/` and `docker-arangodb/`, `docker-kafka/` — install Java/Redis/
  Nginx and stand up the ArangoDB + Kafka dependencies.
