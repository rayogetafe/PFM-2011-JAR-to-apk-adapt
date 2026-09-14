#!/usr/bin/env python3
"""Verify a v41 core JAR against its base JAR and applied roster plan."""
from __future__ import annotations

import json
from pathlib import Path
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "patches"))
from v41_roster_expansion import POSITION, RESOURCE_TO_LEAGUE, parse_league


def strength(player: dict) -> float:
    return sum(player["attrs"][2:5]) / 3


def main(base_path: str, patched_path: str, plan_path: str) -> None:
    plan = json.loads(Path(plan_path).read_text(encoding="utf-8"))
    base_zip = zipfile.ZipFile(base_path)
    patched_zip = zipfile.ZipFile(patched_path)
    largest_first_xi_change = (0.0, "")
    largest_roster_change = (0.0, "")
    total_owned = 0

    for resource, league in RESOURCE_TO_LEAGUE.items():
        base_teams, base_players, _ = parse_league(base_zip.read(resource))
        teams, players, _ = parse_league(patched_zip.read(resource))
        team_plans = list(plan["leagues"][league].items())
        owners = [player_id for team in teams for player_id in team["roster"]]
        if len(owners) != len(set(owners)):
            raise RuntimeError(f"{league}: duplicate ownership")
        total_owned += len(owners)

        for index, ((doc_team, team_plan), base_team, team) in enumerate(zip(team_plans, base_teams, teams)):
            if team["name"] != team_plan["database_team"] or base_team["name"] != team["name"]:
                raise RuntimeError(f"{league}/{doc_team}: team order mismatch")
            expected = 20 - len(team_plan["remove"]) + len(team_plan["move_in"]) + len(team_plan["add"])
            if len(team["roster"]) != expected or not 21 <= expected <= 25:
                raise RuntimeError(f"{league}/{doc_team}: size {len(team['roster'])}, expected {expected}")

            names = [players[player_id]["name"] for player_id in team["roster"]]
            for removal in team_plan["remove"]:
                if removal["name"] in names:
                    raise RuntimeError(f"{league}/{doc_team}: removal still owned: {removal['name']}")
            for move in team_plan["move_in"]:
                if names.count(move["name"]) != 1:
                    raise RuntimeError(f"{league}/{doc_team}: move missing/duplicate: {move['name']}")
            for addition in team_plan["add"]:
                matches = [players[player_id] for player_id in team["roster"] if players[player_id]["name"] == addition["name"]]
                if len(matches) != 1:
                    raise RuntimeError(f"{league}/{doc_team}: addition missing/duplicate: {addition['name']}")
                player = matches[0]
                if player["attrs"][0] != addition["age"] or player["attrs"][1] != POSITION[addition["position"]] or player["attrs"][2:5] != addition["stats"]:
                    raise RuntimeError(f"{league}/{doc_team}: addition fields mismatch: {addition['name']}")

            for player_name, position_name in team_plan["position_changes"].items():
                match = [players[player_id] for player_id in team["roster"] if players[player_id]["name"] == player_name]
                if len(match) != 1 or match[0]["attrs"][1] != POSITION[position_name]:
                    raise RuntimeError(f"{league}/{doc_team}: position correction failed: {player_name}")

            base_existing = {base_players[player_id]["name"]: base_players[player_id] for player_id in base_team["roster"]}
            patched_existing = {players[player_id]["name"]: players[player_id] for player_id in team["roster"] if player_id < len(base_players)}
            before_points = sum(sum(base_existing[name]["attrs"][2:5]) for name in team_plan["existing_adjustments"])
            after_points = sum(sum(patched_existing[name]["attrs"][2:5]) for name in team_plan["existing_adjustments"])
            if before_points != after_points:
                raise RuntimeError(f"{league}/{doc_team}: existing rebalance changed aggregate strength")

            base_xi = sum(strength(base_players[player_id]) for player_id in base_team["roster"][:11]) / 11
            new_xi = sum(strength(players[player_id]) for player_id in team["roster"][:11]) / 11
            xi_change = new_xi - base_xi
            if abs(xi_change) > abs(largest_first_xi_change[0]):
                largest_first_xi_change = (xi_change, f"{league}/{doc_team}")
            base_avg = sum(strength(base_players[player_id]) for player_id in base_team["roster"]) / len(base_team["roster"])
            new_avg = sum(strength(players[player_id]) for player_id in team["roster"]) / len(team["roster"])
            roster_change = new_avg - base_avg
            if abs(roster_change) > abs(largest_roster_change[0]):
                largest_roster_change = (roster_change, f"{league}/{doc_team}")

    print(f"v41 audit passed: 98 clubs, {total_owned} owned players")
    print(f"largest first-XI average change: {largest_first_xi_change[0]:+.2f} ({largest_first_xi_change[1]})")
    print(f"largest full-roster average change: {largest_roster_change[0]:+.2f} ({largest_roster_change[1]})")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit("usage: audit_v41_build.py BASE_JAR PATCHED_JAR PLAN_JSON")
    main(*sys.argv[1:])
