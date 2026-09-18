from __future__ import annotations

import asyncio
import json
import math
import time
from collections.abc import Awaitable, Callable, Collection
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any, Literal
from urllib.parse import urlencode

import httpx
from pydantic import BaseModel, ConfigDict, Field, ValidationError

from .models import AgentTaskRequest, Scenario, ToolExecution, ToolProvenance

PARK_LATITUDE = 40.01546
PARK_LONGITUDE = 116.32724


class ToolArguments(BaseModel):
    model_config = ConfigDict(extra="forbid")


class EnvironmentArguments(ToolArguments):
    pass


class NearbyPlaceArguments(ToolArguments):
    category: Literal["food", "scenic", "restroom"]
    radius_meters: int = Field(ge=300, le=2_500)


class RouteProfileArguments(ToolArguments):
    priorities: list[Literal["shade", "rest", "short", "quiet", "shelter", "scenery"]] = Field(
        min_length=1,
        max_length=6,
    )
    reported_change: str = Field(max_length=300)


@dataclass(frozen=True)
class ToolDefinition:
    name: str
    description: str
    arguments_model: type[ToolArguments]
    scenarios: frozenset[Scenario]
    runner: Callable[[ToolArguments, AgentTaskRequest], Awaitable[tuple[dict[str, Any], ToolProvenance]]]

    def openai_schema(self) -> dict[str, Any]:
        parameters = self.arguments_model.model_json_schema()
        parameters.pop("title", None)
        return {
            "type": "function",
            "name": self.name,
            "description": self.description,
            "strict": True,
            "parameters": parameters,
        }


