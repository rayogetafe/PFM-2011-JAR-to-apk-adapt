#!/usr/bin/env python3
"""Build v40 badges from an explicit team-index manifest.

The league binaries define the actual club order used by en1..fr20.  Never
infer that order from visual similarity: the bundled J2ME badges are fictional
and their colours are not authoritative club identifiers.
"""
from __future__ import annotations

import argparse
from collections import deque
import io
from pathlib import Path
import tarfile

from PIL import Image


LEAGUES = {
    "en": ("England", [
        ("Chelsea", "chelsea-fc.png"),
        ("Man United", "manchester-united.png"),
        ("Arsenal", "arsenal-fc.png"),
        ("Tottenham", "tottenham-hotspur.png"),
        ("Man City", "manchester-city.png"),
        ("Aston Villa", "aston-villa.png"),
        ("Liverpool", "liverpool-fc.png"),
        ("Everton", "everton-fc.png"),
        ("Birmingham", "birmingham-city.png"),
        ("Blackburn", "blackburn-rovers.png"),
        ("Stoke City", "stoke-city.png"),
        ("Fulham", "fulham-fc.png"),
        ("Sunderland", "sunderland-afc.png"),
        ("Bolton", "bolton-wanderers.png"),
        ("Wolves", "wolverhampton-wanderers.png"),
        ("Wigan", "wigan-athletic.png"),
        ("West Ham", "west-ham-united.png"),
        ("Newcastle", "newcastle-united.png"),
        ("West Brom", "west-bromwich-albion.png"),
        ("Blackpool", "blackpool-fc.png"),
    ]),
    "es": ("Spain", [
        ("Barcelona", "fc-barcelona.png"),
        ("Real Madrid", "real-madrid.png"),
        ("Sevilla", "sevilla-fc.png"),
        ("Atl. Madrid", "atletico-madrid.png"),
        ("Villarreal", "villarreal-cf.png"),
        ("Valencia", "valencia-cf.png"),
        ("Deportivo", "deportivo-de-a-coruna.png"),
        ("Malaga", "malaga-cf.png"),
        ("Mallorca", "rcd-mallorca.png"),
        ("Espanyol", "rcd-espanyo.png"),
        ("Almeria", "ud-almeria.png"),
        ("Racing", "racing-de-santander.png"),
        ("Ath. Bilbao", "athletic-club.png"),
        ("Sporting", "real-sporting-de-gijon.png"),
        ("Osasuna", "ca-osasuna.png"),
        ("Levante", "levante-ud.png"),
        ("Getafe", "getafe-cf.png"),
        ("R. Sociedad", "real-sociedad-logo-footylogos.png"),
        ("Zaragoza", "real-zaragoza.png"),
        ("Hercules", "hercules-cf.png"),
    ]),
    "it": ("Italy", [
        ("Inter", "inter-milan.png"),
        ("Roma", "as-roma.png"),
        ("Milan", "ac-milan.png"),
        ("Sampdoria", "uc-sampdoria.png"),
        ("Palermo", "palermo-fc.png"),
        ("Napoli", "ssc-napoli.png"),
        ("Juventus", "juventus-fc.png"),
        ("Parma", "parma-calcio.png"),
        ("Genoa", "genoa-cfc.png"),
        ("Bari", "ssc-bar.png"),
        ("Fiorentina", "acf-fiorentina.png"),
        ("Lazio", "ss-lazio.png"),
        ("Catania", "catania-fc.jpg"),
        ("Chievo", None),
        ("Udinese", "udinese-calcio.png"),
        ("Cagliari", "cagliari-calcio.png"),
        ("Bologna", "bologna-fc.png"),
        ("Lecce", "us-lecce.png"),
        ("Cesena", "cesena-f.png"),
        ("Brescia", "brescia-calcio.png"),
    ]),
    "de": ("Germany", [
        ("Bayern", "bayern-munchen.png"),
        ("Schalke 04", "schalke-04.png"),
        ("Werder", "werder-bremen.png"),
        ("Leverkusen", "bayer-04-leverkusen.png"),
        ("Dortmund", "borussia-dortmund.png"),
        ("Stuttgart", "vfb-stuttgart.png"),
        ("Hamburg", "hamburger-sv-.png"),
        ("Wolfsburg", "vfl-wolfsburg.png"),
        ("Mainz 05", "1-fsv-mainz-05.png"),
        ("Frankfurt", "eintracht-frankfurt.png"),
        ("Hoffenheim", "tsg-hoffenheim.png"),
        ("M'gladbach", "borussia-monchengladbach.png"),
        ("Koln", "1-fc-koln.png"),
        ("Freiburg", "sc-freiburg.png"),
        ("Hannover 96", "hannover-9.png"),
        ("Nurnberg", "1-fc-nurnberg.png"),
        ("K'lautern", "1-fc-kaiserslautern.png"),
        ("St. Pauli", "fc-st-pauli.png"),
    ]),
    "fr": ("France", [
        ("Marseille", "olympique-marseill.png"),
        ("Lyon", "olympique-lyonnais.png"),
        ("Auxerre", "aj-auxerre.png"),
        ("Lille", "lille-losc.png"),
        ("Montpellier", "montpellier-hsc.png"),
        ("Bordeaux", "girondins-bordeaux.png"),
        ("Lorient", "fc-lorient.png"),
        ("Monaco", "as-monaco.png"),
        ("Rennes", "stade-rennais.png"),
        ("Valenciennes", "valenciennes-fc.png"),
        ("Lens", "rc-lens.png"),
        ("Nancy", "as-nancy-lorraine.png"),
        ("Paris SG", "paris-saint-germain.png"),
        ("Toulouse", "toulouse-fc.png"),
        ("Nice", "ogc-nice.png"),
        ("Sochaux", "sochaux-montbeliard.png"),
        ("St-Etienne", "as-saint-etienne.png"),
        ("Caen", "sm-caen.png"),
        ("Brest", "stade-brestois.png"),
        ("Arles", "ac-arlesien.png"),
    ]),
}

