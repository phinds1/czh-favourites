# Gateway Path Contract — czh-favorites

Status: verified against `./references/gis/` only (2026-11-03)  
Grep anchor: favorites

This document is the **authoritative REST path contract** for the czh-favorites microservice,
for any agent or human wiring an external front-end (gateway/proxy) to it. It records:

1. the **public (gateway-facing) URL shapes** exactly as the legacy GIS implementation defined them,
2. the **backend servlet paths** the czh-favorites Spring service serves (target state after the
   US spelling cleanup plan `.planning/plans/US_SPELLING_PLAN.md`), and
3. the **mapping** between the two.

Scope note: paths, query parameters, headers, path parameters, version-segment semantics and
response codes only — verified strictly against the legacy GIS reference tree in this repo
(`references/gis/`). Nothing outside `references/gis/` was used as evidence.

---

## 1. Evidence — every path claim and its source

All paths below are backed by these files inside `references/gis/` (no other sources):

| #  | Evidence file                                                                                                              | What it proves                                                                                                                                                                                                   |               |
|----|----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| E1 | `components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java` | Player API: 10 JAX-RS endpoints under `/{version}/draw-games/favorite-wagers/...`, query params `group` + `game-names`                                                                                           |               |
| E2 | `components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java`                       | Admin API: 3 GET endpoints, class `@Path("/admin")` + `/{version}/player/{playerId}/draw-games/favorite-wagers/...`, query params `groupNumber` + `game-names`                                                   |               |
| E3 | `components/gis/gis-env-config/src/main/env-config/gis/esa-configuration-gis.xml.ftl`                                      | Legacy gateway routes: `/{context}/api/{version}/draw-games/favorite-wagers/{var:.*}` (player) and `/{context}/api/admin/{version}/player/{var:.*}/draw-games/favorite-wagers/{var:.*}` (admin)                  |               |
| E4 | `tools/tests/rest-tests/src/test/java/com/gtech/pd/sites/czh/gis/favorites/*IntegrationTest.java` (13 files)               | 130 URL occurrences, all called with context `dg`, version literal **`v1`**: `/dg/api/v1/draw-games/favorite-wagers/{wagers\                                                                                     | groups}[...]` |
| E5 | `tools/swagger/src/main/swagger/swagger-ui-player.json`                                                                    | Public player spec, servers base `/dg/api/v2`, methods + path params for all 10 player endpoints, header set                                                                                                     |               |
| E6 | `tools/swagger/src/main/swagger/swagger-ui-admin.json`                                                                     | Public admin spec, servers base `/dg/api/admin/v1`, wagers list + detail with query params `group` + `game-names`                                                                                                |               |
| E7 | `tools/api-doc/src/main/resources/asciidoc/player/Favorites.adoc`                                                          | Player docs: `/api/v2/draw-games/favorite-wagers/...`, `X-PLAYER-ID` header, optional query params `group` + `game-names`                                                                                        |               |
| E8 | `tools/api-doc/src/main/resources/asciidoc/admin/FavoritesAdmin.adoc`                                                      | Admin docs: `/api/admin/v1/player/{playerId}/draw-games/favorite-wagers/wagers[?...]` + `.../wagers/{id}`; optional query params; if omitted, all favourites returned                                            |               |
| E9 | `tools/tests/player-tests/src/test/conf/admin_rest_tests.xml`                                                              | A **different, older** admin surface (`admin-rest/api/v1/admin/players/{playerId}/draw-games/favorite-wagers/...`, plural `players`) — recorded only to warn that it is **not** this service's contract (see §7) |               |

## 2. Public (gateway-facing) URL contract — legacy GIS

The legacy gateway (E3) exposed draw-games resources under a context prefix. In practice the
context is `dg` and the version literal is `v1` (E4), the published player swagger says `v2`
(E5), the admin swagger says `v1` (E6). **The version segment is an opaque token chosen by the
caller**; the backend must never hardcode it (see §5).

### 2.1 Player API (player identity via `X-PLAYER-ID` header)

