"""CRM-style entity routers (Campaign, Lead, Contact, Account, Opportunity,
Industry, LeadSource, CampaignMember), all built via the generic CRUD
router factory since they share a simple single-column UUID primary key.
"""

import uuid

from core.models.crm import Account, Campaign, CampaignMember, Contact, Industry, Lead, LeadSource, Opportunity
from core.routers._generic import build_crud_router
from core.schemas.crm import (
    AccountCreate,
    AccountRead,
    AccountUpdate,
    CampaignCreate,
    CampaignMemberCreate,
    CampaignMemberRead,
    CampaignMemberUpdate,
    CampaignRead,
    CampaignUpdate,
    ContactCreate,
    ContactRead,
    ContactUpdate,
    IndustryCreate,
    IndustryRead,
    IndustryUpdate,
    LeadCreate,
    LeadRead,
    LeadSourceCreate,
    LeadSourceRead,
    LeadSourceUpdate,
    LeadUpdate,
    OpportunityCreate,
    OpportunityRead,
    OpportunityUpdate,
)

campaigns_router = build_crud_router(
    model=Campaign,
    pk_field="campaign_id",
    pk_type=uuid.UUID,
    create_schema=CampaignCreate,
    update_schema=CampaignUpdate,
    read_schema=CampaignRead,
    prefix="/campaigns",
    tags=["CRM - Campaigns"],
)

campaign_members_router = build_crud_router(
    model=CampaignMember,
    pk_field="campaign_member_id",
    pk_type=uuid.UUID,
    create_schema=CampaignMemberCreate,
    update_schema=CampaignMemberUpdate,
    read_schema=CampaignMemberRead,
    prefix="/campaign-members",
    tags=["CRM - Campaigns"],
)

leads_router = build_crud_router(
    model=Lead,
    pk_field="lead_id",
    pk_type=uuid.UUID,
    create_schema=LeadCreate,
    update_schema=LeadUpdate,
    read_schema=LeadRead,
    prefix="/leads",
    tags=["CRM - Leads"],
)

lead_sources_router = build_crud_router(
    model=LeadSource,
    pk_field="lead_source_id",
    pk_type=uuid.UUID,
    create_schema=LeadSourceCreate,
    update_schema=LeadSourceUpdate,
    read_schema=LeadSourceRead,
    prefix="/lead-sources",
    tags=["CRM - Leads"],
)

contacts_router = build_crud_router(
    model=Contact,
    pk_field="contact_id",
    pk_type=uuid.UUID,
    create_schema=ContactCreate,
    update_schema=ContactUpdate,
    read_schema=ContactRead,
    prefix="/contacts",
    tags=["CRM - Accounts"],
)

accounts_router = build_crud_router(
    model=Account,
    pk_field="account_id",
    pk_type=uuid.UUID,
    create_schema=AccountCreate,
    update_schema=AccountUpdate,
    read_schema=AccountRead,
    prefix="/accounts",
    tags=["CRM - Accounts"],
)

opportunities_router = build_crud_router(
    model=Opportunity,
    pk_field="opportunity_id",
    pk_type=uuid.UUID,
    create_schema=OpportunityCreate,
    update_schema=OpportunityUpdate,
    read_schema=OpportunityRead,
    prefix="/opportunities",
    tags=["CRM - Accounts"],
)

industries_router = build_crud_router(
    model=Industry,
    pk_field="industry_id",
    pk_type=uuid.UUID,
    create_schema=IndustryCreate,
    update_schema=IndustryUpdate,
    read_schema=IndustryRead,
    prefix="/industries",
    tags=["CRM - Accounts"],
)

all_crm_routers = [
    campaigns_router,
    campaign_members_router,
    leads_router,
    lead_sources_router,
    contacts_router,
    accounts_router,
    opportunities_router,
    industries_router,
]
