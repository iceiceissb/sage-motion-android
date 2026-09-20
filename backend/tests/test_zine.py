import base64

import httpx
import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from sage_backend.config import Settings
from sage_backend.main import create_app
from sage_backend.zine import ZineGateway, ZineRequest, ZineResult

PNG = base64.b64encode(b"\x89PNG\r\n\x1a\nfixture").decode()
BODY = {"image_data_url": "data:image/png;base64," + PNG, "caption": "A park"}


def test_zine_input_validates_image_and_size():
    with pytest.raises(ValidationError):
        ZineRequest(image_data_url="data:image/png;base64," + base64.b64encode(b"wrong").decode())
    with pytest.raises(ValidationError):
        ZineRequest(**{**BODY, "caption": "x" * 501})


def test_zine_requires_authentication_and_explicit_server_enablement():
    app = create_app(
        Settings(_env_file=None, SAGE_ENV="test", OPENAI_API_KEY="test", SAGE_BACKEND_AUTH_TOKEN="secret")
    )
    with TestClient(app) as client:
        assert client.post("/v1/journey/zine", json=BODY).status_code == 401
        headers = {"Authorization": "Bearer secret"}
        assert client.post("/v1/journey/zine", headers=headers, json=BODY).status_code == 503
        assert client.get("/v1/capabilities", headers=headers).json() == {"agent": True, "zine": False}


def test_zine_success_and_provider_errors_are_sanitized():
    class FakeGateway:
        fail = False

        async def generate(self, task):
            if self.fail:
                raise ValueError("private user image and API credential")
            return ZineResult(image_base64=PNG)

    app = create_app(Settings(_env_file=None, SAGE_ENV="test", OPENAI_API_KEY="test", SAGE_ZINE_ENABLED=True))
    with TestClient(app) as client:
        gateway = FakeGateway()
        app.state.zine = gateway
        assert client.post("/v1/journey/zine", json=BODY).json()["image_base64"] == PNG
        gateway.fail = True
        response = client.post("/v1/journey/zine", json=BODY)
        assert response.status_code == 502
        assert "private" not in response.text


@pytest.mark.asyncio
async def test_zine_gateway_sends_one_multipart_edit_and_never_retries():
    requests = []

    async def handle(request):
        requests.append(request)
        assert request.url.path == "/v1/images/edits"
        assert "multipart/form-data" in request.headers["content-type"]
        assert b'filename="scene.png"' in await request.aread()
        return httpx.Response(200, json={"data": [{"b64_json": PNG}]})

    async with httpx.AsyncClient(transport=httpx.MockTransport(handle)) as client:
        result = await ZineGateway(client, Settings(_env_file=None)).generate(ZineRequest(**BODY))
    assert result.image_base64 == PNG
    assert len(requests) == 1


@pytest.mark.asyncio
async def test_zine_rejects_non_image_provider_output():
    async with httpx.AsyncClient(
        transport=httpx.MockTransport(
            lambda _: httpx.Response(200, json={"data": [{"b64_json": base64.b64encode(b"bad").decode()}]})
        )
    ) as client:
        with pytest.raises(ValueError):
            await ZineGateway(client, Settings(_env_file=None)).generate(ZineRequest(**BODY))
