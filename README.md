# PFM 2011 JAR-to-APK adaptation

Android adaptation of the Play Football Manager 2011 Alpha 0.84 J2ME core.

## Current build

`v41-audited-roster-expansion` continues from the v40 code on the historical
`v28-build` line.  It keeps v39's match-statistics changes and adds:

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
