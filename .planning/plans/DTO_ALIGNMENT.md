# DTO Alignment Plan — favorites `WagerDto` ↔ front-end `WagerDTO`

Source prompt: `ai-prompts/matching-dtos.prompt.md`
Supporting analysis: `/java/czh/czh-gis-integrated/.requirements/integrations/favorites-integration.md`

## 1. Analysis of the prompt

**Goal:** favorites keeps its *own* DTO classes (no shared module) but the wager structure
must be identical to the front end (`gisorch`, which mirrors legacy `gis`), so a legacy
JSON body round-trips unchanged. Favorites may add its own extras (e.g. favorite name).

**Reference locations**
- gis (legacy): `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto`
- gisorch (front end): `references/czh-gisorch/czh-gis-api/src/main/java/cz/bsl/gis/api/drawgames`

**Current state (favorites-api)**
- `WagerDto` is a compact 6-field class (`gameName, stake, price, duration, serialNumber, boards: List<List<Integer>>`) with `ignoreUnknown=true`. It silently drops `type`, `drawNames`, `addonWagers`, `primarySelections`, etc.
- `FavoriteWagerDto`: `Long id`, `wagerName`, `groupNumber`, `gameName`, `playerId`, `flags`, `createdAt`, `wager`. Front end uses `String id`, `name`, `group{id,name}`, `wager`.
- `FavoriteGroupDto` / page DTOs diverge (`groupName`, `items/totalCount` vs `name`, `favoriteWagers`/`favoriteWagerGroups`).
- Consumers of `WagerDto`: `FavoritesConverter` (JSON <-> `WAGER_JSON`), `FavoritesValidator` (`getGameName`), `DefaultFavoritesService`, `FavoritesController`, `AdminFavoritesController`, DAO record, plus tests in api and app modules.

**Dependent types of `WagerDTO` (transitive closure, gisorch)**
`WagerDTO`, `BoardDTO`, `StakeDTO`, `WagerType`, `ErrorDTO`, `FieldErrorDTO`, `AddressDTO`, `IdDocumentDTO`, `PrizeDTO`, `PrizeStatus`, `PrizeType`, `PromotionDTO`, `TaxDTO`, `TextDTO`, `TicketSecurityDTO`.
Favorite-level: `FavoriteWagerDTO`, `FavoriteWagerGroupDTO`, `FavoriteWagerPageDTO`, `FavoriteWagerGroupPageDTO`.

**Observations / risks**
1. **Doc conflict:** the integration doc says "use the same shared `WagerDTO`"; the prompt says *no shared DTOs*. The prompt (human, newer) wins: copy the classes into `favorites-api`, same JSON shape, own package.
2. gis and gisorch copies differ in some files (diff reports differences, e.g. `AddressDTO`, `BetTypeDTO`) and gisorch has extra types (`CancelStatus`/`PrizeStatus` etc.). Need a per-file diff of the closure above to decide the baseline: **gisorch = target** (it is the actual front end); gis = cross-check.
3. `WagerDTO` uses `@JsonAutoDetect(fieldVisibility=ANY, getterVisibility=NONE)`; copies must keep it (and JSON names) to stay wire-compatible.
4. `WagerDTO` carries server/lifecycle fields (`id, status, requestId, digest, transactionTime, signature, prizes, ...`). A favorite must tolerate them on input and not persist/return them (see Phase 4).
5. Stored `WAGER_JSON` format changes (`boards` int-stacks -> `BoardDTO` objects). No migration (decided).
6. `gameName` is a promoted DB column; derive from `wager.gameName`.
7. Wire-level differences of `FavoriteWagerDto` (id type, `group` object, `name`, 200 vs 201, page wrapper names) are in the integration doc §1–2. They are the same alignment problem; included here as Phase 6 but may be split out.

## 2. Decisions (confirmed)
- No migration of stored `WAGER_JSON`; no backward-compatible reading of the old compact format.
- No input aliases (`wagerName` is dropped in favour of `name`).
- Favorites-only output attributes (`playerId`, `flags`, `createdAt`, `totalCount`, ...) may remain; the front end ignores them. They must stay optional on input.
- Still open: is legacy group `id` the same as `groupNumber`, and does create without a number auto-assign one?

## 3. Work plan

Tests first in each phase; use the module-local JUnit test, then integrate (`./mvn.sh install`).

