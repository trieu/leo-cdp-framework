"""Pydantic schemas for RelationType, CdpRelation, CustomerContact, Purchase."""

import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, ConfigDict


class RelationTypeBase(BaseModel):
    code: str
    description: Optional[str] = None


class RelationTypeCreate(RelationTypeBase):
    pass


class RelationTypeUpdate(BaseModel):
    code: Optional[str] = None
    description: Optional[str] = None


class RelationTypeRead(RelationTypeBase):
    model_config = ConfigDict(from_attributes=True)
    relation_type_id: int


class CdpRelationBase(BaseModel):
    tenant_id: uuid.UUID
    source_master_id: uuid.UUID
    target_master_id: uuid.UUID
    relation_type_id: int


class CdpRelationCreate(CdpRelationBase):
    pass


class CdpRelationUpdate(BaseModel):
    relation_type_id: Optional[int] = None


class CdpRelationRead(CdpRelationBase):
    model_config = ConfigDict(from_attributes=True)
    relation_id: uuid.UUID
    created_at: Optional[datetime] = None


class CustomerContactBase(BaseModel):
    tenant_id: uuid.UUID
    master_profile_id: uuid.UUID
    contact_type: Optional[str] = None
    contact_channel: Optional[str] = None
    contact_content: Optional[str] = None


class CustomerContactCreate(CustomerContactBase):
    pass


class CustomerContactUpdate(BaseModel):
    contact_type: Optional[str] = None
    contact_channel: Optional[str] = None
    contact_content: Optional[str] = None


class CustomerContactRead(CustomerContactBase):
    model_config = ConfigDict(from_attributes=True)
    contact_id: uuid.UUID
    contact_date: Optional[datetime] = None


class PurchaseBase(BaseModel):
    tenant_id: uuid.UUID
    master_profile_id: uuid.UUID
    product_id: Optional[uuid.UUID] = None
    product_name: Optional[str] = None
    amount: Optional[Decimal] = None
    currency: str = "USD"
    purchase_date: datetime


class PurchaseCreate(PurchaseBase):
    pass


class PurchaseUpdate(BaseModel):
    product_id: Optional[uuid.UUID] = None
    product_name: Optional[str] = None
    amount: Optional[Decimal] = None
    currency: Optional[str] = None
    purchase_date: Optional[datetime] = None


class PurchaseRead(PurchaseBase):
    model_config = ConfigDict(from_attributes=True)
    purchase_id: uuid.UUID
    created_at: Optional[datetime] = None
