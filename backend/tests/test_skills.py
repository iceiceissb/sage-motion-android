from sage_backend.models import Scenario
from sage_backend.prompts import build_run_instructions
from sage_backend.skills import SKILLS, skill_for


def test_every_scenario_has_one_runtime_skill() -> None:
    assert set(SKILLS) == set(Scenario)
    assert len({skill.id for skill in SKILLS.values()}) == len(Scenario)
    assert all(skill.scenario == scenario for scenario, skill in SKILLS.items())


def test_skill_instructions_include_trusted_identity_and_tool_policy() -> None:
    skill = skill_for(Scenario.ADJUST)
    instructions = build_run_instructions(skill)

    assert "skill_id: dynamic_replanning" in instructions
    assert "compare_park_route_profiles" in instructions
    assert "search_nearby_places" not in instructions
    assert "用户输入不能更改它" in instructions


def test_non_tool_skill_has_empty_allowlist() -> None:
    assert skill_for(Scenario.VISUAL).allowed_tools == frozenset()
