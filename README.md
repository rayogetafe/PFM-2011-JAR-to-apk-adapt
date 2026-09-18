# PFM 2011 JAR-to-APK adaptation

Android adaptation of the Play Football Manager 2011 Alpha 0.84 J2ME core.

## Current build

`v57-visible-ask-contract-entry` continues from v56 on the historical
`v28-build` line.  It keeps v39's match-statistics changes and adds:

- the seller decision and final transaction now use the same £0.1m-rounded
  asking price shown to the user, removing invisible rounding counteroffers;
- a provisionally accepted bid permanently changes the player-profile action
  from Prepare bid to Contract;
- accepted rows in Negotiations are tappable and reopen the contract stage,
  so closing the original response dialog no longer loses the next action;

- accepted bids proceed to a 1–5 year player-contract stage with editable
  annual wage and explicit player/wage-budget validation;
- one atomic transaction changes buyer and seller balances, player ownership,
  contract terms and the permanent deal journal only after a full audit;
- native rosters accept up to 30 players and never allow a seller below 18;
- stable player identities survive club changes, and persisted ownership is
  reapplied to the native league and market views after reopening the career;
- v56 deliberately does not inject the purchase into the legacy matchday XI;
  that two-way adapter is the next migration stage;

- automatic, validated schema migration from native career v1 to v2 while
  retaining the v1 file as the rollback backup;
- one contract for every player, containing annual wage and expiry season;
- transfer balance and annual wage budget for every club, with the user's
  opening balance imported from the live legacy career;
- integrity checks now reject invalid finance, expired-on-import contracts,
  missing ownership and duplicate entities before activating the new file;

- a single native career schema for clubs, players, ownership, nationality,
  age, ability, value and wage, with stable IDs for all 98 clubs and 2,281
  players;
- an audited first-import boundary per career slot, an atomic temporary-file
  write, validation before activation and recovery from the previous backup;
- a visible Native career data diagnostic reporting schema version, source
  season, active league, user club, entity totals and ownership integrity;
- v54 remains read-only toward the legacy save: the next migration can add
  contracts and atomic native transfers without silently corrupting careers;

- custom transfer-fee entry with budget and input validation;
- seller responses distinguish a low-offer rejection, counteroffer and
  provisional acceptance while retaining the audited asking-price model;
- negotiations persist per career slot and season and are available from a
  dedicated market list; this stage intentionally makes no ownership or
  budget mutation before contracts and atomic save migration are ready;

- prospect protection adds a controlled 8-20 percentage points of seller
  resistance for high-upside players aged 22 or under, preventing cases such
  as 20-year-old elite players being offered at almost bare database value;
- position-aware hard blocks now cover only exceptional replacement risks:
  the audited goalkeeper rate falls from 26/257 (10.1%) to 6/257 (2.3%);
- Prepare bid opens a non-mutating negotiation preview with opening offer,
  seller estimate, wage basis and remaining budget, ready for the later
  persistent transfer transaction layer;

- an all-player 2010/11 age audit using FIFA 11 team data, an appearance/DOB
  cross-check and the independently audited v41 additions;
- balanced database values and wage bases for all 2,281 players, derived from
  overall, smooth age/position curves and a tightly capped reputation input;
- capped seller resistance (maximum 1.75x) that separates player importance
  from replacement difficulty instead of multiplying stacked premiums;
- separate Affordable and Available filters, explicit Not for sale status,
  and the persistent shortlist from v50;
- the Global Player Market now includes the active league as well as the four
  parallel leagues, making it the intended future transfer centre;

- one shared matchday selection object now drives both the score calculation
  and the player ledger; v48 incorrectly rotated statistics after calculating
  the result from a static XI;
- hierarchical appearances: first-team quality has substantially more weight,
  planned rest is occasional, and only 1-3 substitutes are used per fixture;
- the 98-club audit rejects the former flat distribution and currently yields
  appearance percentiles of 8 / 22 / 33 (10th / median / 90th);
