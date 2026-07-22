"""SQLAlchemy 2 engine + session factory with connection pooling.

Uses SQLAlchemy's default QueuePool for the psycopg2 driver, sized via
DB_POOL_SIZE / DB_MAX_OVERFLOW (.env), with pool_pre_ping to transparently
recover from dropped connections and pool_recycle to avoid stale connections.
"""

from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from core.config import settings

engine = create_engine(
    settings.database_url,
    pool_size=settings.db_pool_size,
    max_overflow=settings.db_max_overflow,
    pool_recycle=settings.db_pool_recycle_seconds,
    pool_pre_ping=settings.db_pool_pre_ping,
    echo=settings.db_echo_sql,
    future=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)


def get_db() -> Generator[Session, None, None]:
    """FastAPI dependency yielding a request-scoped SQLAlchemy session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
