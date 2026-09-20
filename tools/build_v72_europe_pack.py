#!/usr/bin/env python3
"""Build the audited v72 European club registry from the supplied research pack."""
from __future__ import annotations

import argparse
import csv
import difflib
import io
import re
import shutil
import unicodedata
import zipfile
from pathlib import Path

from docx import Document
from PIL import Image


def key(value: str) -> str:
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]", "", value)


def slug(value: str) -> str:
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().lower()
    return re.sub(r"(^-|-$)", "", re.sub(r"[^a-z0-9]+", "-", value))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("guide", type=Path)
    ap.add_argument("squads", type=Path)
    ap.add_argument("logos", type=Path)
    ap.add_argument("domestic", type=Path)
    ap.add_argument("affinity", type=Path)
    ap.add_argument("output", type=Path)
    args = ap.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    guide = Document(args.guide)
    master = [[c.text.strip() for c in r.cells] for r in guide.tables[1].rows][1:]
    additions = [[c.text.strip() for c in r.cells] for r in guide.tables[2].rows][1:]
    clubs = []
    for row in master:
        country, name, short, city, tier, europe, rep, fin, acad, profile, priority, note = row
        clubs.append(dict(country=country, name=name, short=short, city=city, tier=tier,
                          europe=europe, rep=rep, fin=fin, acad=acad, profile=profile,
                          priority=priority, note=note))
    existing = {key(c["name"]) for c in clubs}
    for priority, country, name, why in additions:
        # The supplied logo archive intentionally has no Unirea asset; keep the
        # short-lived Priority-C suggestion in the research notes, not runtime.
        if priority == "C":
            continue
        if key(name) not in existing:
            clubs.append(dict(country=country, name=name, short=name, city="", tier="Top tier",
                              europe="UEFA qualifying", rep="3", fin="3", acad="3",
                              profile="BALANCED", priority=priority, note=why))

    corrections = {
        "fcsb": "Steaua București", "astragiurgiu": "Astra Ploiești",
    }
    for club in clubs:
        club["name"] = corrections.get(key(club["name"]), club["name"])
        club["short"] = corrections.get(key(club["short"]), club["short"])
        club["active_from"] = "2011" if key(club["name"]) in {
            "ludogoretsrazgrad", "fckrasnodar", "kubankrasnodar"
        } else "2010"
        club["id"] = "eu_" + slug(club["country"]) + "_" + slug(club["name"])

    # Match logos by country and a small explicit historical-name alias list.
    aliases = {
        "steauabucuresti": "fcsb", "astraploiesti": "astragiurgiu", "bateborisov": "bate",
        "litexlovech": "pfclitexlovech", "omonianicosia": "omonia", "aabaalborg": "aalborgboldspilklub",
        "odensebk": "odenseboldklub", "redstarbelgrade": "fcredstarbelgrade",
        "dniprodnipropetrovsk": "fcdniprodnipropetrovsk", "partizanbelgrade": "fkpartizan",
        "anzhimakhachkala": "fkanzhimakhachkala", "zenitstpetersburg": "zenitsaintpetersburg",
        "wisakrakow": "wislakrakow", "tromsil": "tromsoil", "brndbyif": "brondbyif",
    }
    badge_dir = args.output / "europe_badges_v72"
    if badge_dir.exists(): shutil.rmtree(badge_dir)
    badge_dir.mkdir()
    with zipfile.ZipFile(args.logos) as zf:
        logo_entries = [n for n in zf.namelist() if not n.endswith("/")]
        by_country = {}
        for name in logo_entries:
            parts = Path(name).parts
            by_country.setdefault(key(parts[-2]), []).append(name)
        for country in sorted({c["country"] for c in clubs}):
            group = [c for c in clubs if c["country"] == country]
            available = list(by_country.get(key(country), []))
            if len(group) != len(available): raise SystemExit(f"Logo/club count mismatch for {country}: {len(group)} != {len(available)}")
            while group:
                scored = []
                for club in group:
                    wanted = aliases.get(key(club["name"]), key(club["name"]))
                    for source in available:
                        actual = key(Path(source).stem)
                        score = difflib.SequenceMatcher(None, wanted, actual).ratio()
                        if wanted == actual: score += 2
                        elif wanted in actual or actual in wanted: score += 1
                        scored.append((score, club, source))
                _, club, source = max(scored, key=lambda x: x[0])
                badge = club["id"] + ".png"
                with Image.open(io.BytesIO(zf.read(source))) as image:
                    image = image.convert("RGBA"); image.thumbnail((256, 256), Image.Resampling.LANCZOS)
                    image.save(badge_dir / badge, "PNG", optimize=True)
                club["badge"] = badge
                group.remove(club); available.remove(source)

    cols = ["id","country","name","short","city","tier","europe","rep","fin","acad","profile","priority","active_from","badge","note"]
    with (args.output / "europe_clubs_v72.tsv").open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, cols, delimiter="\t"); w.writeheader(); w.writerows(clubs)
    shutil.copyfile(args.domestic, args.output / "transfer_domestic_share_v72.csv")
    shutil.copyfile(args.affinity, args.output / "transfer_international_affinity_v72.csv")

    # Roster audit: exact normalized duplicates, including collisions with the top-five database.
    squads = Document(args.squads)
    occurrences = {}
    country = ""
    roster_count = player_count = 0
    for i, p in enumerate(squads.paragraphs):
        if p.style.name.startswith("Heading 2"): country = p.text.strip()
        if i + 2 < len(squads.paragraphs) and ";" in squads.paragraphs[i+1].text and squads.paragraphs[i+2].text.startswith("Source basis:"):
            roster_count += 1
            club = p.text.strip().replace("  [ADD]", "")
            for player in [x.strip() for x in squads.paragraphs[i+1].text.split(";") if x.strip()]:
                player_count += 1; occurrences.setdefault(key(player), []).append((player, club, country))
    duplicates = [v for v in occurrences.values() if len(v) > 1]
    report = ["# European data audit v72", "", f"- Club records: {len(clubs)}", f"- Logos: {len(list(badge_dir.iterdir()))}",
              f"- Supplied roster lists: {roster_count}", f"- Supplied player entries: {player_count}",
              f"- Exact normalized identities appearing at multiple clubs: {len(duplicates)}", "",
              "Roster entries are deliberately not imported in v72 until every ownership conflict is resolved.", "", "## Duplicate identities", ""]
    report += ["- " + " | ".join(f"{n} — {c} ({co})" for n,c,co in rows) for rows in duplicates]
    (args.output / "EUROPE_DATA_AUDIT_V72.md").write_text("\n".join(report)+"\n", encoding="utf-8")


if __name__ == "__main__": main()
