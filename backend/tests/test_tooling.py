import httpx
import pytest

from sage_backend.models import AgentTaskRequest, Scenario
from sage_backend.tooling import ToolRegistry


@pytest.mark.asyncio
async def test_scenario_tool_allowlist_is_minimal() -> None:
    async with httpx.AsyncClient() as client:
        registry = ToolRegistry(client)

        assert registry.schemas_for(Scenario.VISUAL) == []
        assert {tool["name"] for tool in registry.schemas_for(Scenario.ENVIRONMENT)} == {
            "get_park_environment",
            "compare_park_route_profiles",
        }
        assert "search_nearby_places" in {tool["name"] for tool in registry.schemas_for(Scenario.VOICE)}


@pytest.mark.asyncio
async def test_route_tool_is_deterministic_and_preserves_amap_boundary() -> None:
    request = AgentTaskRequest(
        request_id="request002",
        scenario=Scenario.ADJUST,
        prompt="前方封路而且快下雨了",
    )
    async with httpx.AsyncClient() as client:
        registry = ToolRegistry(client)
        result = await registry.execute(
            call_id="call_route",
            name="compare_park_route_profiles",
            raw_arguments='{"priorities":["shelter","shade"],"reported_change":"封路"}',
            request=request,
        )

    assert result.ok
    assert result.data["ranking"][0]["name"] == "林下连廊绕行线"
    assert "高德 RouteSearchV2" in result.data["geometry_boundary"]
    assert not result.provenance.is_live_data


@pytest.mark.asyncio
async def test_disallowed_tool_is_rejected_without_execution() -> None:
    request = AgentTaskRequest(request_id="request003", scenario=Scenario.VISUAL, prompt="看看照片")
    async with httpx.AsyncClient() as client:
        registry = ToolRegistry(client)
        result = await registry.execute(
            call_id="call_weather",
            name="get_park_environment",
            raw_arguments="{}",
            request=request,
        )

    assert not result.ok
    assert result.error_code == "tool_not_allowed"


@pytest.mark.asyncio
async def test_skill_allowlist_is_enforced_during_execution() -> None:
    request = AgentTaskRequest(request_id="request007", scenario=Scenario.VOICE, prompt="天气如何")
    async with httpx.AsyncClient() as client:
        registry = ToolRegistry(client)
        schemas = registry.schemas_for(Scenario.VOICE, frozenset({"get_park_environment"}))
        result = await registry.execute(
            call_id="call_place",
            name="search_nearby_places",
            raw_arguments='{"category":"food","radius_meters":500}',
            request=request,
            allowed_tool_names=frozenset({"get_park_environment"}),
        )

    assert {schema["name"] for schema in schemas} == {"get_park_environment"}
    assert not result.ok
    assert result.error_code == "tool_not_allowed"


@pytest.mark.asyncio
async def test_place_failover_filters_radius_and_caches_provider():
    calls = []

    def handle(request):
        calls.append(request)
        if request.method == "POST":
            return httpx.Response(503)
        return httpx.Response(
            200,
            json=[
                {"lat": "40.0155", "lon": "116.3272", "name": "Nearby", "type": "restaurant"},
                {"lat": "41.0", "lon": "117.0", "name": "Too far", "type": "restaurant"},
            ],
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handle)) as client:
        registry = ToolRegistry(client)
        request = AgentTaskRequest(scenario=Scenario.VOICE, prompt="food nearby")

        async def query():
            return await registry.execute(
                call_id="places",
                name="search_nearby_places",
                raw_arguments='{"category":"food","radius_meters":500}',
                request=request,
            )

        first = await query()
        second = await query()
    assert first.ok
    assert [p["name"] for p in first.data["places"]] == ["Nearby"]
    assert first.provenance.source == "OpenStreetMap Nominatim"
    assert second.provenance.cache_status == "fresh_cache"
    assert len(calls) == 3


@pytest.mark.asyncio
async def test_all_place_providers_failing_returns_no_fabricated_places():
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: httpx.Response(503))) as client:
        result = await ToolRegistry(client).execute(
            call_id="places",
            name="search_nearby_places",
            raw_arguments='{"category":"food","radius_meters":500}',
            request=AgentTaskRequest(scenario=Scenario.VOICE, prompt="food nearby"),
        )
    assert not result.ok
    assert result.data == {}
    assert not result.provenance.is_live_data