| Method | Public URL                                                 | Query params                                    |
|--------|------------------------------------------------------------|-------------------------------------------------|
| POST   | `/dg/api/{version}/draw-games/favorite-wagers/wagers`      | —                                               |
| GET    | `/dg/api/{version}/draw-games/favorite-wagers/wagers`      | `group` (optional), `game-names` (optional CSV) |
| GET    | `/dg/api/{version}/draw-games/favorite-wagers/wagers/{id}` | —                                               |
| PUT    | `/dg/api/{version}/draw-games/favorite-wagers/wagers/{id}` | —                                               |
| DELETE | `/dg/api/{version}/draw-games/favorite-wagers/wagers/{id}` | —                                               |
| POST   | `/dg/api/{version}/draw-games/favorite-wagers/groups`      | —                                               |
| GET    | `/dg/api/{version}/draw-games/favorite-wagers/groups`      | —                                               |
| GET    | `/dg/api/{version}/draw-games/favorite-wagers/groups/{id}` | —                                               |
| PUT    | `/dg/api/{version}/draw-games/favorite-wagers/groups/{id}` | —                                               |
| DELETE | `/dg/api/{version}/draw-games/favorite-wagers/groups/{id}` | —                                               |

`{id}` on the player group paths is the **group number** (E1: `getFavoriteWagerGroup(@PathParam("id"))`
→ `favG.getGroupNumber()`).

### 2.2 Admin API (read-only, `playerId` trusted path param)

| Method | Public URL                                                                                  | Query params                                                                         |
|--------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| GET    | `/dg/api/admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers`               | `groupNumber` (optional, E2) / `group` (E6+E8 — see §7), `game-names` (optional CSV) |
| GET    | `/dg/api/admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers/{wagerId}`     | —                                                                                    |
| GET    | `/dg/api/admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups/{groupNumber}` | —                                                                                    |

Note **singular `player`** in the admin path (E2, E3, E6, E8 all agree).

### 2.3 Auth headers (gateway-enforced in legacy, not backend logic)

- Player endpoints (E5/E7): `X-PLAYER-ID`, `X-SESSION-TOKEN`, `X-PLATFORM`, `X-BTMID`. Legacy
  gateway rule `requireAllHeaders` + `require_player_id` (E3). The backend only needs
  `X-PLAYER-ID` (HTTP headers are case-insensitive; the Spring service reads `X-Player-Id`).
- Admin endpoints (E6/E8): `X-SESSION-TOKEN`, `X-PLATFORM`, `X-BTMID`. Legacy gateway rule
  `requireOperatorHeaders` (E3). The backend takes `playerId` from the path as a trusted value;
  operator auth is the gateway's job, not the backend's (E2 calls `adminAuthentication.checkPermission`
  with gateway-provided context).

## 3. Backend servlet paths — czh-favorites target state

After `.planning/plans/US_SPELLING_PLAN.md` Phases 1–2 the Spring controllers serve
(`{version}` bound as an `@PathVariable`, accepted and ignored):

| Controller                 | Base mapping                                               | Endpoints                                                                                                                                                                                                                     |
|----------------------------|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `FavoritesController`      | `@RequestMapping("/{version}/draw-games/favorite-wagers")` | `/wagers`, `/wagers/{id}`, `/groups`, `/groups/{id}` — POST/GET/PUT/DELETE per §2.1                                                                                                                                           |
| `AdminFavoritesController` | `@RequestMapping("/admin")`                                | `@GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/wagers")` (+ optional `groupNumber`, `game-names` params), `.../wagers/{wagerId}`, `.../groups/{groupNumber}`, plus the retained `.../groups` list (§8) |

Current (pre-fix, wrong) paths for contrast: `/favorites/wagers`, `/favorites/groups`,
`/admin/favorites/players/{playerId}/wagers|groups` — these are being replaced; nothing external
should wire to them.

## 4. Mapping public → backend

| Public (gateway)                                             | Backend (servlet)                                                                      |
|--------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `/dg/api/{v}/draw-games/favorite-wagers/**`                  | `/{v}/draw-games/favorite-wagers/**` — strip `/dg/api`                                 |
| `/dg/api/admin/{v}/player/{p}/draw-games/favorite-wagers/**` | `/admin/{v}/player/{p}/draw-games/favorite-wagers/**` — strip `/dg/api`, keep `/admin` |

The legacy gateway proxied the full `/dg/api/...` path through to the draw-games backend (E3
`remote_proxy` URL includes `{context}/api/...`); the container then stripped its servlet root.
For czh-favorites the equivalent rule is: **the gateway strips exactly `/dg/api`; `/admin` stays
on the wire** so it reaches the `AdminFavoritesController` class mapping.

