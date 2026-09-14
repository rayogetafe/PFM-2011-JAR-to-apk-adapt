#!/usr/bin/env python3
"""Build high-resolution club badges while preserving the 2010/11 identities.

The original 47x50 sprites are used to match the game's opaque numeric badge
IDs to named, transparent PNG artwork.  The output remains numeric so both the
legacy canvas and the native Android screens can use the same files.
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops
from scipy.optimize import linear_sum_assignment


LEAGUES = {
    "en": ("England", [
        "Arsenal FC", "Aston Villa", "Birmingham City", "Blackburn Rovers",
        "Blackpool FC", "Bolton Wanderers", "Chelsea FC", "Everton FC",
        "Fulham FC", "Liverpool FC", "Manchester City", "Manchester United",
        "Newcastle United", "Stoke City", "Sunderland AFC", "Tottenham Hotspur",
        "West Bromwich Albion", "West Ham United", "Wigan Athletic",
        "Wolverhampton Wanderers",
    ]),
    "es": ("Spain", [
        "UD Almeria", "Athletic Bilbao", "Atletico de Madrid", "FC Barcelona",
        "Deportivo La Coruna", "RCD Espanyol", "Getafe CF", "Hercules CF",
        "Levante UD", "Malaga CF", "RCD Mallorca", "CA Osasuna",
        "Racing Santander", "Real Madrid", "Real Sociedad", "Sevilla FC",
        "Sporting Gijon", "Valencia CF", "Villarreal CF", "Real Zaragoza",
    ]),
    "it": ("Italy", [
        "AS Roma", "SSC Napoli", "Inter Milan", "AC Milan", "Palermo FC",
        "Sampdoria", "Juventus FC", "Udinese Calcio", "ACF Fiorentina",
        "Cesena FC", "SS Lazio", "Catania FC", "Chievo Verona",
        "Cagliari Calcio", "US Lecce", "Genoa CFC", "Bologna FC 1909",
        "Parma Calcio 1913", "SSC Bari", "Brescia Calcio",
    ]),
    "de": ("Germany", [
        "Bayer 04 Leverkusen", "Bayern Munich", "Borussia Dortmund",
        "Borussia Monchengladbach", "Eintracht Frankfurt", "SC Freiburg",
        "Hamburger SV", "Hannover 96", "TSG 1899 Hoffenheim",
        "1.FC Kaiserslautern", "1.FC Koln", "1.FSV Mainz 05",
        "1.FC Nurnberg", "FC Schalke 04", "FC St. Pauli", "VfB Stuttgart",
        "SV Werder Bremen", "VfL Wolfsburg",
    ]),
    "fr": ("France", [
        "AC Arles-Avignon", "AJ Auxerre", "Girondins Bordeaux", "Stade Brestois 29",
        "SM Caen", "RC Lens", "LOSC Lille", "FC Lorient", "Olympique Lyon",
        "Olympique Marseille", "AS Monaco", "AS Nancy Lorraine", "OGC Nice",
        "Paris Saint-Germain", "Stade Rennais FC", "AS Saint-Etienne",
        "FC Sochaux", "FC Toulouse", "Valenciennes FC", "Montpellier HSC",
    ]),
}


ALIASES = {
    "Birmingham City": "Birmingham City.png",
    "Blackburn Rovers": "Blackburn Rovers.png",
    "Blackpool FC": "Blackpool FC.png",
    "Bolton Wanderers": "Bolton Wanderers.png",
    "Stoke City": "Stoke City.png",
    "West Bromwich Albion": "West Bromwich Albion.png",
    "West Ham United": "West Ham United.png",
    "Wigan Athletic": "Wigan Athletic.png",
    "Wolverhampton Wanderers": "Wolverhampton Wanderers.png",
    "UD Almeria": "UD Almería.png",
    "Atletico de Madrid": "Atlético de Madrid.png",
    "Deportivo La Coruna": "Deportivo A Coruña.png",
    "RCD Espanyol": "RCD Espanyol Barcelona.png",
    "Hercules CF": "Hércules CF.png",
    "Malaga CF": "Málaga CF.png",
    "RCD Mallorca": "RCD Mallorca.png",
    "Sporting Gijon": "Sporting Gijón.png",
    "Real Zaragoza": "Real Zaragoza.png",
    "Palermo FC": "Palermo FC.png",
    "Sampdoria": "UC Sampdoria.png",
    "Cesena FC": "Cesena FC.png",
    "Catania FC": "Catania FC.png",
    "Chievo Verona": "Chievo Verona.png",
    "SSC Bari": "SSC Bari.png",
    "Brescia Calcio": "Brescia Calcio.png",
    "Borussia Monchengladbach": "Borussia Mönchengladbach.png",
    "1.FC Kaiserslautern": "1.FC Kaiserslautern.png",
    "1.FC Koln": "1.FC Köln.png",
    "1.FC Nurnberg": "1.FC Nürnberg.png",
    "Girondins Bordeaux": "Girondins Bordeaux.png",
    "AC Arles-Avignon": "AC Arles-Avignon.png",
    "SM Caen": "SM Caen.png",
    "AS Nancy Lorraine": "AS Nancy Lorraine.png",
    "AS Saint-Etienne": "AS Saint-Étienne.png",
    "FC Sochaux": "FC Sochaux-Montbéliard.png",
    "Valenciennes FC": "Valenciennes FC.png",
    "Montpellier HSC": "Montpellier HSC.png",
}

# These clubs changed their identity materially after 2010/11. A modern,
# sharper logo would be the wrong asset for this database, so retain the
# bundled historical crest for them.
FORCE_HISTORICAL = {
    "Manchester City", "Everton FC", "AS Roma", "Juventus FC",
    "Inter Milan", "LOSC Lille", "Paris Saint-Germain",
}


def named_logo(root: Path, name: str) -> Path | None:
    wanted = ALIASES.get(name, name + ".png")
    matches = list((root / "logos").glob("*/" + wanted))
    return matches[0] if matches else None


def feature(path: Path, size: int = 40) -> np.ndarray:
    im = Image.open(path).convert("RGBA")
    alpha = im.getchannel("A")
    box = alpha.getbbox()
    if box:
        im = im.crop(box)
    im.thumbnail((size - 4, size - 4), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(im, ((size - im.width) // 2, (size - im.height) // 2))
    a = np.asarray(canvas, dtype=np.float32) / 255.0
    alpha = a[..., 3]
    spatial = np.concatenate((alpha[..., None] * 1.2, a[..., :3] * alpha[..., None]), axis=2).ravel()
    hsv = np.asarray(canvas.convert("HSV"), dtype=np.float32) / 255.0
    mask = alpha > 0.15
    weights = alpha[mask]
    hist = []
    for channel, bins in ((0, 18), (1, 8), (2, 8)):
        h, _ = np.histogram(hsv[..., channel][mask], bins=bins, range=(0, 1), weights=weights)
        hist.extend((h / max(1.0, h.sum()) * 7.0).tolist())
    return np.concatenate((spatial, np.asarray(hist, dtype=np.float32)))


def prepare(src: Path, dst: Path) -> None:
    im = Image.open(src).convert("RGBA")
    alpha = im.getchannel("A")
    box = alpha.getbbox()
    if box:
        im = im.crop(box)
    im.thumbnail((220, 220), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    out.alpha_composite(im, ((256 - im.width) // 2, (256 - im.height) // 2))
    dst.parent.mkdir(parents=True, exist_ok=True)
    out.save(dst, optimize=True)


def prepare_legacy(src: Path, reference: Path, dst: Path) -> None:
    """Keep the exact J2ME canvas size; the legacy UI does not scale images."""
    source = Image.open(src).convert("RGBA")
    alpha = source.getchannel("A")
    box = alpha.getbbox()
    if box:
        source = source.crop(box)
    with Image.open(reference) as old:
        size = old.size
    source.thumbnail((max(1, size[0] - 2), max(1, size[1] - 2)), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    out.alpha_composite(source, ((size[0] - source.width) // 2, (size[1] - source.height) // 2))
    dst.parent.mkdir(parents=True, exist_ok=True)
    out.save(dst, optimize=True)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("originals", type=Path)
    ap.add_argument("named_logos", type=Path)
    ap.add_argument("output", type=Path)
    ap.add_argument("legacy_output", type=Path)
    args = ap.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    args.legacy_output.mkdir(parents=True, exist_ok=True)

    for code, (_, names) in LEAGUES.items():
        originals = [args.originals / f"{code}{i}.png" for i in range(1, len(names) + 1)]
        resolved = [(n, named_logo(args.named_logos, n)) for n in names]
        resolved = [(n, p) for n, p in resolved if p is not None]
        named_names = [n for n, _ in resolved]
        named = [p for _, p in resolved]
        old_f = [feature(p) for p in originals]
        new_f = [feature(p) for p in named]
        costs = np.array([[np.mean((a - b) ** 2) for b in new_f] for a in old_f])
        rows, cols = linear_sum_assignment(costs)
        mapping = dict(zip(rows, cols))
        print(f"[{code}]")
        for old_idx in range(len(originals)):
            name_idx = mapping.get(old_idx)
            # A high distance normally means the club has since adopted a new
            # crest.  In that case the small original is the safer historical
            # source than a crisp but anachronistic replacement.
            if (name_idx is None or named_names[name_idx] in FORCE_HISTORICAL
                    or costs[old_idx, name_idx] > 0.24):
                print(f"{code}{old_idx+1:02d} -> historical original")
                prepare(originals[old_idx], args.output / f"{code}{old_idx+1}.png")
                shutil.copy2(originals[old_idx], args.legacy_output / f"{code}{old_idx+1}.png")
            else:
                print(f"{code}{old_idx+1:02d} -> {named_names[name_idx]} ({costs[old_idx,name_idx]:.4f})")
                prepare(named[name_idx], args.output / f"{code}{old_idx+1}.png")
                prepare_legacy(named[name_idx], originals[old_idx], args.legacy_output / f"{code}{old_idx+1}.png")

    # The international selection is not a domestic 2010/11 league. Keep its
    # historically bundled artwork, but put it on a clean high-resolution canvas.
    for i in range(1, 21):
        prepare(args.originals / f"in{i}.png", args.output / f"in{i}.png")
        args.legacy_output.mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.originals / f"in{i}.png", args.legacy_output / f"in{i}.png")


if __name__ == "__main__":
    main()