DATABASES = {"en": "england.bin", "es": "spain.bin", "it": "italy.bin",
             "de": "germany.bin", "fr": "france.bin"}


def database_team_names(path: Path) -> list[str]:
    data = path.read_bytes()
    position = 0

    def u8() -> int:
        nonlocal position
        value = data[position]
        position += 1
        return value

    def text() -> str:
        nonlocal position
        size = u8()
        value = data[position:position + size].decode("iso-8859-1").replace("\n", " ").strip()
        position += size
        return value

    names = []
    for _ in range(u8()):
        names.append(text())
        text()  # long display name/description
        position += 16
        roster_count = u8()
        position += 2 * roster_count
        listed_count = u8()
        position += 2 * listed_count
        position += 29
        score_count = u8()
        position += score_count
    return names


def transparent_edge_background(image: Image.Image) -> Image.Image:
    """Remove only pale background pixels connected to the image edge."""
    out = image.convert("RGBA")
    px = out.load()
    width, height = out.size
    queue = deque()
    seen = set()
    for x in range(width):
        queue.append((x, 0)); queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y)); queue.append((width - 1, y))
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen:
            continue
        seen.add((x, y))
        r, g, b, a = px[x, y]
        pale = a == 0 or (min(r, g, b) >= 218 and max(r, g, b) - min(r, g, b) <= 28)
        if not pale:
            continue
        px[x, y] = (r, g, b, 0)
        if x: queue.append((x - 1, y))
        if x + 1 < width: queue.append((x + 1, y))
        if y: queue.append((x, y - 1))
        if y + 1 < height: queue.append((x, y + 1))
    return out


