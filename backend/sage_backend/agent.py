from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any

from pydantic import ValidationError

from .config import Settings
from .models import AgentEvent, AgentTaskRequest, AgentTaskResult, ModelTaskResult, Scenario, ToolExecution
from .openai_gateway import ResponsesGateway
from .prompts import SYSTEM_INSTRUCTIONS, build_user_input
from .tooling import ToolRegistry


class AgentRunError(RuntimeError):
    pass


PRIMARY_ACTION = {
    Scenario.ENVIRONMENT: "开始路线 · 下一步",
    Scenario.EXPLORE: "返回探索",
    Scenario.VISUAL: "保存结果 · 下一步",
    Scenario.VOICE: "完成体验",
    Scenario.CREATE: "保存回顾 · 下一步",
    Scenario.ADJUST: "采用新路线 · 完成体验",
}

TOOL_STAGE = {
    "get_park_environment": "locating",
    "search_nearby_places": "locating",
    "compare_park_route_profiles": "replanning",
}


class SagePrimaryAgent:
    def __init__(
        self,
        gateway: ResponsesGateway,
        tools: ToolRegistry,
        settings: Settings,
    ) -> None:
        self.gateway = gateway
        self.tools = tools
        self.settings = settings

    async def run(self, request: AgentTaskRequest) -> AsyncIterator[AgentEvent]:
        yield _event("stage_changed", request, stage="activating")
        schemas = self.tools.schemas_for(request.scenario)
        input_items: list[dict[str, Any]] = [
            {
                "role": "user",
                "content": [{"type": "input_text", "text": build_user_input(request)}],
            }
        ]
        executions: list[ToolExecution] = []
        total_tool_calls = 0
        last_openai_request_id: str | None = None

        for round_index in range(self.settings.max_tool_rounds + 1):
            yield _event("stage_changed", request, stage="reasoning")
            response = await self.gateway.create_response(
                request_id=request.request_id,
                scenario=request.scenario.value,
                input_items=input_items,
                tools=schemas,
                output_schema=ModelTaskResult.model_json_schema(),
                instructions=SYSTEM_INSTRUCTIONS,
                round_index=round_index,
            )
            last_openai_request_id = response.get("_sage_openai_request_id")
            output_items = response.get("output")
            if not isinstance(output_items, list):
                raise AgentRunError("model response did not contain output items")
            input_items.extend(item for item in output_items if isinstance(item, dict))
            calls = [
                item
                for item in output_items
                if isinstance(item, dict) and item.get("type") == "function_call"
            ]

            if not calls:
                model_result = _parse_model_result(response)
                final_result = _finalize_result(
                    model_result,
                    request,
                    executions,
                    self.settings.openai_model,
                    last_openai_request_id,
                )
                yield _event("stage_changed", request, stage="responding")
                if final_result.uncertainty:
                    yield _event("stage_changed", request, stage="uncertain")
                yield _event("stage_changed", request, stage="complete")
                yield AgentEvent(
                    event="completed",
                    data={"request_id": request.request_id, "result": final_result.model_dump(mode="json")},
                )
                return

            if round_index >= self.settings.max_tool_rounds:
                raise AgentRunError("model exceeded the tool-round limit")
            if total_tool_calls + len(calls) > self.settings.max_tool_calls:
                raise AgentRunError("model exceeded the tool-call limit")
            total_tool_calls += len(calls)

            for call in calls:
                name = str(call.get("name", ""))
                yield _event("stage_changed", request, stage=TOOL_STAGE.get(name, "reasoning"))
                yield AgentEvent(
                    event="tool_started",
                    data={
                        "request_id": request.request_id,
                        "call_id": str(call.get("call_id", "")),
                        "tool_name": name,
                    },
                )

            round_results = await asyncio.gather(
                *(
                    self.tools.execute(
                        call_id=str(call.get("call_id", "")),
                        name=str(call.get("name", "")),
                        raw_arguments=str(call.get("arguments", "{}")),
                        request=request,
                    )
                    for call in calls
                )
            )
            executions.extend(round_results)
            for result in round_results:
                input_items.append(
                    {
                        "type": "function_call_output",
                        "call_id": result.call_id,
                        "output": result.model_dump_json(),
                    }
                )
                yield AgentEvent(
                    event="tool_completed",
                    data={
                        "request_id": request.request_id,
                        "call_id": result.call_id,
                        "tool_name": result.tool_name,
                        "ok": result.ok,
                        "source": result.provenance.source,
                        "cache_status": result.provenance.cache_status,
                        "latency_ms": result.latency_ms,
                    },
                )

        raise AgentRunError("agent stopped without a final result")


def _parse_model_result(response: dict[str, Any]) -> ModelTaskResult:
    text_parts: list[str] = []
    for item in response.get("output", []):
        if not isinstance(item, dict) or item.get("type") != "message":
            continue
        for content in item.get("content", []):
            if isinstance(content, dict) and content.get("type") == "output_text":
                text_parts.append(str(content.get("text", "")))
    if not text_parts:
        raise AgentRunError("model returned neither tool calls nor structured output")
    try:
        return ModelTaskResult.model_validate_json("".join(text_parts))
    except (ValidationError, json.JSONDecodeError) as error:
        raise AgentRunError("model returned invalid structured output") from error


def _finalize_result(
    model_result: ModelTaskResult,
    request: AgentTaskRequest,
    executions: list[ToolExecution],
    model: str,
    openai_request_id: str | None,
) -> AgentTaskResult:
    provenance_evidence = []
    attributions = []
    for execution in executions:
        provenance = execution.provenance
        status = "成功" if execution.ok else "未返回"
        provenance_evidence.append(
            f"工具：{execution.tool_name} · {status} · {provenance.source} · {provenance.cache_status}"
        )
        if execution.ok and provenance.source not in attributions:
            attributions.append(provenance.source)
    if openai_request_id:
        provenance_evidence.append(f"模型请求追踪：{openai_request_id}")

    source_label = f"服务端主 Agent · OpenAI {model}"
    successful_tools = [execution.tool_name for execution in executions if execution.ok]
    if successful_tools:
        source_label += f" · {len(successful_tools)} 个工具"
    return AgentTaskResult(
        **model_result.model_dump(exclude={"evidence"}),
        primary_action=PRIMARY_ACTION[request.scenario],
        evidence=(provenance_evidence + model_result.evidence)[:12],
        source_label=source_label,
        source_url=None,
        is_live_data=any(execution.ok and execution.provenance.is_live_data for execution in executions),
        attribution="；".join(attributions) or "OpenAI Responses API",
    )


def _event(name: str, request: AgentTaskRequest, **data: Any) -> AgentEvent:
    return AgentEvent(event=name, data={"request_id": request.request_id, **data})
