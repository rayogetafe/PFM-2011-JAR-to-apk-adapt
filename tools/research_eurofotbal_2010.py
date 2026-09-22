#!/usr/bin/env python3
"""Collect factual 2010/11 roster fields from EuroFotbal's season archive.

Run from the repository root. Outputs are reviewable TSVs; ambiguous club
matches are not silently accepted. This script does not assign game ratings.
"""
import argparse
import csv
import difflib
import re
import unicodedata
import urllib.request
from collections import defaultdict
from pathlib import Path
from urllib.parse import urljoin

from lxml import html

BASE = "https://www.eurofotbal.cz"
COUNTRIES = {
    "Austria": "rakousko", "Belgium": "belgie", "Belarus": "belorusko",
    "Bulgaria": "bulharsko", "Croatia": "chorvatsko", "Cyprus": "kypr",
    "Czech": "cesko", "Denmark": "dansko", "Finland": "finsko",
    "Greece": "recko", "Hungary": "madarsko", "Ireland": "irsko",
    "Israel": "izrael", "Moldova": "moldavsko", "Netherlands": "nizozemsko",
    "Norway": "norsko", "Poland": "polsko", "Portugal": "portugalsko",
    "Romania": "rumunsko", "Russia": "rusko", "Scotland": "skotsko",
    "Serbia": "srbsko", "Slovakia": "slovensko", "Slovenia": "slovinsko",
    "Sweden": "svedsko", "Switzerland": "svycarsko", "Turkey": "turecko",
    "Ukraine": "ukrajina", "Azerbaijan": "azerbajdzan",
}
ALIASES = {
    "Austria Wien": "Austria Vídeň", "Rapid Wien": "Rapid Vídeň",
    "FC Copenhagen": "FC Kodaň", "Brøndby IF": "Bröndby Kodaň",
    "Red Bull Salzburg": "Salzburg", "Zenit St. Petersburg": "Zenit Petrohrad",
}
PATHS = {
    "eu_azerbaijan_neftci-pfk": "neftci-baku",
    "eu_azerbaijan_qarabag-fk": "qarabag-agdam",
    "eu_bulgaria_ludogorets-razgrad": "ludogorec-razgrad",
    "eu_bulgaria_litex-lovech": "litex-lovec",
    "eu_croatia_dinamo-zagreb": "dinamo-zahreb",
    "eu_croatia_hnk-rijeka": "nk-rijeka",
    "eu_cyprus_aek-larnaca": "aek-larnaka",
    "eu_cyprus_apoel": "apoel-nikosie",
    "eu_cyprus_omonia-nicosia": "omonia-nikosie",
    "eu_finland_hjk-helsinki": "hjk-helsinky",
    "eu_greece_aek-athens": "aek-ateny",
    "eu_greece_aris-thessaloniki": "aris-solun",
    "eu_greece_olympiacos": "olympiakos-pireus",
    "eu_hungary_videoton-fc": "videoton-fc-fehervar",
    "eu_hungary_ferencvaros": "ferencvarosi-tc",
    "eu_netherlands_ajax": "afc-ajax",
    "eu_poland_legia-warsaw": "legia-varsava",
    "eu_portugal_pacos-de-ferreira": "pacos-de-ferreira",
    "eu_portugal_rio-ave": "rio-ave-fc",
    "eu_portugal_vitoria-sc": "vitoria-sc-guimaraes",
    "eu_portugal_vitoria-setubal": "vitoria-fc-setubal",
    "eu_romania_sc-vaslui": "fc-vaslui",
    "eu_romania_astra-ploiesti": "astra-giurgiu",
    "eu_romania_dinamo-bucuresti": "dinamo-bukurest",
    "eu_romania_rapid-bucuresti": "rapid-1923",
    "eu_romania_steaua-bucuresti": "fcsb",
    "eu_romania_fc-timisoara": "politehnica-temesvar",
    "eu_russia_cska-moscow": "cska-moskva",
    "eu_russia_fc-krasnodar": "fk-krasnodar",
    "eu_russia_kuban-krasnodar": "kuban-krasnodar",
    "eu_russia_lokomotiv-moscow": "lokomotiv-moskva",
    "eu_russia_spartak-moscow": "spartak-moskva",
    "eu_russia_zenit-st-petersburg": "zenit-petrohrad",
    "eu_russia_dynamo-moscow": "dinamo-moskva",
    "eu_scotland_celtic": "celtic-fc",
    "eu_scotland_rangers": "rangers-fc",
    "eu_serbia_partizan-belgrade": "fk-partizan",
    "eu_serbia_red-star-belgrade": "fk-crvena-zvezda",
    "eu_sweden_aik": "aik-solna",
    "eu_switzerland_fc-basel": "fc-basilej",
    "eu_switzerland_fc-zurich": "fc-curych",
    "eu_turkey_besiktas": "besiktas-jk",
    "eu_turkey_fenerbahce": "fenerbahce-sk",
    "eu_turkey_galatasaray": "galatasaray-sk",
    "eu_ukraine_dynamo-kyiv": "dynamo-kyjev",
    "eu_ukraine_karpaty-lviv": "karpaty-lvov",
    "eu_ukraine_metalist-kharkiv": "metalist-charkov",
    "eu_ukraine_shakhtar-donetsk": "sachtar-doneck",
    "eu_ukraine_dnipro-dnipropetrovsk": "dnepr-dnepropetrovsk",
    "eu_austria_red-bull-salzburg": "red-bull-salzburg",
    "eu_austria_sturm-graz": "sk-sturm-graz",
    "eu_belgium_club-brugge-kv": "club-bruggy",
    "eu_belgium_standard-liege": "standard-lutych",
    "eu_denmark_aab-aalborg": "aalborg-bk",
    "eu_denmark_fc-nordsjlland": "fc-nordsjaelland",
    "eu_denmark_odense-bk": "odense-boldklub",
    "eu_denmark_brndby-if": "brondby-if",
    "eu_greece_paok": "paok-solun",
    "eu_greece_panathinaikos": "panathinaikos-fc",
    "eu_netherlands_feyenoord": "feyenoord-rotterdam",
    "eu_norway_valerenga": "valerenga-if",
    "eu_russia_anzhi-makhachkala": "anzi-machackala",
    "eu_scotland_aberdeen": "aberdeen-fc",
    "eu_switzerland_grasshopper-club-zurich": "grasshopper-curych",
}
CALENDAR = {"Russia", "Norway", "Sweden", "Finland", "Ireland", "Belarus"}
HEADINGS = {"Brankáři": "GK", "Obránci": "DEF", "Záložníci": "MID", "Útočníci": "ATT"}


