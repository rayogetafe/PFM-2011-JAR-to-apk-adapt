# PFM 2011 JAR-to-APK adaptation

Android adaptation of the Play Football Manager 2011 Alpha 0.84 J2ME core.

## Current build

`v44-world-engine-parity` continues from v43 on the historical
`v28-build` line.  It keeps v39's match-statistics changes and adds:

- a fourteen-segment parallel match model shaped after the stock fast-match
  engine, using the ordered starting XI instead of whole-squad averages;
- an automated 10-season guard requiring 2.45-3.35 total goals per match
  (current five-league range: 2.92-3.04);
- only the four non-active top-five leagues in World Competitions;
- fixed-width two-digit ranks and numeric columns plus a visible zone legend;

- a structured World Competition Center with the same compact table columns,
  badge cells and qualification/relegation zones as the active league view;
- a round selector covering every matchday of the current parallel season;
- separate league leaderboards and club/squad/player-profile navigation;
- reconstructable final-table history for completed parallel seasons without
  retaining obsolete match-by-match archives;

- deterministic parallel 2010/11 seasons for the other top-five leagues,
  synchronized to the active career calendar;
- World Leagues tables, results and player scoring lists;
- an audited nationality sidecar for all 2,281 club-owned players (2,179 FIFA
  11 matches plus 102 manual identity checks) without changing legacy saves;
- the stock engine and exact League Center for the active competition, avoiding
  conflicting duplicate simulation;

- an explicit, auditable mapping from the league database order to all 98 club
  badge slots, replacing v39's unreliable visual-similarity assignment;
- the complete supplied badge set, including Chievo, in both native Android
  screens and the legacy J2ME renderer;
- a first position-depth pass which reclassifies 36 real 2010/11 wide or second
  forwards as `FW` without changing ratings, ownership, prices, or wages;
- independently audited September 2010 squads of 21-25 players for all 98
  clubs, with 359 new records and five corrected same-league moves;
- 43 stale, duplicate, loaned-out, or post-snapshot records removed from club
  ownership, plus 22 rejected suggestions retained in the audit plan;
- a budget-neutral attribute rebalance: every adjusted club keeps exactly the
  same aggregate SPE / RES / QUA total across its pre-existing players.

The source database and transfer code already allocate up to 25 players per
club. v41 now uses that capacity while keeping original first-XI and bench order
stable. The full applied plan is recorded in
[`overrides/roster_v41_plan.json`](overrides/roster_v41_plan.json).

## Badge audit

[`overrides/badges_v40_manifest.tsv`](overrides/badges_v40_manifest.tsv) is the
authoritative resource-to-club mapping. The separately supplied Chievo image is
mapped to `it14`; Atalanta remains unused because the club was in Serie B in
2010/11.
