"""Router for the partitioned graph_edges table.

Custom (not the generic factory) because graph_edges has a composite primary
key (edge_id, relation) required by PostgreSQL list partitioning. edge_id
alone is still globally unique (BIGSERIAL on the parent table), so lookups
below key off edge_id only for convenience.
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from core.config import settings
from core.database import get_db
from core.models.graph import GraphEdge
from core.schemas.graph import GraphEdgeCreate, GraphEdgeRead

router = APIRouter(prefix="/graph-edges", tags=["Graph"])


@router.get("/", response_model=list[GraphEdgeRead])
def list_edges(
    relation: str | None = None,
    from_id: str | None = None,
    to_id: str | None = None,
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    stmt = select(GraphEdge)
    if relation:
        stmt = stmt.where(GraphEdge.relation == relation)
    if from_id:
        stmt = stmt.where(GraphEdge.from_id == from_id)
    if to_id:
        stmt = stmt.where(GraphEdge.to_id == to_id)
    stmt = stmt.offset(skip).limit(limit)
    return db.execute(stmt).scalars().all()


@router.get("/count")
def count_edges(db: Session = Depends(get_db)):
    return {"count": db.execute(select(func.count()).select_from(GraphEdge)).scalar_one()}


@router.get("/{edge_id}", response_model=GraphEdgeRead)
def get_edge(edge_id: int, db: Session = Depends(get_db)):
    obj = db.execute(select(GraphEdge).where(GraphEdge.edge_id == edge_id)).scalar_one_or_none()
    if obj is None:
        raise HTTPException(status_code=404, detail=f"GraphEdge '{edge_id}' not found")
    return obj


@router.post("/", response_model=GraphEdgeRead, status_code=201)
def create_edge(payload: GraphEdgeCreate, db: Session = Depends(get_db)):
    obj = GraphEdge(**payload.model_dump())
    db.add(obj)
    db.commit()
    db.refresh(obj)
    return obj


@router.delete("/{edge_id}", status_code=204)
def delete_edge(edge_id: int, db: Session = Depends(get_db)):
    obj = db.execute(select(GraphEdge).where(GraphEdge.edge_id == edge_id)).scalar_one_or_none()
    if obj is None:
        raise HTTPException(status_code=404, detail=f"GraphEdge '{edge_id}' not found")
    db.delete(obj)
    db.commit()
