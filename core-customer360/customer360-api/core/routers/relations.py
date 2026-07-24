"""Routers for RelationType, CdpRelation, CustomerContact, Transaction --
supporting entities around a resolved master profile (interactions,
transactions, and the profile-to-profile relationship graph).
"""

import uuid

from core.models.relations import CdpRelation, CustomerContact, RelationType, Transaction
from core.routers._generic import build_crud_router
from core.schemas.relations import (
    CdpRelationCreate,
    CdpRelationRead,
    CdpRelationUpdate,
    CustomerContactCreate,
    CustomerContactRead,
    CustomerContactUpdate,
    RelationTypeCreate,
    RelationTypeRead,
    RelationTypeUpdate,
    TransactionCreate,
    TransactionRead,
    TransactionUpdate,
)

relation_types_router = build_crud_router(
    model=RelationType,
    pk_field="relation_type_id",
    pk_type=int,
    create_schema=RelationTypeCreate,
    update_schema=RelationTypeUpdate,
    read_schema=RelationTypeRead,
    prefix="/relation-types",
    tags=["Relations"],
)

cdp_relations_router = build_crud_router(
    model=CdpRelation,
    pk_field="relation_id",
    pk_type=uuid.UUID,
    create_schema=CdpRelationCreate,
    update_schema=CdpRelationUpdate,
    read_schema=CdpRelationRead,
    prefix="/relations",
    tags=["Relations"],
)

customer_contacts_router = build_crud_router(
    model=CustomerContact,
    pk_field="contact_id",
    pk_type=uuid.UUID,
    create_schema=CustomerContactCreate,
    update_schema=CustomerContactUpdate,
    read_schema=CustomerContactRead,
    prefix="/customer-contacts",
    tags=["Customer Interactions"],
)

transactions_router = build_crud_router(
    model=Transaction,
    pk_field="transaction_id",
    pk_type=uuid.UUID,
    create_schema=TransactionCreate,
    update_schema=TransactionUpdate,
    read_schema=TransactionRead,
    prefix="/transactions",
    tags=["Customer Interactions"],
)

all_relations_routers = [
    relation_types_router,
    cdp_relations_router,
    customer_contacts_router,
    transactions_router,
]