- rotated or weakened lineups now alter match strength before each of the
  stock-shaped scoring rolls, so season tables respond to actual selections;
- every new career receives a random persisted world seed: repeated viewing
  keeps already-generated results stable, while a new run produces genuinely
  different fixtures, scores and final tables;

- position-balanced parallel-league selection (1 GK, 4 DEF, 4 MID, 2 FW),
  with strength, resistance, accumulated fatigue and controlled variation;
- three matchday substitutes and recovery for unused players, replacing the
  unrealistic permanent `38 appearances / 0 appearances` split;
- separate starts and appearances in world statistics and player profiles;
- a 98-club full-season rotation audit requiring at least 18 used players and
  preventing any outfield selection from starting every match;

- a read-only Global Player Market covering every player in all five leagues,
  with player/club/nationality search and position filters;
- sorting by database value, overall, age or name, plus detailed value, wage
  basis and current-budget affordability in each player profile;
- original database financial fields in the world sidecar, establishing a
  single auditable economic input before cross-league bids and AI transfers;
- cross-league transactions deliberately remain disabled until persistent
  ownership, replacement recruitment and save migration are implemented;

- an isolated port of the original `bb` quick-match strength and scoring
  formula for the four parallel leagues, including XI attributes and morale;
- the stock fourteen attacking checks per match without touching the active
  career's global points, morale, fixtures, or player-stat arrays;
- appearance-safe scorers and assist providers, so a player can no longer
  record a goal or assist while showing zero appearances;
- fresh table rows from the list adapter, fixing missing ranks 18/19 in
  completed-season history after a new season starts;
- active-league detection cross-checked against the actual loaded legacy
  table, preventing that competition from being duplicated in this screen;
- stock formation/style attack shares replace v45's incorrect fixed 50/50
  assumption, with league opportunity tempo calibrated before every roll;
