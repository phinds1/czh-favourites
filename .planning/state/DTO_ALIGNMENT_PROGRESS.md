# DTO Alignment — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Phase 0 - Baseline diff (read-only) | complete |
| 2 | Phase 1 - Golden round-trip tests (red) | complete |
| 3 | Phase 2 - Copy wager DTO closure into favorites-api | complete |
| 4 | Phase 3 - Favorite-level DTOs | complete |
| 5 | Phase 4 - Service layer | complete |
| 6 | Phase 5 - Persistence | complete |
| 7 | Phase 6 - Controllers and wire behaviour | complete |
| 8 | Phase 7 - Verification | complete |

## Review notes

### Phase 1 — Phase 0 - Baseline diff (read-only)

⚠ Plan's 'still open' questions resolved from integration doc: legacy group id == groupNumber slot; create without number auto-assigns next free slot (max 10). Confirm.

⚠ gisorch is a strict superset of gis; only additive deltas ErrorDTO.uid, FieldErrorDTO.fieldPath/message. No gis-only fields to port.

Files for review:
- `/java/czh/czh-favourites/.requirements/design/dto-alignment-baseline.md:1`
- `/java/czh/czh-favourites/czh-favorites-api/src/test/resources/legacy/sportka.json:1`

### Phase 2 — Phase 1 - Golden round-trip tests (red)

⚠ Red state confirmed as planned: 6 golden tests red (boards objects unbindable, id numeric, server fields dropped); 15 old compact tests still green. Green comes with phases 2-3.

⚠ Golden assertions canonicalize JSON trees via serialize+reparse because Jackson node equality is numeric-type-strict (IntNode != LongNode).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-api/src/test/java/cz/bsl/czh/favorites/api/WagerDtoTest.java:29`
- `/java/czh/czh-favourites/czh-favorites-api/src/test/java/cz/bsl/czh/favorites/api/FavoriteWagerDtoTest.java:28`
- `/java/czh/czh-favourites/czh-favorites-api/src/test/java/cz/bsl/czh/favorites/api/FixtureJson.java:1`

### Phase 3 — Phase 2 - Copy wager DTO closure into favorites-api

⚠ Favorites extensions added on top of gisorch copies: @JsonIgnoreProperties(ignoreUnknown=true) on input, @JsonInclude(NON_NULL) on output (legacy body must round-trip sparse). JSON property names and @JsonAutoDetect identical to gisorch.

⚠ Drift check (field sets vs gisorch) passes for all 15 files; will be promoted to a repo script in Phase 7.

⚠ App module (FavoritesConverter/Validator/Controller) does not compile against the new WagerDto yet - service layer is Phase 4. Only -pl czh-favorites-api is green right now.

⚠ Old compact-shape WagerDto tests rewritten to the full structure; 3 favorite-level golden tests intentionally still red (Phase 3).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/WagerDto.java:23`
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/BoardDto.java:15`
- `/java/czh/czh-favourites/czh-favorites-api/src/test/java/cz/bsl/czh/favorites/api/WagerDtoTest.java:25`

### Phase 4 — Phase 3 - Favorite-level DTOs

⚠ Favorite-level wire shape now mirrors gisorch: id/name are strings, group {id,name} where id = group slot number (surrogate DB key not exposed); wagerName/groupNumber/wager-level groupName dropped with NO input aliases (plan decision).

⚠ Favorites extras kept as additive/optional: playerId, gameName (derived from wager.gameName in service), flags, createdAt (+updatedAt on groups), totalCount on pages. Front end ignores them per integration doc.

⚠ Page DTOs use legacy names favoriteWagers/favoriteWagerGroups + paging fields (null=omitted while unpaged).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerDto.java:27`
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerGroupDto.java:1`
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteGroupDto.java:1`

### Phase 5 — Phase 4 - Service layer

⚠ Server-assigned strip list (30 fields, plan said id/status/requestId/digest/transactionTime/signature/prizes/...): my judgment filled the ... with placement-outcome and response-only fields (freeTicket(s), revision, taxes, discounts, promotion, ticketMessage, security, etc). serialNumber KEPT (legacy stored it as placeReference). Review the list in FavoritesConverter.stripServerAssignedFields.

⚠ Group resolution diverges from legacy gis in one point: name-only groups resolve by NAME first (legacy always auto-created a new group); integration doc explicitly offered resolve-by-name. Update keeps legacy parity: unknown id or name mismatch -> 404.

⚠ Auto-created groups enforce the maxGroupIndex quota (legacy parity); no-group wager body creates the default group (minGroupIndex + defaultGroupName).

⚠ Service+DAO unit tests green (100); controller/jamcrest tests NOT yet updated - Phase 6. App needs -am (api jar installed).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesConverter.java:107`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/DefaultFavoritesService.java:262`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java:53`

### Phase 6 — Phase 5 - Persistence

⚠ No WAGER_JSON migration or old-format reading (plan decision). Breaking-change release-note text written in the design doc section 5 - include it in the GitHub release description.

⚠ No schema change: WAGER_JSON VARCHAR(30000) == MAX_WAGER_JSON_BYTES. Note: DB2 counts bytes vs Java chars for non-ASCII content (review point, not fixed).

Files for review:
- `/java/czh/czh-favourites/.requirements/design/dto-alignment-baseline.md:69`

### Phase 7 — Phase 6 - Controllers and wire behaviour

⚠ POST wagers and POST groups now return 200 with body (legacy contract); Location header dropped (was 201+Location; US_SPELLING had flagged this veto - now applied per plan).

⚠ Semantics flip: wager create no longer requires an existing group (auto-creates by id or name), group create without id auto-assigns a slot - validation tests flipped accordingly (legacy name-only bodies now 200).

⚠ All jamcrest fixtures rewritten to legacy wire shape (name, group{id,name}, favoriteWagers/favoriteWagerGroups); Vision GUI player-lookup.js and vision-test.sh seeds updated to match.

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java:59`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/resources/favorites/wager/create.req.js:1`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js:78`

### Phase 8 — Phase 7 - Verification

⚠ Verification: ./mvn.sh install green (189 tests); vision-test.sh 21/21; drift check ok (15 files). Integrated smoke fixtures in czh-gis-integrated tightened to assert name/group.name/wager.gameName - the live-stack run needs the gateway host (czhrigisrhel9), not available in this sandbox: run tests/favorites/favorites-smoke.sh when the stack is up.

⚠ Also fixed pre-existing bug: dao-test-schema.sql identities now START WITH 1 (HSQLDB was generating id 0, which validateId rejects; matches DB2 semantics). US_SPELLING had documented this as known flakiness.

⚠ Legacy round-trip is a permanent test (LegacySportkaRoundTripTest): POST sportka.json -> 200 string id -> GET wager equals input -> PUT rename -> DELETE -> 404.

Files for review:
- `/java/czh/czh-favourites/bin/dto-drift-check.py:1`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favorites/web/LegacySportkaRoundTripTest.java:1`
- `/java/czh/czh-favourites/.requirements/design/routing.md:13`
