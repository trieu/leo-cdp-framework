# Customer 360 frontend (static HTML)

`index.html` is a slim shell for a single-page admin (Tailwind CSS + jQuery 3 +
Handlebars templates, all via CDN) that mirrors
`../ui-wireframes/customer-360-profile-details.png`: a searchable master-profile
list and a profile detail dashboard (overview, attributes/segments, engagement
summary, cross-channel activity, timeline, scoring, personalized items). All
data is fetched live from `customer360-api` (FastAPI) which reads PostgreSQL --
nothing is hardcoded in the HTML.

## Structure

```
index.html                 slim shell: CDN <script>/<link> tags + empty mount points
static/css/app.css          small CSS additions on top of the Tailwind CDN build
static/js/config.js          API base/tenant config (localStorage) + ajax client
static/js/formatters.js      display formatters, label maps, badge-class helpers
static/js/templates.js       fetches + compiles every static/templates/*.html file
static/js/list-view.js       Master Profiles list (search/filter/pagination)
static/js/profile-detail-view.js Profile detail dashboard (view-model building + loads)
static/js/main.js            bootstraps templates, tab/view switching, settings modal
static/templates/tabs.html               header + nav bar (static)
static/templates/settings-modal.html     API base/tenant settings dialog (static)
static/templates/profiles-list.html      Master Profiles list shell (static)
static/templates/placeholder.html        "not implemented" shell for other nav tabs
static/templates/profiles-rows.html      Handlebars: list <tr> rows
static/templates/profile-details.html    Handlebars: detail grid, includes the partials below
static/templates/identity.html           partial: left profile identity card
static/templates/channels.html           partial: channels & identifiers card
static/templates/overview.html           partial: Profile Overview card
static/templates/segments.html           partial: Attributes & Segments card
static/templates/engagement.html         partial: Engagement Summary card
static/templates/activity.html           partial: Cross-Channel Activity card
static/templates/timeline.html           partial: Timeline card
static/templates/scoring.html            partial: Scoring & Value card
static/templates/personalized-items.html partial: Personalized Items card shell
static/templates/content-items.html      Handlebars: personalized item cards list
```

Each card in the profile detail dashboard is its own template file (registered
as a Handlebars partial by `static/js/templates.js` and included from
`profile-details.html` via `{{> name}}`), so adding/editing a single card never
requires touching the others.

## Run

1. Start `customer360-api` (see `../customer360-api/start.sh`) so it's listening on
   `http://localhost:8000`.
2. Serve this folder with any static file server (opening via `file://` will be
   blocked by the browser's CORS policy for the `static/templates/*.html` and API
   `fetch`/XHR calls), e.g.:
   ```bash
   cd core-customer360/frontend-admin
   python3 -m http.server 8890
   ```
   then open `http://localhost:8890/index.html`.
3. Click the "Admin" button (top right) to change the API base URL or the
   `X-Tenant-Id` dev header (used for Postgres Row-Level Security when
   `SSO_LOGIN=false`) if your setup differs from the defaults.

