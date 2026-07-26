"""FastAPI application entrypoint for the Customer 360 / Identity Resolution API.

Connects to PostgreSQL via SQLAlchemy 2 ORM using a pooled engine (see
core/database.py: pool_size/max_overflow/pool_recycle/pool_pre_ping configured
from .env). Run with:

    uvicorn app:app --reload

or simply:

    python app.py
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from core.auth import auth_middleware
from core.config import settings
from core.database import engine
from core.init_core_data import init_core_data
from core.routers.content import all_content_routers
from core.routers.crm import all_crm_routers
from core.routers.events import all_events_routers
from core.routers.graph import router as graph_router
from core.routers.identity import all_identity_routers
from core.routers.relations import all_relations_routers
from core.routers.reporting import router as reporting_router
from core.routers.segment import all_segment_routers

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Customer 360 / Identity Resolution API",
    description=(
        "CRUD + reporting API over the customer360 PostgreSQL schema "
        "(core-customer360/database-schema.sql), covering CRM entities and "
        "the full Customer Identity Resolution (CIR) pipeline: raw profile "
        "staging (AppsFlyer/MoEngage/Web Tracking/...), master profiles, "
        "profile links, matching-rule metadata, and resolution reporting."
    ),
    version="1.0.0",
)

# Permissive CORS for local dev so the static frontend-admin HTML (opened via
# file:// or a plain dev static server on a different origin/port) can call
# this API. Not used with credentials, so allow_origins="*" is safe here.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.middleware("http")(auth_middleware)

# CIR core models first (primary focus of this API), then supporting CRM /
# relations / graph entities.
for r in all_identity_routers:
    app.include_router(r, prefix="/api/v1")
app.include_router(reporting_router, prefix="/api/v1")
for r in all_relations_routers:
    app.include_router(r, prefix="/api/v1")
for r in all_events_routers:
    app.include_router(r, prefix="/api/v1")
for r in all_content_routers:
    app.include_router(r, prefix="/api/v1")
app.include_router(graph_router, prefix="/api/v1")
for r in all_crm_routers:
    app.include_router(r, prefix="/api/v1")
for r in all_segment_routers:
    app.include_router(r, prefix="/api/v1")


@app.on_event("startup")
def _seed_core_data_on_startup() -> None:
    init_core_data()


@app.get("/", tags=["Health"])
def root():
    return {"service": "customer360-api", "status": "ok", "docs": "/docs"}


@app.get("/health", tags=["Health"])
def health():
    """Verifies the pooled SQLAlchemy engine can actually reach PostgreSQL."""
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    return {"status": "ok", "database": "reachable", "sso_login": settings.sso_login}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
