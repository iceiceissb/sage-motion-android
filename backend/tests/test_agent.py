import json
from typing import Any

import httpx
import pytest

from sage_backend.agent import SagePrimaryAgent
from sage_backend.config import Settings
from sage_backend.models import AgentTaskRequest, Scenario
from sage_backend.tooling import ToolRegistry


class FakeGateway:
    def __init__(self) -> None:
        self.calls: list[dict[str, Any]] = []

    async def create_response(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(kwargs)
        if len(self.calls) == 1:
            return {
                "output": [
                    {
                        "type": "function_call",
                        "call_id": "call_route",
                        "name": "compare_park_route_profiles",
                        "arguments": json.dumps(
                            {"priorities": ["shelter", "shade"], "reported_change": "封路"},
                            ensure_ascii=False,
                        ),
                    }
                ],
                "_sage_openai_request_id": "req_first",
            }
        result = {
            "title": "路线已调整：林下连廊绕行线",
            "summary": "根据用户报告的封路情况，优先选择有遮挡的绕行画像。",
            "uncertainty": "实际通行状态请以现场标识和高德路线为准",
            "alternative_title": "保留当前路线",
            "metrics": [{"title": "优先避雨", "detail": "语义路线画像"}],
            "evidence": ["用户报告前方封路"],
        }
        return {
            "output": [
                {
                    "type": "message",
                    "content": [{"type": "output_text", "text": json.dumps(result, ensure_ascii=False)}],
                }
            ],
            "_sage_openai_request_id": "req_second",
        }


@pytest.mark.asyncio
async def test_agent_executes_tool_loop_and_adds_trusted_provenance() -> None:
    settings = Settings(_env_file=None, SAGE_ENV="test", OPENAI_API_KEY="test-key")
    gateway = FakeGateway()
    async with httpx.AsyncClient() as client:
        agent = SagePrimaryAgent(gateway, ToolRegistry(client), settings)
        request = AgentTaskRequest(
            request_id="request004",
            scenario=Scenario.ADJUST,
            prompt="前方封路，而且快下雨了",
        )
        events = [event async for event in agent.run(request)]

    assert len(gateway.calls) == 2
    assert any(event.event == "tool_started" for event in events)
    completed = next(event for event in events if event.event == "completed")
    result = completed.data["result"]
    assert result["primary_action"] == "采用新路线 · 完成体验"
    assert result["source_label"].startswith("服务端主 Agent")
    assert any("compare_park_route_profiles" in item for item in result["evidence"])
    assert not result["is_live_data"]
    second_input = gateway.calls[1]["input_items"]
    assert any(item.get("type") == "function_call_output" for item in second_input)