class ToolRegistry:
    def __init__(self, http: httpx.AsyncClient) -> None:
        self.http = http
        self._cache: dict[str, tuple[float, dict[str, Any], ToolProvenance]] = {}
        self._definitions = {
            definition.name: definition
            for definition in (
                ToolDefinition(
                    name="get_park_environment",
                    description=(
                        "读取东升八家郊野公园南区固定实验点的实时天气和空气质量。"
                        "不读取参与者位置；需要天气、体感、降水、UV 或 AQI 事实时调用。"
                    ),
                    arguments_model=EnvironmentArguments,
                    scenarios=frozenset({Scenario.ENVIRONMENT, Scenario.VOICE, Scenario.ADJUST}),
                    runner=self._get_park_environment,
                ),
                ToolDefinition(
                    name="search_nearby_places",
                    description=(
                        "以公园固定实验点为中心查询实际命名的餐饮、景点或洗手间。"
                        "只在用户明确询问附近地点时调用；结果不包含评分或实时营业状态。"
                    ),
                    arguments_model=NearbyPlaceArguments,
                    scenarios=frozenset({Scenario.VOICE}),
                    runner=self._search_nearby_places,
                ),
                ToolDefinition(
                    name="compare_park_route_profiles",
                    description=(
                        "根据用户偏好和用户报告的变化，对内置园路语义画像进行可复现评分。"
                        "它不生成地图几何，客户端仍以高德 RouteSearchV2 返回的步行路径为准。"
                    ),
                    arguments_model=RouteProfileArguments,
                    scenarios=frozenset({Scenario.ENVIRONMENT, Scenario.VOICE, Scenario.ADJUST}),
                    runner=self._compare_route_profiles,
                ),
            )
        }

    def schemas_for(
        self,
        scenario: Scenario,
        allowed_tool_names: Collection[str] | None = None,
    ) -> list[dict[str, Any]]:
        return [
            definition.openai_schema()
            for definition in self._definitions.values()
            if scenario in definition.scenarios
            and (allowed_tool_names is None or definition.name in allowed_tool_names)
        ]

    async def execute(
        self,
        *,
        call_id: str,
        name: str,
        raw_arguments: str,
        request: AgentTaskRequest,
        allowed_tool_names: Collection[str] | None = None,
    ) -> ToolExecution:
        started_at = time.monotonic()
        definition = self._definitions.get(name)
        if (
            definition is None
            or request.scenario not in definition.scenarios
            or (allowed_tool_names is not None and name not in allowed_tool_names)
        ):
            return self._failed_execution(call_id, name, "tool_not_allowed", started_at)
        try:
            decoded = json.loads(raw_arguments or "{}")
            arguments = definition.arguments_model.model_validate(decoded)
        except (json.JSONDecodeError, ValidationError, TypeError):
            return self._failed_execution(call_id, name, "invalid_arguments", started_at)

        try:
            async with asyncio.timeout(14):
                data, provenance = await definition.runner(arguments, request)
            return ToolExecution(
                call_id=call_id,
                tool_name=name,
                ok=True,
                data=data,
                provenance=provenance,
                latency_ms=_elapsed_ms(started_at),
            )
        except (TimeoutError, httpx.HTTPError, ValueError, KeyError, TypeError):
            return self._failed_execution(call_id, name, "tool_unavailable", started_at)

    def _failed_execution(self, call_id: str, name: str, code: str, started_at: float) -> ToolExecution:
        return ToolExecution(
            call_id=call_id,
            tool_name=name,
            ok=False,
            error_code=code,
            provenance=ToolProvenance(
                tool_name=name,
                source="工具未返回可靠结果",
                fetched_at=_now_iso(),
                cache_status="unavailable",
                is_live_data=False,
            ),
            latency_ms=_elapsed_ms(started_at),
        )

    async def _get_park_environment(
        self,
        _: ToolArguments,
        __: AgentTaskRequest,
    ) -> tuple[dict[str, Any], ToolProvenance]:
        cached = self._cache_get("environment")
        if cached is not None:
            return cached

        common = {"latitude": PARK_LATITUDE, "longitude": PARK_LONGITUDE, "timezone": "Asia/Shanghai"}
        weather_params = {
            **common,
            "current": "temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m",
            "daily": "uv_index_max",
            "forecast_days": 1,
        }
        air_params = {**common, "current": "pm2_5,us_aqi"}
        weather, air = await asyncio.gather(
            self._get_json("https://api.open-meteo.com/v1/forecast", weather_params),
            self._get_json("https://air-quality-api.open-meteo.com/v1/air-quality", air_params),
        )
        current = weather["current"]
        air_current = air.get("current", {})
        uv_values = weather.get("daily", {}).get("uv_index_max", [])
        data = {
            "location_label": "东升八家郊野公园南区固定实验点",
            "temperature_celsius": current["temperature_2m"],
            "apparent_temperature_celsius": current["apparent_temperature"],
            "precipitation_mm": current["precipitation"],
            "wind_speed_kmh": current["wind_speed_10m"],
            "weather_code": current["weather_code"],
            "uv_index_max": uv_values[0] if uv_values else None,
            "us_aqi": air_current.get("us_aqi"),
            "pm25": air_current.get("pm2_5"),
            "observed_at": current.get("time"),
            "privacy_note": "固定实验点，不是参与者实时位置",
        }
        provenance = ToolProvenance(
            tool_name="get_park_environment",
            source="Open-Meteo / CAMS",
            source_url="https://open-meteo.com/",
            fetched_at=_now_iso(),
            cache_status="live",
            is_live_data=True,
        )
        self._cache_put("environment", data, provenance, ttl_seconds=900)
        return data, provenance

    async def _search_nearby_places(
        self,
        raw_arguments: ToolArguments,
        _: AgentTaskRequest,
    ) -> tuple[dict[str, Any], ToolProvenance]:
        arguments = NearbyPlaceArguments.model_validate(raw_arguments.model_dump())
        cache_key = f"places:{arguments.category}:{arguments.radius_meters}"
        cached = self._cache_get(cache_key)
        if cached is not None:
            return cached

        filters = {
            "food": '[amenity~"restaurant|cafe|fast_food"]',
            "restroom": '[amenity="toilets"]',
            "scenic": '[tourism~"attraction|viewpoint"]',
        }
        osm_filter = filters[arguments.category]
        query = (
            "[out:json][timeout:8];("
            f"node(around:{arguments.radius_meters},{PARK_LATITUDE},{PARK_LONGITUDE}){osm_filter};"
            f"way(around:{arguments.radius_meters},{PARK_LATITUDE},{PARK_LONGITUDE}){osm_filter};"
            ");out center tags 30;"
        )
        payload: dict[str, Any] | None = None
        for endpoint in (
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        ):
            try:
                response = await self.http.post(
                    endpoint,
                    content=urlencode({"data": query}),
                    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"},
                )
                response.raise_for_status()
                payload = response.json()
                break
            except (httpx.HTTPError, ValueError):
                continue
        if payload is None:
            raise ValueError("all place providers failed")

        places: list[dict[str, Any]] = []
        for element in payload.get("elements", []):
            tags = element.get("tags", {})
            name = (tags.get("name:zh") or tags.get("name") or "").strip()
            if not name:
                continue
            center = element.get("center", {})
            latitude = element.get("lat", center.get("lat"))
            longitude = element.get("lon", center.get("lon"))
            if latitude is None or longitude is None:
                continue
            places.append(
                {
                    "name": name[:100],
                    "category": tags.get("amenity") or tags.get("tourism") or arguments.category,
                    "distance_meters": _distance_meters(
                        PARK_LATITUDE,
                        PARK_LONGITUDE,
                        float(latitude),
                        float(longitude),
                    ),
                    "address": "".join(
                        part
                        for part in (tags.get("addr:street", ""), tags.get("addr:housenumber", ""))
                        if part
                    )[:160]
                    or None,
                }
            )
        unique = {place["name"]: place for place in sorted(places, key=lambda item: item["distance_meters"])}
        data = {
            "places": list(unique.values())[:5],
            "query_center": "东升八家郊野公园南区固定实验点",
            "radius_meters": arguments.radius_meters,
            "limitations": "OpenStreetMap 不提供可靠评分或实时营业状态",
        }
        provenance = ToolProvenance(
            tool_name="search_nearby_places",
            source="OpenStreetMap Overpass",
            source_url="https://www.openstreetmap.org/",
            fetched_at=_now_iso(),
            cache_status="live",
            is_live_data=True,
        )
        self._cache_put(cache_key, data, provenance, ttl_seconds=1_800)
        return data, provenance

    async def _compare_route_profiles(
        self,
        raw_arguments: ToolArguments,
        request: AgentTaskRequest,
    ) -> tuple[dict[str, Any], ToolProvenance]:
        arguments = RouteProfileArguments.model_validate(raw_arguments.model_dump())
        combined = f"{request.prompt} {arguments.reported_change}".replace(" ", "")
        priorities = set(arguments.priorities)
        if any(word in combined for word in ("雨", "避雨", "雷")):
            priorities.add("shelter")
        if any(word in combined for word in ("座椅", "休息", "累")):
            priorities.add("rest")
        if any(word in combined for word in ("安静", "人多", "拥挤")):
            priorities.add("quiet")
        if any(word in combined for word in ("短", "快", "少走")):
            priorities.add("short")

        profiles = [
            {
                "name": "湖边林荫线",
                "features": {"shade": 4, "rest": 4, "short": 4, "quiet": 3, "shelter": 1, "scenery": 5},
            },
            {
                "name": "草坪外环线",
                "features": {"shade": 1, "rest": 2, "short": 2, "quiet": 4, "shelter": 1, "scenery": 4},
            },
            {
                "name": "林下连廊绕行线",
                "features": {"shade": 5, "rest": 4, "short": 2, "quiet": 4, "shelter": 5, "scenery": 3},
            },
        ]
        ranked = []
        for profile in profiles:
            score = sum(profile["features"][priority] for priority in priorities)
            reported_blockage = any(word in combined for word in ("封路", "施工", "走不通"))
            if request.scenario == Scenario.ADJUST and reported_blockage:
                score += 6 if profile["name"] == "林下连廊绕行线" else 0
            ranked.append({**profile, "score": score})
        ranked.sort(key=lambda item: (-item["score"], item["name"]))
        data = {
            "ranking": ranked[:2],
            "applied_priorities": sorted(priorities),
            "active_route": request.journey_context.active_route_name,
            "geometry_boundary": "只比较语义画像；实际路径几何与导航以 Android 高德 RouteSearchV2 为准",
        }
        return data, ToolProvenance(
            tool_name="compare_park_route_profiles",
            source="SAGE 可复现路线画像评分器",
            source_url=None,
            fetched_at=_now_iso(),
            cache_status="static",
            is_live_data=False,
        )

    async def _get_json(self, url: str, params: dict[str, Any]) -> dict[str, Any]:
        last_error: Exception | None = None
        for attempt in range(2):
            try:
                response = await self.http.get(url, params=params)
                response.raise_for_status()
                payload = response.json()
                if not isinstance(payload, dict):
                    raise ValueError("expected an object response")
                return payload
            except (httpx.HTTPError, ValueError) as error:
                last_error = error
                if attempt == 0:
                    await asyncio.sleep(0.2)
        raise ValueError("external data provider failed") from last_error

    def _cache_get(self, key: str) -> tuple[dict[str, Any], ToolProvenance] | None:
        cached = self._cache.get(key)
        if cached is None:
            return None
        expires_at, data, provenance = cached
        if time.monotonic() >= expires_at:
            self._cache.pop(key, None)
            return None
        return data, provenance.model_copy(update={"cache_status": "fresh_cache"})

    def _cache_put(
        self,
        key: str,
        data: dict[str, Any],
        provenance: ToolProvenance,
        *,
        ttl_seconds: int,
    ) -> None:
        self._cache[key] = (time.monotonic() + ttl_seconds, data, provenance)


def _now_iso() -> str:
    return datetime.now(UTC).isoformat(timespec="seconds")


def _elapsed_ms(started_at: float) -> int:
    return max(0, round((time.monotonic() - started_at) * 1_000))


def _distance_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> int:
    latitude_distance = math.radians(lat2 - lat1)
    longitude_distance = math.radians(lon2 - lon1)
    a = (
        math.sin(latitude_distance / 2) ** 2
        + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(longitude_distance / 2) ** 2
    )
    return int(6_371_000 * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a)))
