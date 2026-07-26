"""Import every ORM model so Base.metadata is fully populated (needed for
relationship/FK resolution and any metadata-driven tooling)."""

from core.models.base import Base
from core.models.content import CdpContentItem
from core.models.crm import (
    Account,
    Campaign,
    CampaignMember,
    Contact,
    Industry,
    Lead,
    LeadSource,
    Opportunity,
)
from core.models.graph import GraphEdge
from core.models.identity import (
    CdpIdResolutionStatus,
    CdpMasterProfile,
    CdpProfileAttribute,
    CdpProfileLink,
    CdpRawProfileStage,
)
from core.models.relations import CdpRelation, CustomerContact, RelationType, Transaction
from core.models.segmentation import CdpSegment
from core.models.system import sys_tenant_table, sys_user_table

__all__ = [
    "Base",
    "Account",
    "Campaign",
    "CampaignMember",
    "CdpContentItem",
    "Contact",
    "Industry",
    "Lead",
    "LeadSource",
    "Opportunity",
    "GraphEdge",
    "CdpIdResolutionStatus",
    "CdpMasterProfile",
    "CdpProfileAttribute",
    "CdpProfileLink",
    "CdpRawProfileStage",
    "CdpRelation",
    "CdpSegment",
    "CustomerContact",
    "Transaction",
    "RelationType",
    "sys_tenant_table",
    "sys_user_table",
]