def norm(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]", "", s)


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (historical roster research)"})
    return html.fromstring(urllib.request.urlopen(req, timeout=18).read())


def write(path, columns, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=columns, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--clubs", type=Path, default=Path("overrides/app/src/main/assets/europe_clubs_v72.tsv"))
    ap.add_argument("--mapping", type=Path, default=Path("tools/eurofotbal_club_mapping.tsv"))
    ap.add_argument("--facts", type=Path, default=Path("tools/eurofotbal_roster_facts.tsv"))
    ap.add_argument("--catalog-only", action="store_true")
    args = ap.parse_args()
    with args.clubs.open(encoding="utf-8", newline="") as f:
        clubs = list(csv.DictReader(f, delimiter="\t"))
    by_country = defaultdict(list)
    for c in clubs: by_country[c["country"]].append(c)
    prior = {}
    if args.mapping.exists():
        with args.mapping.open(encoding="utf-8", newline="") as f:
            prior = {x["club_id"]: x for x in csv.DictReader(f, delimiter="\t")}
    mapping = []
    for country, group in by_country.items():
        page = f"{BASE}/kluby/{COUNTRIES[country]}/"
        try:
            doc = fetch(page)
            links = [(a.text_content().strip(), urljoin(BASE, a.attrib["href"]))
                     for a in doc.xpath('//a[starts-with(@href,"/kluby/")]')
                     if re.match(rf"^/kluby/{COUNTRIES[country]}/[^/]+/$", a.attrib["href"])]
            links = list(dict.fromkeys(links))
        except Exception as ex:
            print("CATALOG ERROR", country, ex)
            links = []
        for c in group:
            if c["id"] in PATHS:
                slug = PATHS[c["id"]]
                season = "2010" if country in CALENDAR else "2010-2011"
                mapping.append(dict(club_id=c["id"], club=c["name"], source_name="MANUALLY_MAPPED",
                                    source_url=f"{BASE}/kluby/{COUNTRIES[country]}/{slug}/soupiska/{season}/",
                                    score="1.000", status="CONFIRMED"))
                continue
            old = prior.get(c["id"])
            if old and old["status"] == "CONFIRMED":
                if country in CALENDAR:
                    old["source_url"] = old["source_url"].replace("/soupiska/2010-2011/", "/soupiska/2010/")
                mapping.append(old)
                continue
            wanted = norm(ALIASES.get(c["name"], c["name"]))
            ranked = sorted(((difflib.SequenceMatcher(None, wanted, norm(name)).ratio(), name, url)
                             for name, url in links), reverse=True)
            score, name, url = ranked[0] if ranked else (0, "", "")
            # Require either an exact normalized alias or a manual review.
            season = "2010" if country in CALENDAR else "2010-2011"
            mapping.append(dict(club_id=c["id"], club=c["name"], source_name=name,
                                source_url=url+f"soupiska/{season}/" if url else "",
                                score=f"{score:.3f}", status="CONFIRMED" if score == 1 else "REVIEW"))
            if score != 1: print("REVIEW", c["id"], f"{score:.3f}", name, url)
    write(args.mapping, ["club_id", "club", "source_name", "source_url", "score", "status"], mapping)
    print("MAPPED", sum(x["status"] == "CONFIRMED" for x in mapping), "/", len(mapping))
    if args.catalog_only: return
    facts = []
    cached = defaultdict(list)
    if args.facts.exists():
        with args.facts.open(encoding="utf-8", newline="") as f:
            for row in csv.DictReader(f, delimiter="\t"):
                cached[(row["club_id"], row["source_url"])].append(row)
    for x in mapping:
        if x["status"] != "CONFIRMED": continue
        if cached[(x["club_id"], x["source_url"])]:
            facts.extend(cached[(x["club_id"], x["source_url"])])
            continue
        try:
            doc = fetch(x["source_url"])
            count_before = len(facts)
            for table in doc.xpath('//div[@role="table"][@aria-label="Tabulka klub soupiska"]'):
                headings = table.xpath('preceding::h3[1]')
                label = headings[0].text_content().strip() if headings else ""
                pos = HEADINGS.get(label)
                if not pos: continue
                for row in table.xpath('.//div[@role="row"][@onclick]'):
                    cells = [v.text_content().strip() for v in row.xpath('./div[@role="cell"]')]
                    if len(cells) < 7: continue
                    name, dob, apps = cells[2], cells[3], cells[5]
                    if not name or not re.match(r"\d\d\.\d\d\.\d{4}", dob): continue
                    facts.append(dict(club_id=x["club_id"], player=name, identity_key=norm(name),
                                      position=pos, dob=dob, appearances=apps if apps.isdigit() else "0",
                                      source_url=x["source_url"]))
            if len(facts) == count_before: print("EMPTY ROSTER", x["club_id"], x["source_url"])
        except Exception as ex:
            print("ROSTER ERROR", x["club_id"], ex)
    # EuroFotbal lacks a complete 2010 table for these two clubs. Their
    # first-choice keepers are independently documented by the clubs/UEFA.
    for club, name, dob, source in [
        ("eu_ireland_shamrock-rovers", "Alan Mannus", "19.05.1982",
         "https://www.shamrockrovers.ie/news/alan-mannus-retirement/"),
        ("eu_romania_fc-timisoara", "Costel Pantilimon", "01.02.1987",
         "https://www.uefa.com/uefachampionsleague/news/025a-0ea66cdeff68-f71fb5433f56-1000--city-sign-romania-goalkeeper-pantilimon/"),
    ]:
        if not any(r["club_id"] == club and r["identity_key"] == norm(name) for r in facts):
            facts.append(dict(club_id=club, player=name, identity_key=norm(name),
                              position="GK", dob=dob, appearances="0", source_url=source))
    write(args.facts, ["club_id", "player", "identity_key", "position", "dob", "appearances", "source_url"], facts)
    print("FACTS", len(facts), "clubs", len({x["club_id"] for x in facts}))


if __name__ == "__main__": main()