### Phase 0 — Baseline diff (read-only)
- [ ] Per-file `diff` gis vs gisorch for the closure above; record deltas in a table (field, type, annotation).
- [ ] Pick gisorch as source of truth; list any gis-only fields to add additively.
- [ ] Capture legacy fixtures (`references/gis/tools/tests/rest-tests/.../favorites/sportka.json`, `group/sportkagroup.json`) into `czh-favorites-api/src/test/resources/` as golden JSON.

### Phase 1 — Golden round-trip tests (red)
- [ ] `WagerDtoTest`: deserialize `sportka.json` wager -> serialize -> JSON-equal to input (incl. `addonWagers`, `boards[].primarySelections`, `drawNames`, `type`).
- [ ] `FavoriteWagerDtoTest`: full legacy body (`name`, `group{id,name}`, `wager`) round-trips.
- [ ] Test that unknown/server-assigned wager fields are accepted.

### Phase 2 — Copy wager DTO closure into `favorites-api`
- [ ] Package `cz.bsl.czh.favorites.api` (consistent with existing `*Dto` naming; keep `Dto` casing e.g. `WagerDto`, `BoardDto`, `StakeDto`, `WagerType`, ...). Keep JSON property names and `@JsonAutoDetect` identical to gisorch.
- [ ] Replace compact `WagerDto` with full structure: `BoardDto`, `StakeDto`, `TaxDto`, `TextDto`, `AddressDto`, `IdDocumentDto`, `PrizeDto`, `PromotionDto`, `TicketSecurityDto`, `ErrorDto`, `FieldErrorDto`, enums.
- [ ] Javadoc header: "Mirrors front end `WagerDTO` (gisorch); not shared by design — keep in sync" + `Grep anchor: favorites`.
- [ ] Phase 1 wager tests green.

### Phase 3 — Favorite-level DTOs
- [ ] `FavoriteWagerDto`: `String id`, `name`, `group` (`FavoriteWagerGroupDto{id,name}`), `wager`; keep favorites extras (`playerId`, `flags`, `createdAt`) as additive, optional.
- [ ] `FavoriteGroupDto` -> `id`, `name` (+ allowed extras).
- [ ] Page DTOs: `favoriteWagers` / `favoriteWagerGroups` + `nextPageUrl, previousPageUrl, pageUrls, nextItems, previousItems`; keep `totalCount`.
- [ ] Update api tests (`FavoriteWagerDtoTest`, `FavoritePageDtoTest`).

### Phase 4 — Service layer
- [ ] `FavoritesConverter`: serialize/deserialize full `WagerDto`; strip server-assigned fields (`id, status, requestId, digest, transactionTime, signature, prizes, ...`) before storing; map `group.id` <-> `groupNumber`, `name` <-> `WAGER_NAME`, `id` Long <-> String.
- [ ] `FavoritesValidator`: use `wager.getGameName()`, validate `boards` as `BoardDto` (non-empty selections), keep size limit `MAX_WAGER_JSON_BYTES`.
- [ ] `DefaultFavoritesService`, DAO record: derive `gameName` from wager.
- [ ] Update unit tests: `FavoritesConverterTest`, `FavoritesValidatorTest`, `DefaultFavoritesServiceTest`, `FavoritesWagerDaoTest`.

### Phase 5 — Persistence
- [ ] No migration or old-format compat (decided); note the breaking `WAGER_JSON` format in release notes.
- [ ] Check `czh-favorites-db` schema: `WAGER_JSON` column size vs larger payload; adjust if needed.

### Phase 6 — Controllers & wire behaviour (from integration doc)
- [ ] `FavoritesController` / `AdminFavoritesController`: adopt new DTOs; POST returns 200 (not 201); 404 + standard error body for unknown ids.
- [ ] Controller tests + jamcrest request/response tests (`test/`, `js/`) updated with legacy fixtures.

### Phase 7 — Verification
- [ ] `./mvn.sh install` (all modules green).
- [ ] Jamcrest integration run against local service: POST legacy `sportka.json` -> 200 string `id` -> GET equals input -> PUT rename -> DELETE 204 -> GET 404.
- [ ] Run `tests/favorites/favorites-smoke.sh` in czh-gis-integrated when available; tighten fixtures to assert `name`, `group.name`, `wager.gameName`.
- [ ] Update docs/README/routing map; add a "DTO drift check" note (script or test comparing field names with `references/czh-gisorch`) so divergence is detected.

## 4. Done criteria
- Legacy front-end JSON posts and reads back identically (plus `id`).
- No dependency on gis/gisorch artifacts; DTOs are local copies.
- Favorites-only attributes are additive and optional.
- All unit and integration tests pass.
