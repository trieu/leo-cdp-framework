"""Deterministic, non-PII ``persona_name`` generation for master profiles
whose personal data has been hashed (``cdp_master_profiles.is_hashed = TRUE``).

Business rule (see ``core-customer360/database-schema.sql``):
    Once ``full_name``/``email``/``phone_number``/``national_id`` are
    one-way SHA-256 hashed (see ``scripts/init_sample_data.py``'s
    ``hash_pii()``), the master profile no longer contains any
    human-readable identity signal. Browsing/searching profiles (admin UI,
    semantic search against ``persona_embedding``, etc.) needs *some*
    readable label instead -- so whenever ``is_hashed = TRUE``,
    ``persona_name`` MUST be populated (enforced both by a DB CHECK
    constraint and by this module, which is what actually computes it).

This module never reverses or exposes the original PII: ``persona_name`` is
built only from non-PII attributes (``domain``, acquisition channel) plus a
short stable suffix derived from the profile's own (already-hashed or
otherwise non-reversible) identity value, so the same underlying person
always gets the same persona_name -- idempotent/safe to (re)compute on
every merge.

Generation strategy:
    1. If ``GOOGLE_GENAI_API_KEY`` is configured (see ``.env``), ask Google
       Gemini to craft a short, catchy persona archetype label -- the
       prompt only ever includes non-PII attributes (``domain``,
       acquisition channel), never the raw or hashed PII values themselves.
    2. Otherwise, or if the GenAI call fails/is unavailable for any reason
       (no SDK installed, no network, bad key, quota, ...), fall back to a
       fully offline, deterministic local generator so persona_name is
       *always* produced and the CIR pipeline never depends on network
       access. Every failure is caught -- this function never raises.
"""

import hashlib
import logging
import os
import re
from typing import Any, Dict, Optional

from dotenv import load_dotenv
from pydantic import BaseModel, Field

load_dotenv()

logger = logging.getLogger(__name__)

try:
    from google import genai
    from google.genai import types as genai_types
    _GENAI_SDK_AVAILABLE = True
except ImportError:  # pragma: no cover - exercised only when the optional dep is missing
    _GENAI_SDK_AVAILABLE = False

# SHA-256 hex digests are exactly 64 lowercase hex characters -- how
# scripts/init_sample_data.py's hash_pii() stores PII. Detecting this
# pattern lets us decide "is this profile's PII hashed?" straight from the
# data itself, instead of trusting an upstream flag that may not be set.
_SHA256_HEX_RE = re.compile(r"^[0-9a-f]{64}$")

# PII fields that exist with the same name on both cdp_raw_profiles_stage
# and cdp_master_profiles (see resolver.SCALAR_MERGE_FIELDS).
PII_SCALAR_FIELDS = ("full_name", "email", "phone_number", "national_id")

# Non-PII identity anchors, preferred (in order) as the deterministic seed
# for persona_name generation.
_IDENTITY_ANCHOR_FIELDS = ("device_id", "advertising_id", "external_customer_id", "cookie_id")

_ADJECTIVES = (
    "Loyal", "Curious", "Savvy", "Emerging", "High-Value", "Digital-First",
    "Value-Conscious", "Trendy", "Cautious", "Engaged", "Rising", "Active",
)

_ROLE_BY_DOMAIN = {
    "banking": ("Banking Client", "Digital Banking User", "KYC-Verified Client", "Credit Applicant"),
    "retail": ("Retail Shopper", "Online Shopper", "Loyalty Member", "App Shopper"),
}

GOOGLE_GENAI_API_KEY = os.getenv("GOOGLE_GENAI_API_KEY", None)
GOOGLE_GENAI_MODEL = os.getenv("GOOGLE_GENAI_MODEL", "gemini-3.5-flash")
# Bounds the worst-case latency of a single Gemini call (milliseconds) so a
# slow/unreachable API can never stall the CIR resolution batch -- on
# timeout the call raises and generate_persona_name() falls back offline.
_GENAI_TIMEOUT_MS = int(os.getenv("GOOGLE_GENAI_TIMEOUT_MS", "8000"))


def _has_configured_api_key(api_key: Optional[str]) -> bool:
    """True if ``api_key`` looks like a real key rather than an unfilled
    ``.env`` template placeholder (e.g. ``YOUR_GOOGLE_GENAI_API_KEY``)."""
    return bool(api_key) and not api_key.strip().upper().startswith("YOUR_")


class _PersonaLabel(BaseModel):
    """Structured Gemini output for one persona archetype label."""

    persona_name: str = Field(
        description=(
            "A short (2-5 word), catchy marketing persona archetype label, e.g. "
            "'Savvy Retail Shopper' or 'Digital-First Banking Client'. Must be a "
            "GENERIC archetype only -- it must NEVER contain a real person's name, "
            "email, phone number, or any other personal identifier."
        )
    )


_genai_client = None
_genai_client_initialized = False