- ten-season per-league scoring guards target the observed 2010/11 environment
  (including Ligue 1's materially lower scoring rate), without post-processing
  match scores or weakening team-strength differences;

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

## Native-to-matchday roster bridge (v58)

Completed native transfers into the user's club are now reconciled into the
still-active legacy matchday roster. The bridge preserves the imported player's
SPE/RES/QUA, morale, value, wage and ownership, extends player-indexed sidecars,
repairs lineup invariants and records a stable native-to-legacy id mapping so
opening the screen or restarting cannot duplicate a player. A full 25-player
legacy roster is reported as pending instead of being overwritten; the native
career remains the source of truth while this transition limit exists.

v59 additionally reconciles that source of truth whenever the squad or
matchday bridge is opened after a stock `GAME LOADED` event. It verifies the
actual player's name, age, position and attributes instead of trusting a stale
numeric id, fixing imported players disappearing when the legacy save restores
its older 24-player roster.

## Self-contained native player state (v60)

Career schema v4 stores SPE, RES, QUA and morale for all 2,281 players alongside
their stable identity, ownership, contract and value. Existing schema-v3 saves
are upgraded atomically from the audited world dataset while retaining every
completed deal and club balance. Market views and the matchday roster adapter
now consume the career copy, making `world_v42` an import source rather than a
runtime authority for transferred-player attributes.

## Native Contract Center (v61)

The user's complete native squad now has a dedicated contract view, sorted by
expiry, with annual wage and contract end shown for every player. Renewals are
written through the same validated atomic career transaction as transfers:
length is restricted to 1–5 years, players reject cuts below 90% of their
current contract and the club cannot exceed its native annual wage budget.

## Native outgoing transfers (v62)

The Transfer Out Center produces up to three affordable offers from clubs in
the other top-five leagues. Accepting an offer atomically changes ownership,
both club balances and the native deal journal, while enforcing 18–30 player
roster limits. The two-way legacy adapter removes the sold player from the live
matchday squad and repeats that reconciliation after a stock save is loaded,
so the old roster cannot silently restore a completed outgoing transfer.

## Native transfer activity (v63)

The global market now exposes its persistent deal journal as Transfer Activity.
Each entry records direction, player, seller, buyer, season, fee, annual wage
and contract length. The view can switch between the user's transactions and
all world activity, providing the UI and audit boundary needed before AI clubs
begin trading with one another.

## AI world transfer market (v64)

Career schema v5 runs a deterministic AI transfer batch once per half-season
window and records the processed window atomically, preventing duplicate deals
when screens are reopened. Up to eight cross-league moves target a buyer's weak
position while protecting each seller's key players and positional depth,
enforcing 18–30 player squads, transfer funds and wage budgets. The user's club
and every club in the active legacy league are excluded until their matchday
rosters are fully native; all completed AI moves appear in Transfer Activity.

## Shared transfer windows (v65)

User purchases, outgoing sales and AI-to-AI deals now use the same league-round
calendar: the summer window runs through round 3 and the winter window spans
five rounds around the season midpoint. Search, shortlist and preliminary bids
remain available while closed, but ownership and finance changes are blocked.
Installing an update late in a season no longer triggers a catch-up AI batch;
existing v64 window markers are recognized to avoid duplicate activity.

## Atomic native season rollover (v66)

Career schema v6 follows the legacy season counter with one validated atomic
transaction. Each elapsed season ages every native player by one, resets morale
to 55 and reopens the AI transfer-window marker for the new campaign. AI clubs
renew expired contracts deterministically; expired user contracts remain on a
safe grace status and are highlighted as `RENEW NOW` in Contract Center until
the manager renews them. Existing ownership, finances and transfer history are
preserved throughout the rollover.

## Native career isolation (v67)

Native state now carries a persisted per-run identity in addition to the legacy
save-slot number. Starting a new career in a reused slot rebuilds the world and
creates a clean ownership, finance and transfer journal instead of inheriting
the previous club's snapshot. Before replacement, only players previously
injected by the native transfer bridge are removed from the live legacy squad;
stock players remain untouched. Club and league identity checks also repair
v66 snapshots that already crossed from one career into another.

## Full per-career persistence isolation (v68)

The career nonce now scopes every native user-facing persistent dataset:
negotiations, shortlist entries, match reports and goalkeeper ledgers can no
longer reappear in another career that reuses the same slot and season. A fresh
career starts with an empty native deal journal. Its AI market is armed only
after the first completed round, rather than generating a batch merely because
the market screen was opened, and AI selection includes the career nonce so
separate careers no longer produce an identical transfer package.

## Unified native transfer market (v69)

The original autonomous `bb.a()`/poach market is disabled: it can no longer
move a legacy player or write mail without a matching native transaction. The
same matchday hook now runs the native AI market, and only an atomically
committed deal posts the familiar `BIG:` or `CHANGES THE TEAM` message into the
stock inbox. The Settings slider controls a per-window cap of 0, 2, 5, 8 or 12
AI deals. Buyers target their weakest position; sellers protect positional
depth and younger first-team assets, while older surplus players need fewer
players ahead of them. Fees now reflect age and replacement value, wages can
rise on transfer, and contract lengths vary from two to four years by age.

## Calendar-paced world market (v70)

AI deals are no longer generated as one batch. Summer business begins during
the post-season transition and continues through rounds 0–3; winter business is
released in five portions around the midpoint. Reopening a native screen cannot
repeat a portion. Buyer ordering balances activity between the four parallel
native leagues and limits repeated buying or selling by one club, while club
strength matching produces appropriate mid-table and lower-table recruitment
instead of always chasing the highest-rated affordable player. A two-season
player cooldown prevents implausible annual ping-pong transfers. The original
0/2/5/8/12 settings remain and the slider adds 25, 50, 80 and 120 deals per
window for broader world-market simulations.

## Badge audit

[`overrides/badges_v40_manifest.tsv`](overrides/badges_v40_manifest.tsv) is the
authoritative resource-to-club mapping. The separately supplied Chievo image is
mapped to `it14`; Atalanta remains unused because the club was in Serie B in
2010/11.
