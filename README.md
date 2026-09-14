# PFM 2011 JAR-to-APK adaptation

Android adaptation of the Play Football Manager 2011 Alpha 0.84 J2ME core.

## Current build

`v40-exact-badges-roster-depth` continues from the v39 code on the historical
`v28-build` line.  It keeps v39's match-statistics changes and adds:

- an explicit, auditable mapping from the league database order to all 98 club
  badge slots, replacing v39's unreliable visual-similarity assignment;
- the supplied badge set in both native Android screens and the legacy J2ME
  renderer;
- a first position-depth pass which reclassifies 36 real 2010/11 wide or second
  forwards as `FW` without changing ratings, ownership, prices, or wages;
- roster audit thresholds of 2 GK / 5 DEF / 5 MID / 4 FW.

The source database and transfer code already allocate up to 25 players per
club.  Initial league data still contains exactly 20 players per club, so adding
historically verified players 21–25 is intentionally a separate data phase.

## Badge audit

[`overrides/badges_v40_manifest.tsv`](overrides/badges_v40_manifest.tsv) is the
authoritative resource-to-club mapping.  The supplied Italy archive contains
Atalanta (Serie B in 2010/11) but does not contain Serie A participant Chievo;
v40 therefore keeps the bundled Chievo placeholder rather than assigning the
wrong club's crest.
