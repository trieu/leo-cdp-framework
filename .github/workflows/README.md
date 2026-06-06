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
| 6 | Pull Request DEV → MAIN (AI review by GitHub Copilot) | — (Copilot, native) |
| 7 | Project Owner — review / approve / merge | — |
| 8 | Build Agent / CI Runner — build, version tag, Trivy scan | `detect` → `release` |
| 9 | Public Docker Repo — GHCR | `release` |
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
6. **Pull Request `DEV → MAIN`** — **GitHub Copilot** posts an automatic AI code review.
   This is a native GitHub feature (not a workflow job) — enable it once via repo
   **Settings → Rules** (a ruleset requesting Copilot review) or per-PR by adding
   **Copilot** as a reviewer. Requires a Copilot license on the org/owner.
7. **Project Owner** — reviews, approves, and merges to `main`. The merge triggers the
   release.
8. **Build Agent / CI Runner** — jobs **`detect`** + **`release`** derive a version tag,
   build the Docker image, and run a Trivy security scan (gated on HIGH/CRITICAL).
9. **Public Docker Repo** — publishes the image to GHCR, tagged `:<version>` and `:latest`.
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
| `detect` | push to `main`, published Release | Derives the version tag (`YYYY.MM.DD-<shortsha>`). |
| `release` | after `detect` | Build, Trivy scan (fails on HIGH/CRITICAL), push `:version` + `:latest` to GHCR, then email stakeholders. |

> **AI code review** is handled by **GitHub Copilot** (native PR reviewer configured in
> repo settings), not by a workflow job — so it doesn't appear in this table.

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
| `SMTP_HOST` | `alert-on-failure`, `release` | For email | SMTP server hostname. |
| `SMTP_PORT` | `alert-on-failure`, `release` | For email | e.g. `465` or `587`. |
| `SMTP_USER` | `alert-on-failure`, `release` | For email | SMTP username. |
| `SMTP_PASS` | `alert-on-failure`, `release` | For email | SMTP password / app password. |
| `DEV_TEAM_EMAILS` | `alert-on-failure` | For failure alerts | Comma-separated recipients of CI-failure alerts. |
| `STAKEHOLDER_EMAILS` | `release` | For release notices | Comma-separated recipients of release notifications. |

> `GITHUB_TOKEN` is provided automatically by GitHub Actions — no setup needed. It
> authenticates the GHCR push. The pipeline publishes to **GHCR only**; no Docker
> registry username/password is required, and **no AI API key** (Copilot review is
> a native GitHub feature, not a workflow secret).

### 2. Permissions

- **GHCR publishing** needs `packages: write` (declared at the top of `ci-cd.yml`).
  Confirm **Settings → Actions → General → Workflow permissions** allows write access,
  or that the default `GITHUB_TOKEN` has package scope. The image name is
  `ghcr.io/<owner>/<repo>` (the `IMAGE` env), derived automatically — nothing to configure.

### 3. Branch protection (recommended)

Protect `main` and require these job status checks before merge:
- **validate**
- **docker**

(Optionally also require **GitHub Copilot** review via the same ruleset.)

---

## Usage

### Day-to-day

- **Develop on `dev`.** Every push runs CI validation and a commit-tagged image build.
  Watch the run under the **Actions** tab; a failing `validate` job emails the dev team.
- **Open a PR `dev → main`.** GitHub Copilot posts an automated review (if enabled in
  repo settings); the validation and docker build run as PR checks.
- **Merge to `main`** (owner only) to cut a release — the image is version-tagged,
  scanned, published, and stakeholders are emailed.

### Manual runs

The workflow exposes **`workflow_dispatch`** — trigger it from **Actions → CI/CD →
Run workflow**. It accepts a `platforms` input (`linux/amd64` or
`linux/amd64,linux/arm64`) used by the `release` job for multi-arch builds.

### Pulling a published image

```bash
# Latest release — GHCR
docker pull ghcr.io/<owner>/leo-cdp-framework:latest

# A specific commit (from the `docker` job)
docker pull ghcr.io/<owner>/leo-cdp-framework:<short-sha>
```

Replace `<owner>` with the GitHub org/user that owns this repository. The `release`
job tags GHCR with `:<version>` (`YYYY.MM.DD-<shortsha>`) and `:latest`.

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