def _get_genai_client():
    """Lazily creates and caches a Google GenAI client if the SDK is
    installed and a real (non-placeholder) API key is configured. Returns
    ``None`` -- and never raises -- otherwise, so callers can transparently
    fall back to the offline deterministic generator."""
    global _genai_client, _genai_client_initialized
    if _genai_client_initialized:
        return _genai_client
    _genai_client_initialized = True
    if _GENAI_SDK_AVAILABLE and _has_configured_api_key(GOOGLE_GENAI_API_KEY):
        try:
            _genai_client = genai.Client(
                api_key=GOOGLE_GENAI_API_KEY,
                http_options=genai_types.HttpOptions(timeout=_GENAI_TIMEOUT_MS),
            )
        except Exception:
            logger.warning(
                "Could not initialize Google GenAI client; persona_name will use the "
                "offline deterministic generator instead.",
                exc_info=True,
            )
            _genai_client = None
    return _genai_client


def is_hashed_value(value: Optional[str]) -> bool:
    """True if ``value`` looks like a SHA-256 hex digest (64 hex chars)."""
    return bool(value) and bool(_SHA256_HEX_RE.match(str(value)))


def profile_looks_hashed(profile: Dict[str, Any], fields=PII_SCALAR_FIELDS) -> bool:
    """True if any populated PII field on ``profile`` looks SHA-256 hashed."""
    return any(is_hashed_value(profile.get(field)) for field in fields)


def _stable_seed(profile: Dict[str, Any]) -> str:
    """Returns a stable string to deterministically derive a persona_name
    from, preferring non-PII identity anchors. Falls back to the (already
    one-way-hashed) PII value itself -- still safe/stable since it's a
    digest, never the plaintext."""
    for field in _IDENTITY_ANCHOR_FIELDS:
        value = profile.get(field)
        if value:
            return str(value)
    for field in PII_SCALAR_FIELDS:
        value = profile.get(field)
        if value:
            return str(value)
    return ""


def generate_persona_name(profile: Dict[str, Any]) -> str:
    """Builds a short, human-readable, non-PII persona label for a profile
    whose real identity fields are hashed, e.g.
    ``"Savvy Retail Shopper (TikTok Ads) #4f2a9c"``.

    If ``GOOGLE_GENAI_API_KEY`` is configured, this first tries Google
    Gemini to craft the adjective/role wording (only non-PII attributes --
    ``domain`` and acquisition channel -- are ever sent in the prompt).
    Whenever GenAI is not configured, unavailable, or the call fails for any
    reason, this transparently falls back to a fully offline, deterministic
    local generator -- this function never raises and never blocks the CIR
    pipeline on network availability.

    Deterministic suffix: regardless of which path produced the label, the
    trailing ``#<6-hex>`` suffix is always derived locally from the same
    stable identity anchor, so the same underlying person always gets a
    stable, traceable, non-reversible suffix.
    """
    seed = _stable_seed(profile)
    digest = hashlib.sha256((seed or "unknown").encode("utf-8")).hexdigest()
    suffix = digest[:6]

    if _has_configured_api_key(GOOGLE_GENAI_API_KEY):
        ai_label = _generate_with_genai(profile, suffix)
        if ai_label:
            return ai_label

    return _generate_offline(profile, digest, suffix)


def _generate_offline(profile: Dict[str, Any], digest: str, suffix: str) -> str:
    """Fully offline, deterministic persona_name generator (no network calls)."""
    digest_int = int(digest, 16)

    domain = (profile.get("domain") or "retail").lower()
    roles = _ROLE_BY_DOMAIN.get(domain, _ROLE_BY_DOMAIN["retail"])

    adjective = _ADJECTIVES[digest_int % len(_ADJECTIVES)]
    role = roles[(digest_int // len(_ADJECTIVES)) % len(roles)]

    channel = profile.get("media_source") or profile.get("source_system")
    label = f"{adjective} {role}"
    if channel:
        label += f" ({channel})"
    return f"{label} #{suffix}"


def _generate_with_genai(profile: Dict[str, Any], suffix: str) -> Optional[str]:
    """Asks Google Gemini for a creative persona archetype label, sending
    only non-PII attributes (``domain`` + acquisition channel). Returns
    ``None`` on any failure (missing SDK, network error, bad key, malformed
    response, ...) so the caller falls back to the offline generator --
    this never raises."""
    client = _get_genai_client()
    if client is None:
        return None

    domain = (profile.get("domain") or "retail").lower()
    channel = profile.get("media_source") or profile.get("source_system") or "an unknown channel"
    prompt = (
        "You are labeling an anonymized customer profile for a Customer Data Platform. "
        f"The profile's business domain is '{domain}' and its acquisition channel is "
        f"'{channel}'. All personal data (name/email/phone/national ID) for this profile "
        "has been irreversibly hashed and is NOT available to you. Generate ONE short, "
        "catchy marketing persona archetype label for this profile. Never invent or "
        "include a specific person's name, email, phone number or any other personal "
        "identifier -- only a generic archetype description."
    )
    try:
        response = client.models.generate_content(
            model=GOOGLE_GENAI_MODEL,
            contents=prompt,
            config=genai_types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=_PersonaLabel,
                temperature=0.8,
            ),
        )
        label = (response.parsed.persona_name or "").strip()
        if not label:
            return None
        return f"{label} #{suffix}"
    except Exception:
        logger.warning(
            "Google GenAI persona_name generation failed; falling back to the offline "
            "deterministic generator.",
            exc_info=True,
        )
        return None
