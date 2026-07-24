"""Pydantic schemas for RelationType, CdpRelation, CustomerContact, Transaction."""

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


class TransactionBase(BaseModel):
    tenant_id: uuid.UUID
    master_profile_id: Optional[uuid.UUID] = None
    source_system: Optional[str] = None
    source_transaction_id: Optional[str] = None
    transaction_type: Optional[str] = None
    transaction_status: Optional[str] = None
    entity_type: Optional[str] = None
    entity_id: Optional[str] = None
    entity_name: Optional[str] = None
    quantity: Optional[Decimal] = None
    amount: Optional[Decimal] = None
    currency: Optional[str] = None
    channel: Optional[str] = None
    merchant_id: Optional[str] = None
    merchant_name: Optional[str] = None
    location_id: Optional[str] = None
    location_name: Optional[str] = None
    campaign_id: Optional[str] = None
    campaign_name: Optional[str] = None
    staff_id: Optional[str] = None
    staff_name: Optional[str] = None
    transaction_time: Optional[datetime] = None
    attributes: Optional[dict] = None


class TransactionCreate(TransactionBase):
    pass


class TransactionUpdate(BaseModel):
    master_profile_id: Optional[uuid.UUID] = None
    source_system: Optional[str] = None
    source_transaction_id: Optional[str] = None
    transaction_type: Optional[str] = None
    transaction_status: Optional[str] = None
    entity_type: Optional[str] = None
    entity_id: Optional[str] = None
    entity_name: Optional[str] = None
    quantity: Optional[Decimal] = None
    amount: Optional[Decimal] = None
    currency: Optional[str] = None
    channel: Optional[str] = None
    merchant_id: Optional[str] = None
    merchant_name: Optional[str] = None
    location_id: Optional[str] = None
    location_name: Optional[str] = None
    campaign_id: Optional[str] = None
    campaign_name: Optional[str] = None
    staff_id: Optional[str] = None
    staff_name: Optional[str] = None
    transaction_time: Optional[datetime] = None
    attributes: Optional[dict] = None


class TransactionRead(TransactionBase):
    model_config = ConfigDict(from_attributes=True)
    transaction_id: uuid.UUID
    imported_at: Optional[datetime] = None
    created_at: Optional[datetime] = None
