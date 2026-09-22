# European native player import v74

The v73 research document contained 2,321 season-wide name entries for 129 clubs. The v74 importer produces 2,246 single-owner native player records and preserves the original draft separately for review.

- 1,082 imported ratings and primary positions have an exact normalized player-name match at a matched club in the [FIFA 11 FUT CSV](https://github.com/kafagy/fifa-FUT-Data/blob/master/FIFA11.csv). That CSV is a game-data reference, not proof of an opening-day 2010 roster.
- 1,164 ratings and positions are deterministic gameplay estimates. Their provenance is `ESTIMATED` in the native career and market UI.
- All 2,246 ages, values, wages, contracts, and unknown nationalities are model inputs, not verified historical facts. Ages and finances are labelled as estimated; nationality is stored as `Unknown` rather than inferred from the club's country.
- 75 supplied entries are held out: 15 alternate-club rows resolved by an exact FIFA club match, 33 unresolved cross-club rows, 26 name collisions with the existing top-five database, and one known date conflict (Derek Boateng at Dnipro).
- The supplied lists omit goalkeepers at 40 clubs. No fictional named goalkeepers were inserted. These clubs can acquire them in the market, but external-club match simulation is outside this stage.
- Three clubs with an `active_from` of 2011 retain a dormant roster in 2010, but are hidden from the market until the 2011 season.

Native schema 10 adds the 129 clubs and 2,246 players to existing schema-9 careers without modifying the previous top-five clubs, players, transfers or finances. The prior snapshot remains the atomic backup. The legacy match engine remains top-five-only; a newly bought external player uses the existing native-to-legacy user-squad adapter.

Rebuild input: `tools/build_v74_external_world.py`, the v72 club registry, the v73 source draft, `overrides/nationalities_v42.tsv`, and the FIFA 11 FUT CSV linked above. The downloaded third-party CSV is not vendored into this repository; only the derived, provenance-labelled asset is included.
