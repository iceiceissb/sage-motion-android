from __future__ import annotations

import base64
import binascii
from enum import StrEnum
from typing import Any, Literal
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

MAX_VISION_IMAGE_BYTES = 1_200_000
MAX_VISION_IMAGE_DATA_URL_CHARS = 1_600_100
VISION_IMAGE_PREFIXES = {
    "data:image/jpeg;base64,": b"\xff\xd8\xff",
    "data:image/png;base64,": b"\x89PNG\r\n\x1a\n",
    "data:image/webp;base64,": b"RIFF",
}


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Scenario(StrEnum):
    ENVIRONMENT = "A"
    EXPLORE = "B"
    VISUAL = "B1"
    VOICE = "B2"
    CREATE = "C"
    ADJUST = "D"


class VisionFinding(StrictModel):
    label: str = Field(min_length=1, max_length=80)
    confidence: float = Field(ge=0.0, le=1.0)


class JourneyContext(StrictModel):
    active_route_name: str = Field(default="湖边林荫线", max_length=80)
    route_replanned: bool = False
    previous_voice_turns: list[str] = Field(default_factory=list, max_length=12)
    vision_labels: list[str] = Field(default_factory=list, max_length=24)
    photo_question_count: int = Field(default=0, ge=0, le=100)
    visual_interaction_count: int = Field(default=0, ge=0, le=100)
    voice_interaction_count: int = Field(default=0, ge=0, le=100)
    replan_count: int = Field(default=0, ge=0, le=100)

    @field_validator("previous_voice_turns", "vision_labels")
    @classmethod
    def bound_context_strings(cls, values: list[str]) -> list[str]:
        return [value.strip()[:200] for value in values if value.strip()]


class AgentTaskRequest(StrictModel):
    request_id: str = Field(default_factory=lambda: uuid4().hex, pattern=r"^[A-Za-z0-9_-]{8,64}$")
    scenario: Scenario
    prompt: str = Field(min_length=1, max_length=2_000)
    vision_findings: list[VisionFinding] = Field(default_factory=list, max_length=20)
    vision_image_data_url: str | None = Field(
        default=None,
        max_length=MAX_VISION_IMAGE_DATA_URL_CHARS,
    )
    journey_context: JourneyContext = Field(default_factory=JourneyContext)
    client_capabilities: list[str] = Field(default_factory=list, max_length=16)

    @field_validator("prompt")
    @classmethod
    def normalize_prompt(cls, value: str) -> str:
        return " ".join(value.strip().split())

    @field_validator("vision_image_data_url")
    @classmethod
    def validate_vision_image_data_url(cls, value: str | None) -> str | None:
        if value is None:
            return None
        prefix = next((candidate for candidate in VISION_IMAGE_PREFIXES if value.startswith(candidate)), None)
        if prefix is None:
            raise ValueError("vision image must be a JPEG, PNG, or WebP base64 data URL")
        encoded = value[len(prefix) :]
        try:
            decoded = base64.b64decode(encoded, validate=True)
        except (binascii.Error, ValueError) as error:
            raise ValueError("vision image contains invalid base64") from error
        if not decoded or len(decoded) > MAX_VISION_IMAGE_BYTES:
            raise ValueError("vision image exceeds the decoded size limit")
        signature = VISION_IMAGE_PREFIXES[prefix]
        if not decoded.startswith(signature):
            raise ValueError("vision image bytes do not match the declared media type")
        if prefix == "data:image/webp;base64," and decoded[8:12] != b"WEBP":
            raise ValueError("vision image bytes do not match the declared media type")
        return value

    @model_validator(mode="after")
    def restrict_vision_image_to_visual_scenario(self) -> AgentTaskRequest:
        if self.vision_image_data_url is not None and self.scenario != Scenario.VISUAL:
            raise ValueError("vision image is only accepted for the visual scenario")
        return self


class ResultMetric(StrictModel):
    title: str = Field(min_length=1, max_length=40)
    detail: str = Field(min_length=1, max_length=80)


class ModelTaskResult(StrictModel):
    """Fields the model may author. Provenance is added by trusted server code."""

    title: str = Field(min_length=1, max_length=100)
    summary: str = Field(min_length=1, max_length=800)
    uncertainty: str | None = Field(max_length=400)
    alternative_title: str | None = Field(max_length=100)
    metrics: list[ResultMetric] = Field(max_length=6)
    evidence: list[str] = Field(max_length=10)

    @field_validator("evidence")
    @classmethod
    def bound_evidence(cls, values: list[str]) -> list[str]:
        return [value.strip()[:240] for value in values if value.strip()]


class AgentTaskResult(ModelTaskResult):
    primary_action: str = Field(min_length=1, max_length=80)
    source_label: str = Field(min_length=1, max_length=160)
    source_url: str | None = Field(default=None, max_length=500)
    is_live_data: bool = False
    attribution: str | None = Field(default=None, max_length=500)


class ToolProvenance(StrictModel):
    tool_name: str
    source: str
    source_url: str | None = None
    fetched_at: str
    cache_status: Literal["live", "fresh_cache", "static", "unavailable"]
    is_live_data: bool


class ToolExecution(StrictModel):
    call_id: str
    tool_name: str
    ok: bool
    data: dict[str, Any] = Field(default_factory=dict)
    provenance: ToolProvenance
    error_code: str | None = None
    latency_ms: int = Field(default=0, ge=0)


class AgentEvent(StrictModel):
    event: Literal["stage_changed", "tool_started", "tool_completed", "completed", "error"]
    data: dict[str, Any]
