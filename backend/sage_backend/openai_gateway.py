from __future__ import annotations

import logging
from typing import Any, Protocol

import httpx

from .config import Settings

logger = logging.getLogger(__name__)


class ResponsesGateway(Protocol):
    async def create_response(
        self,
        *,
        request_id: str,
        scenario: str,
        input_items: list[dict[str, Any]],
        tools: list[dict[str, Any]],
        output_schema: dict[str, Any],
        instructions: str,
        round_index: int,
    ) -> dict[str, Any]: ...


class OpenAIUpstreamError(RuntimeError):
    pass


class OpenAIResponsesGateway:
    def __init__(self, http: httpx.AsyncClient, settings: Settings) -> None:
        self.http = http
        self.settings = settings

    async def create_response(
        self,
        *,
        request_id: str,
        scenario: str,
        input_items: list[dict[str, Any]],
        tools: list[dict[str, Any]],
        output_schema: dict[str, Any],
        instructions: str,
        round_index: int,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "model": self.settings.openai_model,
            "instructions": instructions,
            "input": input_items,
            "store": False,
            "max_output_tokens": 1_400,
            "reasoning": {"effort": self.settings.openai_reasoning_effort},
            "metadata": {"sage_request_id": request_id, "scenario": scenario},
            "text": {
                "verbosity": "low",
                "format": {
                    "type": "json_schema",
                    "name": "sage_task_result",
                    "strict": True,
                    "schema": output_schema,
                },
            },
        }
        if tools:
            payload.update({"tools": tools, "tool_choice": "auto", "parallel_tool_calls": True})

        try:
            response = await self.http.post(
                f"{self.settings.openai_base_url.rstrip('/')}/responses",
                json=payload,
                headers={
                    "Authorization": f"Bearer {self.settings.openai_api_key.get_secret_value()}",
                    "Content-Type": "application/json",
                    "X-Client-Request-Id": f"{request_id}-{round_index}",
                },
                timeout=self.settings.openai_timeout_seconds,
            )
        except httpx.HTTPError as error:
            raise OpenAIUpstreamError("OpenAI request failed before a response was received") from error

        upstream_request_id = response.headers.get("x-request-id")
        if response.status_code >= 400:
            logger.warning(
                "OpenAI response failed",
                extra={
                    "sage_request_id": request_id,
                    "openai_request_id": upstream_request_id,
                    "status_code": response.status_code,
                },
            )
            raise OpenAIUpstreamError(f"OpenAI returned HTTP {response.status_code}")
        try:
            data = response.json()
        except ValueError as error:
            raise OpenAIUpstreamError("OpenAI returned invalid JSON") from error
        if not isinstance(data, dict):
            raise OpenAIUpstreamError("OpenAI returned an unexpected payload")
        if data.get("error") or data.get("status") in {"failed", "cancelled", "incomplete"}:
            raise OpenAIUpstreamError("OpenAI did not complete the response")
        data["_sage_openai_request_id"] = upstream_request_id
        usage = data.get("usage") if isinstance(data.get("usage"), dict) else {}
        logger.info(
            "OpenAI response completed",
            extra={
                "sage_request_id": request_id,
                "openai_request_id": upstream_request_id,
                "input_tokens": usage.get("input_tokens"),
                "output_tokens": usage.get("output_tokens"),
                "total_tokens": usage.get("total_tokens"),
            },
        )
        return data
