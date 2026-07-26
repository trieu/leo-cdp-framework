"""FastAPI application entrypoint for the Customer 360 admin frontend.

Serves the static single-page admin UI (index.html + static/) -- plain
HTML/CSS/JS using Tailwind, jQuery and Handlebars via CDN (see README.md).
Nothing here talks to a database; all profile/business data is fetched
client-side, live, from customer360-api.

The only dynamic piece is static/js/config.js: instead of serving the plain
file from disk, an explicit route renders jinja/config.js.j2 on every
request so the window.C360.config defaults (apiBase/tenantId) are overridden
from environment variables (FRONTEND_API_HOSTNAME / FRONTEND_TENANT_ID, see
.env) without rebuilding/editing the frontend JS.

Run with:

    uvicorn app:app --reload

or simply:

    python app.py
"""

import os
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

BASE_DIR = Path(__file__).resolve().parent

# Only relevant when running directly on the host (`python app.py` /
# `uvicorn app:app`) -- start.sh/restart.sh already `source .env` before
# invoking uvicorn, and in Docker the API hostname is injected via
# `environment:`/`-e`, not a file (no .env is copied into the image).
load_dotenv(BASE_DIR / ".env")

API_HOSTNAME = os.getenv("FRONTEND_API_HOSTNAME", "http://localhost:8000").rstrip("/")
API_BASE = f"{API_HOSTNAME}/api/v1"
TENANT_ID = os.getenv("FRONTEND_TENANT_ID", "11111111-1111-1111-1111-111111111111")

app = FastAPI(
    title="frontend-admin",
    description=(
        "Static Customer 360 admin UI (index.html + static assets). "
        "static/js/config.js is rendered from a Jinja2 template so the API "
        "hostname (FRONTEND_API_HOSTNAME) is injected at request time."
    ),
    version="1.0.0",
)

templates = Jinja2Templates(directory=str(BASE_DIR / "jinja"))


@app.get("/", include_in_schema=False)
def index():
    return FileResponse(BASE_DIR / "index.html")


@app.get("/static/js/config.js", include_in_schema=False)
def config_js(request: Request):
    """Renders jinja/config.js.j2, overriding the window.C360.config defaults
    with FRONTEND_API_HOSTNAME/FRONTEND_TENANT_ID.

    Registered before the `/static` mount below so this route takes
    precedence over the plain static/js/config.js file on disk (that file is
    kept only as a fallback default for serving this folder with a plain
    static file server -- see README.md).
    """
    return templates.TemplateResponse(
        request,
        "config.js.j2",
        {"api_base": API_BASE, "tenant_id": TENANT_ID},
        media_type="application/javascript",
    )


# Mounted last so the explicit routes above (config.js override) always win.
app.mount("/static", StaticFiles(directory=str(BASE_DIR / "static")), name="static")


@app.get("/health", tags=["Health"])
def health():
    return {"service": "frontend-admin", "status": "ok", "api_base": API_BASE}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=8890, reload=True)