def prepare(source: Path, size: tuple[int, int], margin: int) -> Image.Image:
    with Image.open(source) as opened:
        # Source packs contain multi-megapixel artwork.  Reduce it before the
        # edge-connected background pass; both final targets are <=256 px.
        image = opened.convert("RGBA")
        image.thumbnail((640, 640), Image.Resampling.LANCZOS)
        image = transparent_edge_background(image)
    box = image.getchannel("A").getbbox()
    if box:
        image = image.crop(box)
    max_size = (max(1, size[0] - margin * 2), max(1, size[1] - margin * 2))
    image.thumbnail(max_size, Image.Resampling.LANCZOS)
    result = Image.new("RGBA", size, (0, 0, 0, 0))
    result.alpha_composite(image, ((size[0] - image.width) // 2, (size[1] - image.height) // 2))
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("originals", type=Path)
    parser.add_argument("supplied", type=Path)
    parser.add_argument("native_output", type=Path)
    parser.add_argument("legacy_output", type=Path)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--archive-dir", type=Path)
    args = parser.parse_args()
    args.native_output.mkdir(parents=True, exist_ok=True)
    args.legacy_output.mkdir(parents=True, exist_ok=True)
    used = set()
    rows = ["resource\tclub\tsource\tstatus"]

    for code, (country, clubs) in LEAGUES.items():
        actual_names = database_team_names(args.originals / DATABASES[code])
        declared_names = [club for club, _ in clubs]
        if actual_names != declared_names:
            raise RuntimeError(f"{code} database order mismatch: {actual_names!r} != {declared_names!r}")
        for index, (club, filename) in enumerate(clubs, 1):
            resource = f"{code}{index}.png"
            original = args.originals / resource
            if not original.is_file():
                raise FileNotFoundError(original)
            if filename is None:
                source = original
                status = "original fallback: Chievo source missing"
            else:
                source = args.supplied / country / filename
                if not source.is_file():
                    raise FileNotFoundError(source)
                used.add(source.resolve())
                status = "explicit supplied mapping"
            native = prepare(source, (256, 256), 18)
            with Image.open(original) as old:
                legacy_size = old.size
            legacy = prepare(source, legacy_size, 1)
            native.save(args.native_output / resource, optimize=True)
            legacy.save(args.legacy_output / resource, optimize=True)
            rows.append(f"{resource}\t{club}\t{country}/{filename or '[bundled original]'}\t{status}")

    for index in range(1, 21):
        resource = f"in{index}.png"
        original = args.originals / resource
        prepare(original, (256, 256), 18).save(args.native_output / resource, optimize=True)
        with Image.open(original) as old:
            old.convert("RGBA").save(args.legacy_output / resource, optimize=True)
        rows.append(f"{resource}\tInternational {index}\t[bundled original]\tnot a domestic league")

    supplied = {path.resolve() for path in args.supplied.glob("*/*") if path.is_file()}
    ignored = sorted(path.as_posix() for path in supplied - used)
    if ignored != [str((args.supplied / "Italy" / "atalanta-bc.png").resolve()).replace("\\", "/")]:
        raise RuntimeError(f"unexpected unused supplied files: {ignored}")
    if len(used) != 97:
        raise RuntimeError(f"expected 97 explicitly mapped supplied badges, got {len(used)}")
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text("\n".join(rows) + "\n", encoding="utf-8")
    if args.archive_dir:
        args.archive_dir.mkdir(parents=True, exist_ok=True)
        for old_part in args.archive_dir.glob("part-*"):
            old_part.unlink()
        buffer = io.BytesIO()
        with tarfile.open(fileobj=buffer, mode="w:gz") as bundle:
            bundle.add(args.native_output, arcname="badges_v40")
            bundle.add(args.legacy_output, arcname="badges_v40_legacy")
        archive = buffer.getvalue()
        chunk = 160 * 1024
        for index, start in enumerate(range(0, len(archive), chunk)):
            (args.archive_dir / f"part-{index:02d}").write_bytes(archive[start:start + chunk])
        print(f"v40 badge archive: {len(archive)} bytes in {(len(archive) + chunk - 1) // chunk} parts")
    print("v40 badges: 97 explicit supplied mappings, Chievo fallback, Atalanta ignored")


if __name__ == "__main__":
    main()
