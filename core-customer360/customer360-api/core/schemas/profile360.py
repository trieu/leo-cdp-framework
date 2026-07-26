"""Response schemas for the Customer 360 profile dashboard aggregate
endpoints (see core/crud/profile360.py and the ``/master-profiles/{id}/...``
routes in core/routers/identity.py)."""

from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel


class EngagementSummary(BaseModel):
    period_days: int
    total_logins: int
    total_transactions: int
    total_spent: Decimal
    avg_transaction_amount: Optional[Decimal] = None
    currency: str
    last_interaction_at: Optional[datetime] = None


class ChannelActivity(BaseModel):
    period_days: int
    app_sessions: int
    web_sessions: int
    customer_service_contacts: int
    transactions: int


class TopInterest(BaseModel):
    category: str
    label: str
    count: int
    percentage: float


class TimelineEntry(BaseModel):
    kind: str  # event | transaction | contact
    title: str
    subtitle: Optional[str] = None
    channel: Optional[str] = None
    amount: Optional[Decimal] = None
    currency: Optional[str] = None
    occurred_at: Optional[datetime] = None
