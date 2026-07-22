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
from sqlalchemy import text

from core.database import engine
from core.routers.crm import all_crm_routers
from core.routers.graph import router as graph_router
from core.routers.identity import all_identity_routers
from core.routers.relations import all_relations_routers
from core.routers.reporting import router as reporting_router

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

# CIR core models first (primary focus of this API), then supporting CRM /
# relations / graph entities.
for r in all_identity_routers:
    app.include_router(r, prefix="/api/v1")
app.include_router(reporting_router, prefix="/api/v1")
for r in all_relations_routers:
    app.include_router(r, prefix="/api/v1")
app.include_router(graph_router, prefix="/api/v1")
for r in all_crm_routers:
    app.include_router(r, prefix="/api/v1")


@app.get("/", tags=["Health"])
def root():
    return {"service": "customer360-api", "status": "ok", "docs": "/docs"}


@app.get("/health", tags=["Health"])
def health():
    """Verifies the pooled SQLAlchemy engine can actually reach PostgreSQL."""
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    return {"status": "ok", "database": "reachable"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
