import json

import httpx
import pytest

from sage_backend.config import Settings
from sage_backend.openai_gateway import OpenAIResponsesGateway


@pytest.mark.asyncio
async def test_responses_request_keeps_key_in_header_and_uses_strict_contract() -> None:
    captured: dict[str, object] = {}

    async def handler(request: httpx.Request) -> httpx.Response:
        captured["authorization"] = request.headers.get("authorization")
        captured["client_request_id"] = request.headers.get("x-client-request-id")
        captured["payload"] = json.loads(request.content)
        return httpx.Response(
            200,
            headers={"x-request-id": "req_openai_test"},
            json={
                "id": "resp_test",
                "status": "completed",
                "output": [],
                "usage": {"input_tokens": 12, "output_tokens": 3, "total_tokens": 15},
            },
        )

    settings = Settings(
        _env_file=None,
        SAGE_ENV="test",
        OPENAI_API_KEY="server-only-key",
        SAGE_OPENAI_MODEL="test-model",
    )
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        gateway = OpenAIResponsesGateway(client, settings)
        response = await gateway.create_response(
            request_id="request006",
            scenario="B2",
            input_items=[{"role": "user", "content": "介绍一下这段路线"}],
            tools=[
                {
                    "type": "function",
                    "name": "get_park_environment",
                    "description": "读取实验点环境",
                    "strict": True,
                    "parameters": {
                        "type": "object",
                        "properties": {},
                        "required": [],
                        "additionalProperties": False,
                    },
                }
            ],
            output_schema={
                "type": "object",
                "properties": {"title": {"type": "string"}},
                "required": ["title"],
                "additionalProperties": False,
            },
            instructions="只使用已提供事实",
            round_index=1,
        )

    payload = captured["payload"]
    assert isinstance(payload, dict)
    assert captured["authorization"] == "Bearer server-only-key"
    assert captured["client_request_id"] == "request006-1"
    assert "server-only-key" not in json.dumps(payload)
    assert payload["model"] == "test-model"
    assert payload["store"] is False
    assert payload["parallel_tool_calls"] is True
    assert payload["tools"][0]["strict"] is True
    assert payload["text"]["format"]["strict"] is True
    assert response["_sage_openai_request_id"] == "req_openai_test"
