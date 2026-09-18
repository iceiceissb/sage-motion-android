from __future__ import annotations

import logging
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.responses import JSONResponse, StreamingResponse

from . import __version__
from .agent import SagePrimaryAgent
from .config import Settings, get_settings
from .models import AgentTaskRequest
from .observability import configure_logging
from .openai_gateway import OpenAIResponsesGateway
from .security import SlidingWindowRateLimiter, authenticate_request
from .sse import stream_agent_with_heartbeat
from .tooling import ToolRegistry

logger = logging.getLogger(__name__)


def create_app(settings: Settings | None = None) -> FastAPI:
    resolved = settings or get_settings()
    configure_logging(resolved.log_level)
    limiter = SlidingWindowRateLimiter(resolved.rate_limit_per_minute)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        timeout = httpx.Timeout(resolved.openai_timeout_seconds, connect=8.0)
        async with httpx.AsyncClient(
            timeout=timeout,
            follow_redirects=False,
            headers={"User-Agent": f"SageMotionAgent/{__version__}"},
        ) as client:
            tools = ToolRegistry(client)
            gateway = OpenAIResponsesGateway(client, resolved)
            app.state.agent = SagePrimaryAgent(gateway, tools, resolved)
            yield

    application = FastAPI(
        title="SAGE Motion Agent Backend",
        version=__version__,
        docs_url="/docs" if resolved.environment != "production" else None,
        redoc_url=None,
        lifespan=lifespan,
    )

    @application.middleware("http")
    async def reject_oversized_requests(request: Request, call_next):
        content_length = request.headers.get("content-length")
        if content_length:
            try:
                limit = 2 * 1024 * 1024 if request.url.path == "/v1/agent/tasks:stream" else 64 * 1024
                too_large = int(content_length) > limit
            except ValueError:
                return JSONResponse(status_code=400, content={"detail": "invalid content-length"})
            if too_large:
                return JSONResponse(status_code=413, content={"detail": "request body too large"})
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Cache-Control"] = "no-store"
        response.headers["Referrer-Policy"] = "no-referrer"
        return response

    @application.get("/healthz")
    async def health() -> dict[str, str]:
        return {"status": "ok", "service": "sage-agent-backend", "version": __version__}

    @application.get("/readyz")
    async def readiness() -> JSONResponse:
        code = status.HTTP_200_OK if resolved.has_openai_key else status.HTTP_503_SERVICE_UNAVAILABLE
        return JSONResponse(
            status_code=code,
            content={"status": "ready" if resolved.has_openai_key else "missing_openai_key"},
        )

    @application.post("/v1/agent/tasks:stream")
    async def stream_task(task: AgentTaskRequest, request: Request) -> StreamingResponse:
        if not resolved.has_openai_key:
            raise HTTPException(status_code=503, detail="OPENAI_API_KEY is not configured")
        subject = authenticate_request(request, resolved)
        await limiter.check(subject)
        logger.info(
            "agent task accepted",
            extra={"sage_request_id": task.request_id, "scenario": task.scenario},
        )
        return StreamingResponse(
            stream_agent_with_heartbeat(request.app.state.agent, task),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache, no-store",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
                "X-Sage-Request-Id": task.request_id,
            },
        )

    return application


app = create_app()
