from collections.abc import AsyncIterator

from fastapi.testclient import TestClient

from sage_backend.config import Settings
from sage_backend.main import create_app
from sage_backend.models import AgentEvent, AgentTaskRequest


class FakeStreamingAgent:
    async def run(self, request: AgentTaskRequest) -> AsyncIterator[AgentEvent]:
        yield AgentEvent(
            event="stage_changed",
            data={"request_id": request.request_id, "stage": "reasoning"},
        )
        yield AgentEvent(
            event="error",
            data={
                "request_id": request.request_id,
                "code": "test_complete",
                "message": "test stream",
                "retryable": False,
            },
        )


def _task_body() -> dict[str, object]:
    return {
        "request_id": "request005",
        "scenario": "B2",
        "prompt": "附近有什么适合拍照的地方？",
        "vision_findings": [],
        "journey_context": {"active_route_name": "湖边林荫线"},
        "client_capabilities": ["amap_route"],
    }


def test_health_and_readiness_do_not_expose_secrets() -> None:
    settings = Settings(_env_file=None, SAGE_ENV="test")
    app = create_app(settings)

    with TestClient(app) as client:
        assert client.get("/healthz").status_code == 200
        readiness = client.get("/readyz")

    assert readiness.status_code == 503
    assert readiness.json() == {"status": "missing_openai_key"}


def test_stream_endpoint_requires_configured_bearer_token() -> None:
    settings = Settings(
        _env_file=None,
        SAGE_ENV="test",
        OPENAI_API_KEY="test-key",
        SAGE_BACKEND_AUTH_TOKEN="test-token",
    )
    app = create_app(settings)

    with TestClient(app) as client:
        app.state.agent = FakeStreamingAgent()
        unauthorized = client.post("/v1/agent/tasks:stream", json=_task_body())

    assert unauthorized.status_code == 401


def test_stream_endpoint_emits_standard_sse_frames() -> None:
    settings = Settings(
        _env_file=None,
        SAGE_ENV="test",
        OPENAI_API_KEY="test-key",
        SAGE_BACKEND_AUTH_TOKEN="test-token",
    )
    app = create_app(settings)

    with TestClient(app) as client:
        app.state.agent = FakeStreamingAgent()
        response = client.post(
            "/v1/agent/tasks:stream",
            json=_task_body(),
            headers={"Authorization": "Bearer test-token", "Accept": "text/event-stream"},
        )

    assert response.status_code == 200
    assert response.headers["x-sage-request-id"] == "request005"
    assert "event: stage_changed" in response.text
    assert '"stage":"reasoning"' in response.text
