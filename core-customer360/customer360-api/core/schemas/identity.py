"""Pydantic schemas for the Customer Identity Resolution (CIR) core models:
master profiles, raw profile staging, profile links, and the matching-rule
metadata / throttle-status tables consumed by identity-resolution-service.
"""

import uuid
from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class MasterProfileBase(BaseModel):
    tenant_id: uuid.UUID
    domain: str = Field(default="retail", pattern="^(retail|banking|real_estate|travel)$")

    full_name: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    is_hashed: Optional[bool] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    secondary_emails: Optional[list[dict]] = None
    secondary_phones: Optional[list[dict]] = None
    date_of_birth: Optional[date] = None
    gender: Optional[str] = None
    address: Optional[dict] = None

    external_ids: Optional[dict] = None
    device_ids: Optional[list[str]] = None
    advertising_ids: Optional[list[str]] = None
    cookie_ids: Optional[list[str]] = None
    push_tokens: Optional[dict] = None

    loyalty_id: Optional[str] = None
    membership_tier: Optional[str] = None
    preferred_store_code: Optional[str] = None

    national_id: Optional[str] = None
    cif_number: Optional[str] = None
    account_numbers: Optional[list[str]] = None
    kyc_status: Optional[str] = None
    risk_segment: Optional[str] = None

    acquisition_source: Optional[str] = None
    acquisition_campaign: Optional[str] = None
    persona_name: Optional[str] = None
    segmentation_tags: Optional[list[str]] = None
    attributes: Optional[dict] = None
    source_systems: Optional[list[str]] = None
    first_seen_raw_profile_id: Optional[uuid.UUID] = None

    # Customer lifecycle & engagement tracking (prospect -> lead -> customer).
    customer_since: Optional[date] = None
    last_activity_at: Optional[datetime] = None
    preferred_channel: Optional[str] = None
    lifecycle_stage: Optional[str] = Field(
        default=None, pattern="^(prospect|lead|customer|vip|dormant|churn_risk)$"
    )
    persona_summary: Optional[str] = None

    # ML & Analytics scoring models (Lead, Churn, CLV, CX, Data Quality).
    lead_conversion_probability: Optional[Decimal] = None
    lead_grade: Optional[str] = None
    churn_probability: Optional[Decimal] = None
    churn_risk_tier: Optional[str] = Field(default=None, pattern="^(low|medium|high|critical)$")
    historical_clv: Optional[Decimal] = None
    predictive_clv: Optional[Decimal] = None
    clv_segment: Optional[str] = None
    engagement_score: Optional[Decimal] = None
    latest_nps_score: Optional[int] = Field(default=None, ge=0, le=10)
    average_csat: Optional[Decimal] = None
    overall_sentiment_score: Optional[Decimal] = None
    profile_completeness_score: Optional[Decimal] = None
    identity_confidence_score: Optional[Decimal] = None
    model_versions: Optional[dict] = None
    scores_updated_at: Optional[datetime] = None


class MasterProfileCreate(MasterProfileBase):
    pass


class MasterProfileUpdate(BaseModel):
    domain: Optional[str] = Field(default=None, pattern="^(retail|banking|real_estate|travel)$")
    full_name: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    is_hashed: Optional[bool] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    secondary_emails: Optional[list[dict]] = None
    secondary_phones: Optional[list[dict]] = None
    date_of_birth: Optional[date] = None
    gender: Optional[str] = None
    address: Optional[dict] = None
    external_ids: Optional[dict] = None
    device_ids: Optional[list[str]] = None
    advertising_ids: Optional[list[str]] = None
    cookie_ids: Optional[list[str]] = None
    push_tokens: Optional[dict] = None
    loyalty_id: Optional[str] = None
    membership_tier: Optional[str] = None
    preferred_store_code: Optional[str] = None
    national_id: Optional[str] = None
    cif_number: Optional[str] = None
    account_numbers: Optional[list[str]] = None
    kyc_status: Optional[str] = None
    risk_segment: Optional[str] = None
    acquisition_source: Optional[str] = None
    acquisition_campaign: Optional[str] = None
    persona_name: Optional[str] = None
    segmentation_tags: Optional[list[str]] = None
    attributes: Optional[dict] = None
    source_systems: Optional[list[str]] = None
    customer_since: Optional[date] = None
    last_activity_at: Optional[datetime] = None
    preferred_channel: Optional[str] = None
    lifecycle_stage: Optional[str] = Field(
        default=None, pattern="^(prospect|lead|customer|vip|dormant|churn_risk)$"
    )
    persona_summary: Optional[str] = None
    lead_conversion_probability: Optional[Decimal] = None
    lead_grade: Optional[str] = None
    churn_probability: Optional[Decimal] = None
    churn_risk_tier: Optional[str] = Field(default=None, pattern="^(low|medium|high|critical)$")
    historical_clv: Optional[Decimal] = None
    predictive_clv: Optional[Decimal] = None
    clv_segment: Optional[str] = None
    engagement_score: Optional[Decimal] = None
    latest_nps_score: Optional[int] = Field(default=None, ge=0, le=10)
    average_csat: Optional[Decimal] = None
    overall_sentiment_score: Optional[Decimal] = None
    profile_completeness_score: Optional[Decimal] = None
    identity_confidence_score: Optional[Decimal] = None
    model_versions: Optional[dict] = None
    scores_updated_at: Optional[datetime] = None
    status_code: Optional[int] = None


