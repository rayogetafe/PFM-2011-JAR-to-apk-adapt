#!/usr/bin/env python3
"""Build an audited 2010/11 age/value override table from the European Soccer DB."""
from __future__ import annotations

import argparse, csv, datetime as dt, difflib, json, re, sqlite3, unicodedata
from collections import Counter, defaultdict
from pathlib import Path


COUNTRIES = {"en":"England", "es":"Spain", "it":"Italy", "de":"Germany", "fr":"France"}
ALIASES = {
    "man united":"manchester united", "man city":"manchester city", "tottenham":"tottenham hotspur",
    "west brom":"west bromwich albion", "wolves":"wolverhampton wanderers", "newcastle":"newcastle united",
    "west ham":"west ham united", "stoke city":"stoke city", "blackpool":"blackpool",
    "ath bilbao":"athletic club de bilbao", "atl madrid":"club atletico de madrid sad",
    "r sociedad":"real sociedad", "sporting":"sporting de gijon", "hercules":"hercules club de futbol",
    "bayern":"fc bayern munich", "mgladbach":"borussia monchengladbach", "koln":"1 fc koln",
    "nurnberg":"1 fc nurnberg", "frankfurt":"eintracht frankfurt", "klautern":"1 fc kaiserslautern",
    "mainz 05":"1 fsv mainz 05", "st pauli":"fc st pauli", "inter":"inter",
    "milan":"ac milan", "roma":"as roma", "lazio":"lazio", "st etienne":"as saint etienne",
    "paris sg":"paris saint germain fc", "arles":"ac arles avignon",
    "caen":"sm caen", "lens":"rc lens", "rennes":"stade rennais fc", "lille":"losc lille metropole",
    "lyon":"olympique lyonnais", "brest":"stade brestois 29", "bordeaux":"fc girondins de bordeaux",
    "lorient":"fc lorient bretagne sud", "marseille":"olympique de marseille", "nancy":"as nancy lorraine",
    "sochaux":"fc sochaux montbeliard", "racing":"racing de santander", "hercules":"hercules cf",
    "bolton":"bolton wanderers", "wigan":"wigan athletic", "blackburn":"blackburn rovers",
    "dortmund":"borussia dortmund", "hamburg":"hamburger sv", "klautern":"1 fc kaiserslautern",
    "leverkusen":"bayer 04 leverkusen", "werder":"werder bremen",
}


def norm(value: str) -> str:
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode().lower()
    return " ".join(re.findall(r"[a-z0-9]+", value))


def age_on(serial: str) -> int:
    # EA's legacy DB serial uses the Gregorian-calendar epoch.
    born = dt.date(1582, 10, 15) + dt.timedelta(days=int(serial)); snap = dt.date(2010, 9, 1)
    return snap.year-born.year-((snap.month, snap.day)<(born.month, born.day))

def age_iso(value: str) -> int:
    born=dt.date.fromisoformat(value[:10]); snap=dt.date(2010,9,1)
    return snap.year-born.year-((snap.month,snap.day)<(born.month,born.day))


def similarity(a: str, b: str) -> float:
    return difflib.SequenceMatcher(None, norm(a), norm(b)).ratio()


def name_score(short: str, full: str) -> float:
    s, f = norm(short), norm(full); st, ft = s.split(), f.split()
    if s == f: return 1.0
    if len(st) == 1 and st[0] in ft: return .96
    if len(st) >= 2 and len(st[0]) == 1 and ft[-1] == st[-1] and ft[0].startswith(st[0]): return .98
    if all(t in ft for t in st): return .94
    if st[-1] == ft[-1]: return .86
    return similarity(s, f) * .82


def market_value(ovr: int, age: int, pos: int) -> int:
    # Smooth, deterministic 2010/11 economy in pounds. OVR dominates; age and position refine it.
    anchors = [(55,250_000),(60,500_000),(65,1_000_000),(70,2_000_000),(75,4_000_000),
               (80,8_000_000),(85,16_000_000),(90,32_000_000),(95,56_000_000),(99,75_000_000)]
    for (lo,lv),(hi,hv) in zip(anchors,anchors[1:]):
        if ovr <= hi:
            base=lv+(hv-lv)*(ovr-lo)/(hi-lo); break
    else: base=anchors[-1][1]
    peak = 27 if pos == 0 else 25
    if age <= peak: factor = 1.0 + min(0.30, (peak-age)*0.025)
    else: factor = max(0.42 if pos == 0 else 0.25, 1.0-(age-peak)*(0.045 if pos == 0 else 0.065))
    pos_factor = (0.88, 0.94, 1.0, 1.04)[pos]
    return max(100_000, int(round(base*factor*pos_factor/100_000))*100_000)


