from __future__ import annotations

import asyncio
import json
import logging
from collections.abc import AsyncIterator
from contextlib import suppress

from .agent import AgentRunError, SagePrimaryAgent
from .models import AgentEvent, AgentTaskRequest
from .openai_gateway import OpenAIUpstreamError

_DONE = object()
logger = logging.getLogger(__name__)


def encode_sse(event: AgentEvent) -> str:
    payload = json.dumps(event.data, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event.event}\ndata: {payload}\n\n"


async def stream_agent_with_heartbeat(
    agent: SagePrimaryAgent,
    request: AgentTaskRequest,
    *,
    heartbeat_seconds: float = 10.0,
) -> AsyncIterator[str]:
    queue: asyncio.Queue[AgentEvent | object] = asyncio.Queue()

    async def produce() -> None:
        try:
            async for event in agent.run(request):
                await queue.put(event)
        except asyncio.CancelledError:
            raise
        except (AgentRunError, OpenAIUpstreamError, TimeoutError):
            logger.warning("agent request failed", extra={"sage_request_id": request.request_id})
            await queue.put(
                AgentEvent(
                    event="error",
                    data={
                        "request_id": request.request_id,
                        "code": "agent_unavailable",
                        "message": "服务端主 Agent 暂时不可用，请使用本地回退或稍后重试。",
                        "retryable": True,
                    },
                )
            )
        except Exception:
            logger.exception("unexpected agent failure", extra={"sage_request_id": request.request_id})
            await queue.put(
                AgentEvent(
                    event="error",
                    data={
                        "request_id": request.request_id,
                        "code": "agent_unavailable",
                        "message": "服务端主 Agent 暂时不可用，请使用本地回退或稍后重试。",
                        "retryable": True,
                    },
                )
            )
        finally:
            await queue.put(_DONE)

    producer = asyncio.create_task(produce())
    try:
        while True:
            try:
                item = await asyncio.wait_for(queue.get(), timeout=heartbeat_seconds)
            except TimeoutError:
                yield ": heartbeat\n\n"
                continue
            if item is _DONE:
                break
            if isinstance(item, AgentEvent):
                yield encode_sse(item)
    finally:
        producer.cancel()
        with suppress(asyncio.CancelledError):
            await producer
