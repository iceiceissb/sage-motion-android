import base64

import pytest
from pydantic import ValidationError

from sage_backend.config import Settings
from sage_backend.models import AgentTaskRequest, ModelTaskResult, Scenario


def _jpeg_data_url(payload: bytes = b"\xff\xd8\xff\xdbtest") -> str:
    return "data:image/jpeg;base64," + base64.b64encode(payload).decode("ascii")


def test_request_normalizes_prompt_and_defaults_context() -> None:
    request = AgentTaskRequest(
        request_id="request001",
        scenario=Scenario.VOICE,
        prompt="  附近   有什么吃的  ",
    )

    assert request.prompt == "附近 有什么吃的"
    assert request.journey_context.active_route_name == "湖边林荫线"


def test_request_rejects_oversized_prompt() -> None:
    with pytest.raises(ValidationError):
        AgentTaskRequest(request_id="request001", scenario=Scenario.VOICE, prompt="问" * 2_001)


def test_visual_request_accepts_valid_bounded_image_data_url() -> None:
    request = AgentTaskRequest(
        request_id="request002",
        scenario=Scenario.VISUAL,
        prompt="请看看这张照片",
        vision_image_data_url=_jpeg_data_url(),
    )

    assert request.vision_image_data_url == _jpeg_data_url()


def test_request_rejects_image_for_non_visual_scenario() -> None:
    with pytest.raises(ValidationError):
        AgentTaskRequest(
            request_id="request003",
            scenario=Scenario.VOICE,
            prompt="这是什么",
            vision_image_data_url=_jpeg_data_url(),
        )


def test_request_rejects_malformed_or_mismatched_image_data() -> None:
    with pytest.raises(ValidationError):
        AgentTaskRequest(
            request_id="request004",
            scenario=Scenario.VISUAL,
            prompt="这是什么",
            vision_image_data_url="data:image/jpeg;base64,bm90LWEtanBlZw==",
        )


def test_model_output_schema_forbids_extra_fields() -> None:
    with pytest.raises(ValidationError):
        ModelTaskResult.model_validate(
            {
                "title": "结果",
                "summary": "摘要",
                "uncertainty": None,
                "alternative_title": None,
                "metrics": [],
                "evidence": [],
                "secret": "must not pass",
            }
        )


def test_production_requires_backend_auth_token() -> None:
    with pytest.raises(ValidationError):
        Settings(
            _env_file=None,
            SAGE_ENV="production",
            OPENAI_API_KEY="server-only-key",
        )


def test_default_primary_model_is_balanced_terra(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("SAGE_OPENAI_MODEL", raising=False)

    settings = Settings(_env_file=None, SAGE_ENV="test")

    assert settings.openai_model == "gpt-5.6-terra"