## 5. Version segment semantics

- The segment occupies the `{version}` position in both surfaces (E1, E2, E3).
- Callers in the field use `v1` (E4: 130/130 occurrences); the published player swagger used
  `v2` (E5); admin swagger `v1` (E6). Any non-empty single segment must therefore work.
- Backend contract: bind it, echo it in create `Location` headers, otherwise ignore it.
  **Never hardcode a version literal** in the backend or in gateway rewrites.

## 6. Response codes

| Operation                 | Legacy reference                                       | czh-favorites behaviour                                                   |                                                                                                |
|---------------------------|--------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| Create (POST wager/group) | E1 Javadoc: `@HTTP 200` added (no Location documented) | `201 Created` + `Location: /{version}/draw-games/favorite-wagers/{wagers\ | groups}/{id}` — kept from the existing service, flagged for human veto in the US spelling plan |
| Get/List/Update           | E1: `@HTTP 200`                                        | `200` with JSON body (page wrappers: `items` + `totalCount`)              |                                                                                                |
| Delete                    | E1: `@HTTP 204`                                        | `204 No Content`                                                          |                                                                                                |
| Validation failure        | —                                                      | `400` (`IllegalArgumentException` via `@ControllerAdvice`)                |                                                                                                |
| Not found / not owned     | E1: 401 for auth; 404 not specified                    | `404` (`NoSuchElementException`)                                          |                                                                                                |

## 7. Discrepancies inside the references (decisions recorded, do not silently change)

1. **Admin list query param name:** E2 (the Java resource the requirements prompt named as
   correct) uses `@QueryParam("groupNumber")`; E6/E8 (swagger + asciidoc) document `group`.
   **Decision:** follow E2 — `groupNumber` — because the prompt designates the Java files as the
   correct reference. Gateway agents must pass `groupNumber` for admin list filtering.
2. **Admin path singular vs plural:** E9 shows a *different, older* admin-rest surface with plural
   `players` (`admin-rest/api/v1/admin/players/{id}/draw-games/favorite-wagers/wagers/`). That is
   the legacy esa-este-gateway player-admin service, **not** this service. This service uses
   singular `player` per E2/E3/E6/E8. Do not wire the plural form.
3. **Player swagger omits the list query params:** E5 lists only headers for
   `GET /favorite-wagers/wagers`; E1 (JAX-RS) and E7 (adoc) both define `group` + `game-names`.
   **Decision:** they exist — E1 is the code contract.
4. **Version literal:** `v1` in all integration tests (E4) vs `v2` in the player swagger (E5).
   **Decision:** treat as opaque pass-through (§5); no rewrite either way.

## 8. Additions beyond the legacy reference (this service only)

| Endpoint                                                                                 | Why it exists                                                                                                                                                              |
|------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups` (list groups) | Not in E2/E6 — added for the service's operator GUI Player Lookup tab. Same shape as the other admin endpoints; document as an extension, not part of the legacy contract. |
| `GET /admin/.../wagers` with **no** query params returns all wagers                      | Matches E8 ("if parameters are omitted all player's favorites are returned") — kept explicit here because the GUI calls it bare.                                          |

## 9. Ports (from `.requirements/integrations/microservice-port-assignements`)

| Service                                             | Port |
|-----------------------------------------------------|------|
| HTTP (app — serves §2/§3 business paths)            | 9480 |
| Management (actuator/prometheus — not business API) | 9481 |

## 10. How to re-verify this contract

```sh
# Player interface (E1)
grep -n "@Path\|@GET\|@POST\|@PUT\|@DELETE\|@QueryParam" \
  references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java

# Admin resource (E2)
grep -n "@Path\|@GET\|@QueryParam" \
  references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java

# Legacy gateway routes (E3)
grep -n "favorite-wagers" \
  references/gis/components/gis/gis-env-config/src/main/env-config/gis/esa-configuration-gis.xml.ftl

# Version literal actually used by callers (E4)
grep -rho '"/dg/api/v[0-9]*/' \
  references/gis/tools/tests/rest-tests/src/test/java/com/gtech/pd/sites/czh/gis/favorites/ | sort | uniq -c
```