def main() -> None:
    ap=argparse.ArgumentParser(); ap.add_argument("world",type=Path); ap.add_argument("qdb",type=Path); ap.add_argument("european",type=Path)
    ap.add_argument("plan",type=Path); ap.add_argument("output",type=Path); ap.add_argument("report",type=Path)
    a=ap.parse_args(); rows=list(csv.DictReader(a.world.open(encoding="utf-8"),delimiter="\t")); players=[r for r in rows if r["type"]=="P"]
    def read(name):
        return list(csv.DictReader((a.qdb/name).open(encoding="utf-16"),delimiter="\t"))
    qplayers=read("players.txt"); qteams=read("teams.txt"); links=read("teamplayerlinks.txt"); league_links=read("leagueteamlinks.txt")
    names={int(x["nameid"]):x["name"].strip() for x in read("playernames.txt")}; qvalues={int(x["playerid"]):int(x["playervalue"]) for x in read("playervalues.txt")}
    dbp={};
    for p in qplayers:
        pid=int(p["playerid"]); first=names.get(int(p["firstnameid"]),""); last=names.get(int(p["lastnameid"]),""); common=names.get(int(p["commonnameid"]),"")
        variants={" ".join((first,last)).strip(),last,common,names.get(int(p["playerjerseynameid"]),"")}-{""}
        dbp[pid]={"player_api_id":pid,"player_name":max(variants,key=len),"variants":variants,"birthday":p["birthdate"],"overall":int(p["overallrating"]),"value":qvalues.get(pid,0)}
    exact_names=defaultdict(list)
    for p in dbp.values():
        for variant in p["variants"]: exact_names[norm(variant)].append(p)
    league_ids={"en":13,"fr":16,"de":19,"it":31,"es":53}
    league_teams=defaultdict(set)
    for x in league_links: league_teams[int(x["leagueid"])].add(int(x["teamid"]))
    teams=[{"team_api_id":int(t["teamid"]),"team_long_name":t["teamname"]} for t in qteams]
    team_ids={}; team_matches={}
    for code,club in sorted({(r["league"],r["club"]) for r in players}):
        target=ALIASES.get(norm(club),norm(club)); ranked=sorted(((similarity(target,t["team_long_name"]),t) for t in teams),reverse=True,key=lambda x:x[0])
        ranked=[x for x in ranked if x[1]["team_api_id"] in league_teams[league_ids[code]]]
        team_ids[(code,club)]=ranked[0][1]["team_api_id"]; team_matches[code+":"+club]=(ranked[0][1]["team_long_name"],round(ranked[0][0],3))
    by_team=defaultdict(set)
    for x in links: by_team[int(x["teamid"])].add(int(x["playerid"]))
    econ=sqlite3.connect(a.european); econ.row_factory=sqlite3.Row
    ecountry={v:k for k,v in COUNTRIES.items()}; eteam={}; eplayers=defaultdict(list)
    for t in econ.execute("select distinct c.name country,t.team_api_id,t.team_long_name from Match m join Country c on c.id=m.country_id join Team t on t.team_api_id=m.home_team_api_id where m.season='2010/2011' and c.name in ('England','Spain','Italy','Germany','France')"):
        eteam[(ecountry[t["country"]],t["team_api_id"])]=t["team_long_name"]
    for code,club in sorted({(r["league"],r["club"]) for r in players}):
        target=ALIASES.get(norm(club),norm(club)); choices=[(similarity(target,n),tid) for (c,tid),n in eteam.items() if c==code]
        tid=max(choices)[1]
        for m in econ.execute("select * from Match where season='2010/2011' and (home_team_api_id=? or away_team_api_id=?)",(tid,tid)):
            side="home" if m["home_team_api_id"]==tid else "away"
            for n in range(1,12):
                pid=m[f"{side}_player_{n}"]
                if pid:
                    p=econ.execute("select player_name,birthday from Player where player_api_id=?",(pid,)).fetchone()
                    if p: eplayers[(code,club)].append(p)
    additions={}
    plan=json.loads(a.plan.read_text(encoding="utf-8"))
    for league in plan["leagues"].values():
        for club,data in league.items():
            for p in data["add"]: additions[(norm(data["database_team"]),norm(p["name"]))]=p["age"]
    out=[]; audit=[]; methods=Counter()
    for r in players:
        key=(norm(r["club"]),norm(r["player"])); candidates=[dbp[x] for x in by_team[team_ids[(r["league"],r["club"])]] if x in dbp]
        scored=sorted(((max(name_score(r["player"],v) for v in p["variants"]),p) for p in candidates),key=lambda x:(-x[0],abs(x[1]["overall"]-int(r["overall"]))))
        method="fifa11-club"; chosen=scored[0][1] if scored and scored[0][0]>=.62 else None
        if key in additions:
            age=additions[key]; method="audited-addition"
        elif chosen:
            age=age_on(chosen["birthday"])
        else:
            unique={p["player_api_id"]:p for p in exact_names[norm(r["player"])]}
            if len(unique)==1:
                chosen=next(iter(unique.values())); age=age_on(chosen["birthday"]); method="fifa11-unique"
            else:
                escored=sorted(((name_score(r["player"],p["player_name"]),p) for p in eplayers[(r["league"],r["club"])]),reverse=True,key=lambda x:x[0])
                if escored and escored[0][0]>=.62:
                    age=age_iso(escored[0][1]["birthday"]); method="appearance-db"
                else:
                    age=int(r["age"]); method="audited-retained"
        value=market_value(int(r["overall"]),age,int(r["position"]))
        if chosen and chosen["value"]>0:
            fifa_model=market_value(chosen["overall"],age,int(r["position"])); reputation=max(.85,min(1.15,chosen["value"]/max(1,fifa_model)))
            value=max(100_000,int(round(value*reputation/100_000))*100_000)
        wage=max(50_000,int(round(value*.075/50_000))*50_000)
        out.append({"league":r["league"],"club":r["club"],"playerIndex":r["playerIndex"],"player":r["player"],"age":age,"value":value,"wage":wage,"method":method})
        audit.append({**out[-1],"old_age":r["age"],"old_value":r.get("value",""),"source_player":chosen["player_name"] if chosen else ""})
        methods[method]+=1
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with a.output.open("w",encoding="utf-8",newline="") as f:
        w=csv.DictWriter(f,fieldnames=out[0].keys(),delimiter="\t");w.writeheader();w.writerows(out)
    a.report.write_text(json.dumps({"players":len(out),"methods":methods,"changed_ages":sum(x["old_age"]!=str(x["age"]) for x in audit),"team_matches":team_matches,"rows":audit},ensure_ascii=False,indent=2),encoding="utf-8")
    print(len(out),dict(methods),"age changes",sum(x["old_age"]!=str(x["age"]) for x in audit))


if __name__=="__main__": main()