class MasterProfileRead(MasterProfileBase):
    model_config = ConfigDict(from_attributes=True)
    master_profile_id: uuid.UUID
    status_code: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class RawProfileBase(BaseModel):
    tenant_id: uuid.UUID
    domain: str = Field(default="retail", pattern="^(retail|banking|real_estate|travel)$")
    source_system: str
    channel: Optional[str] = None

    external_customer_id: Optional[str] = None
    full_name: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    national_id: Optional[str] = None

    device_id: Optional[str] = None
    advertising_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    push_token: Optional[str] = None
    cookie_id: Optional[str] = None
    ga_client_id: Optional[str] = None
    session_id: Optional[str] = None
    ip_address: Optional[str] = None
    user_agent: Optional[str] = None

    media_source: Optional[str] = None
    campaign: Optional[str] = None
    utm_source: Optional[str] = None
    utm_medium: Optional[str] = None
    utm_campaign: Optional[str] = None

    event_name: Optional[str] = None
    event_time: Optional[datetime] = None
    event_payload: Optional[dict] = None


class RawProfileCreate(RawProfileBase):
    pass


class RawProfileUpdate(BaseModel):
    channel: Optional[str] = None
    external_customer_id: Optional[str] = None
    full_name: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    national_id: Optional[str] = None
    device_id: Optional[str] = None
    advertising_id: Optional[str] = None
    platform: Optional[str] = None
    app_version: Optional[str] = None
    push_token: Optional[str] = None
    cookie_id: Optional[str] = None
    ga_client_id: Optional[str] = None
    session_id: Optional[str] = None
    ip_address: Optional[str] = None
    user_agent: Optional[str] = None
    media_source: Optional[str] = None
    campaign: Optional[str] = None
    utm_source: Optional[str] = None
    utm_medium: Optional[str] = None
    utm_campaign: Optional[str] = None
    event_name: Optional[str] = None
    event_time: Optional[datetime] = None
    event_payload: Optional[dict] = None
    status_code: Optional[int] = None


class RawProfileRead(RawProfileBase):
    model_config = ConfigDict(from_attributes=True)
    raw_profile_id: uuid.UUID
    status_code: int
    processed_at: Optional[datetime] = None
    created_at: Optional[datetime] = None


class ProfileLinkBase(BaseModel):
    tenant_id: uuid.UUID
    raw_profile_id: uuid.UUID
    master_profile_id: uuid.UUID
    match_score: Optional[Decimal] = None
    match_method: Optional[str] = None


class ProfileLinkCreate(ProfileLinkBase):
    pass


class ProfileLinkRead(ProfileLinkBase):
    model_config = ConfigDict(from_attributes=True)
    link_id: uuid.UUID
    created_at: Optional[datetime] = None


class ProfileAttributeBase(BaseModel):
    attribute_internal_code: str
    master_profile_column: Optional[str] = None
    name: str
    description: Optional[str] = None
    attribute_group: str = "GENERAL"
    source_table: str = "cdp_master_profiles"
    status: str = "ACTIVE"
    data_type: str = "TEXT"
    domain_scope: str = Field(default="all", pattern="^(all|retail|banking|real_estate|travel)$")
    is_pii: bool = False

    is_identity_resolution: bool = False
    matching_rule: Optional[str] = Field(default=None, pattern="^(exact|fuzzy_trgm|fuzzy_dmetaphone|none)$")
    matching_threshold: Optional[Decimal] = None
    consolidation_rule: Optional[str] = None

    is_scoring_model: bool = False
    scoring_model_name: Optional[str] = None
    scoring_model_version: Optional[str] = None
    value_type: Optional[str] = None
    value_min: Optional[Decimal] = None
    value_max: Optional[Decimal] = None
    refresh_frequency: Optional[str] = None
    display_order: int = 0


class ProfileAttributeCreate(ProfileAttributeBase):
    pass


class ProfileAttributeUpdate(BaseModel):
    master_profile_column: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    attribute_group: Optional[str] = None
    source_table: Optional[str] = None
    status: Optional[str] = None
    data_type: Optional[str] = None
    domain_scope: Optional[str] = Field(default=None, pattern="^(all|retail|banking|real_estate|travel)$")
    is_pii: Optional[bool] = None
    is_identity_resolution: Optional[bool] = None
    matching_rule: Optional[str] = Field(default=None, pattern="^(exact|fuzzy_trgm|fuzzy_dmetaphone|none)$")
    matching_threshold: Optional[Decimal] = None
    consolidation_rule: Optional[str] = None
    is_scoring_model: Optional[bool] = None
    scoring_model_name: Optional[str] = None
    scoring_model_version: Optional[str] = None
    value_type: Optional[str] = None
    value_min: Optional[Decimal] = None
    value_max: Optional[Decimal] = None
    refresh_frequency: Optional[str] = None
    display_order: Optional[int] = None


class ProfileAttributeRead(ProfileAttributeBase):
    model_config = ConfigDict(from_attributes=True)
    id: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class IdResolutionStatusRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: bool
    last_executed_at: Optional[datetime] = None
