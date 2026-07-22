"""Response schemas for the Customer Identity Resolution (CIR) reporting API.

These mirror the ad-hoc SQL queries in the "Phân tích & Báo cáo" section of
core-customer360/identity-resolution.md, exposed as proper JSON endpoints.
"""

import uuid
from typing import Optional

from pydantic import BaseModel


class StatusCodeCount(BaseModel):
    status_code: int
    label: str
    count: int


class DomainCount(BaseModel):
    domain: str
    count: int


class SourceSystemCount(BaseModel):
    source_system: str
    domain: str
    count: int


class IdentityGraphCoverage(BaseModel):
    total_master_profiles: int
    with_email: int
    with_phone_number: int
    with_device_id: int
    with_advertising_id: int
    with_cookie_id: int
    with_external_id: int
    with_national_id: int


class DuplicateMasterProfile(BaseModel):
    master_profile_id: uuid.UUID
    domain: str
    full_name: Optional[str] = None
    linked_raw_profile_count: int
    source_systems: Optional[list[str]] = None


class CirSummary(BaseModel):
    total_raw_profiles: int
    total_master_profiles: int
    processed_raw_profiles: int
    pending_raw_profiles: int
    in_progress_raw_profiles: int
    duplicate_master_profile_count: int
    raw_profiles_by_status: list[StatusCodeCount]
    raw_profiles_by_domain: list[DomainCount]
    master_profiles_by_domain: list[DomainCount]
    raw_profiles_by_source_system: list[SourceSystemCount]
