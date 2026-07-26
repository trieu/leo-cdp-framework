"""Pydantic schemas for CRM-style entities (Campaign, Lead, Contact, ...).

Each entity has: Base (shared writable fields) -> Create -> Update (all
optional, for PATCH) -> Read (adds server-generated id/timestamps).
``embedding`` vector columns are intentionally omitted from all schemas to
keep API payloads small; they can be added back with an explicit opt-in
query param if ever needed.
"""

import uuid
from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


class CampaignBase(BaseModel):
    name: str
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    metadata_: Optional[dict] = None


class CampaignCreate(CampaignBase):
    pass


class CampaignUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    metadata_: Optional[dict] = None


class CampaignRead(CampaignBase):
    model_config = ConfigDict(from_attributes=True)
    campaign_id: uuid.UUID
    created_at: Optional[datetime] = None


class CampaignMemberBase(BaseModel):
    campaign_id: Optional[uuid.UUID] = None
    contact_id: Optional[uuid.UUID] = None
    status: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class CampaignMemberCreate(CampaignMemberBase):
    pass


class CampaignMemberUpdate(BaseModel):
    campaign_id: Optional[uuid.UUID] = None
    contact_id: Optional[uuid.UUID] = None
    status: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class CampaignMemberRead(CampaignMemberBase):
    model_config = ConfigDict(from_attributes=True)
    campaign_member_id: uuid.UUID
    joined_at: Optional[datetime] = None


class LeadBase(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class LeadCreate(LeadBase):
    pass


class LeadUpdate(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class LeadRead(LeadBase):
    model_config = ConfigDict(from_attributes=True)
    lead_id: uuid.UUID
    created_at: Optional[datetime] = None


class LeadSourceBase(BaseModel):
    name: str
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class LeadSourceCreate(LeadSourceBase):
    pass


class LeadSourceUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class LeadSourceRead(LeadSourceBase):
    model_config = ConfigDict(from_attributes=True)
    lead_source_id: uuid.UUID


class ContactBase(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    account_id: Optional[uuid.UUID] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class ContactCreate(ContactBase):
    pass


class ContactUpdate(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    account_id: Optional[uuid.UUID] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class ContactRead(ContactBase):
    model_config = ConfigDict(from_attributes=True)
    contact_id: uuid.UUID
    created_at: Optional[datetime] = None


class AccountBase(BaseModel):
    name: str
    industry_id: Optional[uuid.UUID] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class AccountCreate(AccountBase):
    pass


class AccountUpdate(BaseModel):
    name: Optional[str] = None
    industry_id: Optional[uuid.UUID] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class AccountRead(AccountBase):
    model_config = ConfigDict(from_attributes=True)
    account_id: uuid.UUID
    created_at: Optional[datetime] = None


class OpportunityBase(BaseModel):
    account_id: Optional[uuid.UUID] = None
    name: Optional[str] = None
    value: Optional[float] = None
    stage: Optional[str] = None
    close_date: Optional[date] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class OpportunityCreate(OpportunityBase):
    pass


class OpportunityUpdate(BaseModel):
    account_id: Optional[uuid.UUID] = None
    name: Optional[str] = None
    value: Optional[float] = None
    stage: Optional[str] = None
    close_date: Optional[date] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class OpportunityRead(OpportunityBase):
    model_config = ConfigDict(from_attributes=True)
    opportunity_id: uuid.UUID
    created_at: Optional[datetime] = None


class IndustryBase(BaseModel):
    name: str
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class IndustryCreate(IndustryBase):
    pass


class IndustryUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = None
    metadata_: Optional[dict] = None


class IndustryRead(IndustryBase):
    model_config = ConfigDict(from_attributes=True)
    industry_id: uuid.UUID
