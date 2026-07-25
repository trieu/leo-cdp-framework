"""Application configuration, loaded from environment variables / .env."""

from typing import Optional

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    db_host: str = "localhost"
    db_port: int = 5432
    db_user: str = "postgres"
    db_password: str = "password"
    db_name: str = "customer360"
    db_schema: str = "customer360"

    db_pool_size: int = 10
    db_max_overflow: int = 20
    db_pool_recycle_seconds: int = 1800
    db_pool_pre_ping: bool = True
    db_echo_sql: bool = False

    api_default_page_size: int = 100
    api_max_page_size: int = 1000

    # Redis response cache (see core/cache.py). Disconnected/misconfigured
    # Redis never breaks the API -- it just disables caching (fail open).
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: Optional[str] = None
    cache_enabled: bool = True
    cache_ttl_seconds: int = 60

    # SSO/Keycloak settings 
    keycloak_callback_url: str = Field(
        default="",
        validation_alias=AliasChoices(
            "KEYCLOAK_CALLBACK_URL", "keycloak_callback_url", "keycloakCallbackUrl"
        ),
    )
    keycloak_client_id: str = Field(
        default="leocdp",
        validation_alias=AliasChoices(
            "KEYCLOAK_CLIENT_ID", "keycloak_client_id", "keycloakClientId"),
    )
    keycloak_client_secret: str = Field(
        default="",
        validation_alias=AliasChoices(
            "KEYCLOAK_CLIENT_SECRET", "keycloak_client_secret", "keycloakClientSecret"
        ),
    )
    keycloak_realm: str = Field(
        default="master",
        validation_alias=AliasChoices(
            "KEYCLOAK_REALM", "keycloak_realm", "keycloakRealm"),
    )
    keycloak_verify_ssl: bool = Field(
        default=False,
        validation_alias=AliasChoices(
            "KEYCLOAK_VERIFY_SSL", "keycloak_verify_ssl", "keycloakVerifySSL"),
    )
    sso_login: bool = Field(
        default=False,
        validation_alias=AliasChoices("SSO_LOGIN", "sso_login", "ssoLogin"),
    )
    sso_login_url: str = Field(
        default="",
        validation_alias=AliasChoices(
            "SSO_LOGIN_URL", "sso_login_url", "ssoLoginUrl"),
    )

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    @property
    def sso_configs(self) -> dict[str, str]:
        return {
            "keycloakCallbackUrl": self.keycloak_callback_url,
            "keycloakClientId": self.keycloak_client_id,
            "keycloakClientSecret": self.keycloak_client_secret,
            "keycloakRealm": self.keycloak_realm,
            "keycloakVerifySSL": str(self.keycloak_verify_ssl).lower(),
            "ssoLogin": str(self.sso_login).lower(),
            "ssoLoginUrl": self.sso_login_url,
        }


settings = Settings()
