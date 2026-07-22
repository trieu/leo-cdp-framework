"""Customer Identity Resolution (CIR) package.

Python OOP re-implementation of the PostgreSQL stored procedure
``resolve_customer_identities_dynamic`` described in
``core-customer360/identity-resolution.md``.
"""

from .models import IdentityRule
from .resolver import CustomerIdentityResolver
from .trigger_controller import IdentityResolutionTrigger

__all__ = [
    "IdentityRule",
    "CustomerIdentityResolver",
    "IdentityResolutionTrigger",
]
