from __future__ import annotations

from dataclasses import dataclass

from .models import Scenario


@dataclass(frozen=True, slots=True)
class SkillDefinition:
    """Trusted, server-selected workflow and tool policy for one product scenario."""

    id: str
    scenario: Scenario
    objective: str
    rules: tuple[str, ...]
    allowed_tools: frozenset[str]


SKILLS: dict[Scenario, SkillDefinition] = {
    Scenario.ENVIRONMENT: SkillDefinition(
        id="route_planning",
        scenario=Scenario.ENVIRONMENT,
        objective="规划并比较两条符合用户偏好与当前环境约束的路线画像。",
        rules=(
            "需要天气、体感、降水、UV 或空气质量事实时调用环境工具。",
            "需要路线权衡时调用路线画像工具；路径几何与导航事实仍以 Android 高德为准。",
        ),
        allowed_tools=frozenset({"get_park_environment", "compare_park_route_profiles"}),
    ),
    Scenario.EXPLORE: SkillDefinition(
        id="exploration_hub",
        scenario=Scenario.EXPLORE,
        objective="说明探索工作台已经就绪，并引导用户选择拍照、语音或改道任务。",
        rules=("不制造新的现场事实。",),
        allowed_tools=frozenset(),
    ),
    Scenario.VISUAL: SkillDefinition(
        id="visual_discovery",
        scenario=Scenario.VISUAL,
        objective="优先分析用户明确授权上传的照片，并结合端侧视觉标签生成可核查的视觉发现回答。",
        rules=(
            "附有原图时以原图可见信息为主，端侧标签只作为辅助线索。",
            "没有附图或证据不足时明确无法做细粒度身份确认，不猜测物种或对象身份。",
        ),
        allowed_tools=frozenset(),
    ),
    Scenario.VOICE: SkillDefinition(
        id="voice_companion",
        scenario=Scenario.VOICE,
        objective="结合当前旅程上下文回答用户的公园语音问题。",
        rules=(
            "先判断问题是否确实需要天气、地点或路线信息，只调用与问题相关的工具。",
            "地点结果不代表评分或实时营业状态。",
        ),
        allowed_tools=frozenset(
            {"get_park_environment", "search_nearby_places", "compare_park_route_profiles"}
        ),
    ),
    Scenario.CREATE: SkillDefinition(
        id="journey_composer",
        scenario=Scenario.CREATE,
        objective="根据已记录的路线、照片、问答和行为整理旅程回顾。",
        rules=("不要补造用户没有拍摄、询问或走过的内容。",),
        allowed_tools=frozenset(),
    ),
    Scenario.ADJUST: SkillDefinition(
        id="dynamic_replanning",
        scenario=Scenario.ADJUST,
        objective="依据用户报告的现场变化比较改道路线画像。",
        rules=(
            "封路、施工或拥挤属于用户报告，不能表述为已由服务端核实。",
            "天气事实必须来自环境工具，路径几何与导航事实仍以 Android 高德为准。",
        ),
        allowed_tools=frozenset({"get_park_environment", "compare_park_route_profiles"}),
    ),
}


def skill_for(scenario: Scenario) -> SkillDefinition:
    return SKILLS[scenario]
