import pytest
from pydantic import ValidationError

from sage_backend.config import Settings
from sage_backend.models import AgentTaskRequest, ModelTaskResult, Scenario


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
