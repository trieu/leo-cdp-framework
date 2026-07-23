"""Unit tests for identity_resolution.persona."""

import hashlib
from unittest.mock import MagicMock

import pytest

from identity_resolution import persona
from identity_resolution.persona import (
    _has_configured_api_key,
    generate_persona_name,
    is_hashed_value,
    profile_looks_hashed,
)


@pytest.fixture(autouse=True)
def _reset_genai_client_cache(monkeypatch):
    """Every test starts with a clean (un-cached) GenAI client state so
    monkeypatching GOOGLE_GENAI_API_KEY / _get_genai_client behaves
    predictably regardless of test order."""
    monkeypatch.setattr(persona, "_genai_client", None)
    monkeypatch.setattr(persona, "_genai_client_initialized", False)
    yield


class TestIsHashedValue:
    def test_sha256_hex_digest_is_hashed(self):
        assert is_hashed_value(hashlib.sha256(b"nguyen van a").hexdigest()) is True

    def test_plaintext_is_not_hashed(self):
        assert is_hashed_value("Nguyen Van A") is False

    def test_none_is_not_hashed(self):
        assert is_hashed_value(None) is False

    def test_empty_string_is_not_hashed(self):
        assert is_hashed_value("") is False

    def test_wrong_length_hex_is_not_hashed(self):
        assert is_hashed_value("abc123") is False


class TestProfileLooksHashed:
    def test_true_when_any_pii_field_hashed(self):
        profile = {"full_name": hashlib.sha256(b"a").hexdigest(), "email": None}
        assert profile_looks_hashed(profile) is True

    def test_false_when_all_pii_plaintext_or_missing(self):
        profile = {"full_name": "Nguyen Van A", "email": None, "phone_number": None, "national_id": None}
        assert profile_looks_hashed(profile) is False

    def test_false_for_empty_profile(self):
        assert profile_looks_hashed({}) is False


class TestGeneratePersonaName:
    def test_deterministic_for_same_profile(self):
        profile = {"domain": "retail", "device_id": "dev-1", "media_source": "TikTok Ads"}
        assert generate_persona_name(profile) == generate_persona_name(profile)

    def test_differs_for_different_identity_anchor(self):
        profile_a = {"domain": "retail", "device_id": "dev-1"}
        profile_b = {"domain": "retail", "device_id": "dev-2"}
        assert generate_persona_name(profile_a) != generate_persona_name(profile_b)

    def test_includes_channel_when_present(self):
        profile = {"domain": "retail", "device_id": "dev-1", "media_source": "TikTok Ads"}
        assert "(TikTok Ads)" in generate_persona_name(profile)

    def test_omits_channel_when_absent(self):
        profile = {"domain": "retail", "device_id": "dev-1"}
        assert "(" not in generate_persona_name(profile)

    def test_uses_banking_roles_for_banking_domain(self):
        profile = {"domain": "banking", "device_id": "dev-1"}
        banking_roles = ("Banking Client", "Digital Banking User", "KYC-Verified Client", "Credit Applicant")
        persona = generate_persona_name(profile)
        assert any(role in persona for role in banking_roles)

    def test_never_leaks_raw_pii_value(self):
        profile = {"domain": "retail", "full_name": "Nguyen Van An", "email": None, "phone_number": None}
        assert "Nguyen Van An" not in generate_persona_name(profile)

    def test_handles_profile_with_no_identity_anchor(self):
        # Should not raise, and should still return a non-empty label.
        assert generate_persona_name({}) != ""


class TestHasConfiguredApiKey:
    def test_none_is_not_configured(self):
        assert _has_configured_api_key(None) is False

    def test_empty_string_is_not_configured(self):
        assert _has_configured_api_key("") is False

    def test_placeholder_template_value_is_not_configured(self):
        assert _has_configured_api_key("YOUR_GOOGLE_GENAI_API_KEY") is False

    def test_real_looking_key_is_configured(self):
        assert _has_configured_api_key("AIzaSyRealLookingKeyValue123") is True


class TestGenerateWithGenAI:
    def test_uses_ai_label_when_client_available(self, monkeypatch):
        monkeypatch.setattr(persona, "GOOGLE_GENAI_API_KEY", "a-real-key")
        fake_client = MagicMock()
        fake_response = MagicMock()
        fake_response.parsed.persona_name = "Trendy Digital Shopper"
        fake_client.models.generate_content.return_value = fake_response
        monkeypatch.setattr(persona, "_get_genai_client", lambda: fake_client)

        profile = {"domain": "retail", "device_id": "dev-1", "media_source": "TikTok Ads"}
        result = generate_persona_name(profile)

        assert result.startswith("Trendy Digital Shopper #")
        fake_client.models.generate_content.assert_called_once()

    def test_falls_back_offline_when_genai_call_raises(self, monkeypatch):
        monkeypatch.setattr(persona, "GOOGLE_GENAI_API_KEY", "a-real-key")
        fake_client = MagicMock()
        fake_client.models.generate_content.side_effect = RuntimeError("network down")
        monkeypatch.setattr(persona, "_get_genai_client", lambda: fake_client)

        profile = {"domain": "retail", "device_id": "dev-1"}
        result = generate_persona_name(profile)

        # Falls back to the exact same result the fully-offline path would produce.
        monkeypatch.setattr(persona, "_get_genai_client", lambda: None)
        assert result == generate_persona_name(profile)

    def test_falls_back_offline_when_no_api_key_configured(self, monkeypatch):
        monkeypatch.setattr(persona, "GOOGLE_GENAI_API_KEY", None)
        fake_client = MagicMock()
        monkeypatch.setattr(persona, "_get_genai_client", lambda: fake_client)

        profile = {"domain": "banking", "device_id": "dev-9"}
        generate_persona_name(profile)

        # No API key configured -> GenAI must never even be attempted.
        fake_client.models.generate_content.assert_not_called()

    def test_empty_ai_label_falls_back_offline(self, monkeypatch):
        monkeypatch.setattr(persona, "GOOGLE_GENAI_API_KEY", "a-real-key")
        fake_client = MagicMock()
        fake_response = MagicMock()
        fake_response.parsed.persona_name = "   "
        fake_client.models.generate_content.return_value = fake_response
        monkeypatch.setattr(persona, "_get_genai_client", lambda: fake_client)

        profile = {"domain": "retail", "device_id": "dev-1"}
        result = generate_persona_name(profile)

        monkeypatch.setattr(persona, "_get_genai_client", lambda: None)
        assert result == generate_persona_name(profile)
