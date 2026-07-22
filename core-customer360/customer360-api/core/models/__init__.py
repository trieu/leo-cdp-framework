"""Import every ORM model so Base.metadata is fully populated (needed for
relationship/FK resolution and any metadata-driven tooling)."""

from core.models.base import Base
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
from core.models.relations import CdpRelation, CustomerContact, Purchase, RelationType

__all__ = [
    "Base",
    "Account",
    "Campaign",
    "CampaignMember",
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
    "CustomerContact",
    "Purchase",
    "RelationType",
]
