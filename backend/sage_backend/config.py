from functools import lru_cache
from typing import Literal

from pydantic import AliasChoices, Field, SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    environment: Literal["development", "test", "production"] = Field(
        default="development",
        validation_alias="SAGE_ENV",
    )
    log_level: str = Field(default="INFO", validation_alias="SAGE_LOG_LEVEL")
    openai_api_key: SecretStr = Field(
        default=SecretStr(""),
        validation_alias=AliasChoices("OPENAI_API_KEY", "SAGE_OPENAI_API_KEY"),
    )
    openai_model: str = Field(default="gpt-5.6-terra", validation_alias="SAGE_OPENAI_MODEL")
    openai_base_url: str = Field(
        default="https://api.openai.com/v1",
        validation_alias="SAGE_OPENAI_BASE_URL",
    )
    openai_reasoning_effort: Literal["none", "low", "medium", "high"] = Field(
        default="low",
        validation_alias="SAGE_OPENAI_REASONING_EFFORT",
    )
    openai_timeout_seconds: float = Field(
        default=45.0,
        ge=5.0,
        le=120.0,
        validation_alias="SAGE_OPENAI_TIMEOUT_SECONDS",
    )
    backend_auth_token: SecretStr = Field(
        default=SecretStr(""),
        validation_alias="SAGE_BACKEND_AUTH_TOKEN",
    )
    rate_limit_per_minute: int = Field(
        default=30,
        ge=1,
        le=600,
        validation_alias="SAGE_RATE_LIMIT_PER_MINUTE",
    )
    max_tool_rounds: int = Field(default=4, ge=1, le=8, validation_alias="SAGE_MAX_TOOL_ROUNDS")
    max_tool_calls: int = Field(default=8, ge=1, le=16, validation_alias="SAGE_MAX_TOOL_CALLS")
    zine_enabled: bool = Field(default=False, validation_alias="SAGE_ZINE_ENABLED")
    image_model: str = Field(default="gpt-image-2.5-sunburst", validation_alias="SAGE_IMAGE_MODEL")

    @model_validator(mode="after")
    def validate_production_security(self) -> "Settings":
        if self.environment == "production" and not self.backend_auth_token.get_secret_value():
            raise ValueError("SAGE_BACKEND_AUTH_TOKEN is required in production")
        if not self.openai_base_url.startswith("https://"):
            raise ValueError("SAGE_OPENAI_BASE_URL must use HTTPS")
        return self

    @property
    def has_openai_key(self) -> bool:
        return bool(self.openai_api_key.get_secret_value().strip())


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
