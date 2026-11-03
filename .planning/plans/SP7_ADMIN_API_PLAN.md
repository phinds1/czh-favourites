# SP7: Admin API — Detailed Implementation Plan

**Sub-plan:** SP7 of 10  
**Sanename:** `favorites`  
**Root package:** `cz.bsl.favorites.web` (controller)  
**Progress state:** `.planning/state/SP7_ADMIN_API_PROGRESS.md`

## Overview

Port `AdminFavoriteWagerResource` to a Spring MVC `@RestController` at `/admin/favorites`.
The admin API provides **read-only** access to a player's wagers and groups, identifying the
player via a `{playerId}` **path variable** rather than the `X-Player-Id` header used by the
player controller. This is a deliberate security boundary: the admin caller is a trusted
back-office system, not a player session.

The admin controller reuses `FavoritesService` (SP5) and `FavoritesErrorHandler` (SP6)
unchanged. SP7 adds only the controller class, its test class, and four Jamcrest fixtures.

**Endpoints:**

| Method | Path                                                           | Description         |
|--------|----------------------------------------------------------------|---------------------|
| `GET`  | `/admin/favorites/players/{playerId}/wagers`                  | List player wagers  |
| `GET`  | `/admin/favorites/players/{playerId}/wagers/{id}`             | Get single wager    |
| `GET`  | `/admin/favorites/players/{playerId}/groups`                  | List player groups  |
| `GET`  | `/admin/favorites/players/{playerId}/groups/{groupNumber}`    | Get single group    |

**Security boundary:**
- The admin paths are protected by the API gateway with admin-level credentials (not player headers)
- `playerId` is accepted as a trusted path parameter — no `X-Player-Id` header validation
- The controller is intentionally read-only: no POST / PUT / DELETE admin mutations exist
- Document this boundary explicitly in Javadoc to prevent accidental mutation endpoint creep

**Dependencies:** SP5 (FavoritesService), SP6 (FavoritesErrorHandler).  
SP7 is written after SP6 is complete so the `FavoritesErrorHandler` is reusable.

---

## Phase 1 — Write `AdminFavoritesController`

### Goal
All 4 read-only admin endpoints implemented and compiling.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java` (validateId, validateGroupNumber)
- `czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesErrorHandler.java` (SP6 output — already handles all exceptions)
- `czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java` (SP6 output — mirror error-handling style)

### Tasks

#### 1.1 — Write `AdminFavoritesController.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java`

`@RestController @RequestMapping("/admin/favorites")`. Constructor-inject `FavoritesService service`,
`FavoritesValidator validator`.

```java
/**
 * Read-only admin view of a player's favourite wagers and groups.
 *
 * <p><b>Security boundary:</b> {@code playerId} is a trusted path parameter — this endpoint is
 * reachable only via the admin gateway tier which enforces operator-level credentials. No
 * {@code X-Player-Id} header is validated here. Mutation operations (POST/PUT/DELETE) are
 * intentionally absent; use {@code FavoritesController} for player-facing CRUD.
 *
 * <p>Error handling is provided by {@link FavoritesErrorHandler} ({@code @ControllerAdvice}):
 * {@link IllegalArgumentException} → 400, {@link NoSuchElementException} → 404.
 *
 * <p>Grep anchor: favorites
 */
@RestController
@RequestMapping("/admin/favorites")
public class AdminFavoritesController {

    private final FavoritesService service;
    private final FavoritesValidator validator;

    public AdminFavoritesController(FavoritesService service, FavoritesValidator validator) {
        this.service   = service;
        this.validator = validator;
    }

    // GET /admin/favorites/players/{playerId}/wagers
    @GetMapping("/players/{playerId}/wagers")
    public FavouriteWagerPageDto listWagers(@PathVariable String playerId) {
        return service.listWagers(playerId, null, List.of());
    }

    // GET /admin/favorites/players/{playerId}/wagers/{id}
    @GetMapping("/players/{playerId}/wagers/{id}")
    public FavouriteWagerDto getWager(
            @PathVariable String playerId,
            @PathVariable String id) {
        return service.getWager(validator.validateId(id), playerId);
    }

    // GET /admin/favorites/players/{playerId}/groups
    @GetMapping("/players/{playerId}/groups")
    public FavouriteGroupPageDto listGroups(@PathVariable String playerId) {
        return service.listGroups(playerId);
    }

    // GET /admin/favorites/players/{playerId}/groups/{groupNumber}
    @GetMapping("/players/{playerId}/groups/{groupNumber}")
    public FavouriteGroupDto getGroup(
            @PathVariable String playerId,
            @PathVariable String groupNumber) {
        return service.getGroup(groupNumber, playerId);
    }
}
```

**Note:** Do NOT call `validator.validateGroupNumber(groupNumber)` in `getGroup` — admin callers
may legitimately pass any group number to check existence (including out-of-range ones for
diagnostic purposes). The service will return 404 if the group doesn't exist. If the migration
doc disagrees, document this decision in Javadoc.

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java
```

---

## Phase 2 — Write admin fixtures + `AdminFavoritesControllerTest`

### Goal
All 4 admin endpoints covered by Jamcrest tests; fixtures written from scratch.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/AbstractRestTest.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavouriteWagerDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavouriteGroupDto.java`

### Tasks

#### 2.1 — Write admin Jamcrest fixtures

All files under `czh-favorites-app/src/test/resources/favorites/admin/`:

**`wager-get.resp.js`:**
```js
jsonDefinition = {
  id:          notNull(),
  playerId:    notNull(),
  groupNumber: notNull(),
  gameName:    notNull(),
  wagerName:   notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  wager:       notNull()
};
```

**`wagers.resp.js`:**
```js
jsonDefinition = {
  items:      notNull(),
  totalCount: notNull()
};
```

**`group-get.resp.js`:**
```js
jsonDefinition = {
  id:          notNull(),
  playerId:    notNull(),
  groupNumber: notNull(),
  groupName:   notNull(),
  flags:       notNull(),
  createdAt:   notNull(),
  updatedAt:   notNull()
};
```

**`groups.resp.js`:**
```js
jsonDefinition = {
  items:      notNull(),
  totalCount: notNull()
};
```

#### 2.2 — Write `AdminFavoritesControllerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favorites/web/AdminFavoritesControllerTest.java`

Extends `AbstractRestTest`. `@BeforeEach` seeds data for a known player (`ADMIN_TEST_PLAYER`)
by:
1. POSTing a group via `/favorites/groups` (using `asPlayer(ADMIN_TEST_PLAYER)`)
2. POSTing a wager via `/favorites/wagers` (using `asPlayer(ADMIN_TEST_PLAYER)`)

Then tests hit the admin endpoints without any `X-Player-Id` header (use plain
`new HttpHeaders()` with `Content-Type: application/json`).

```java
private static final String ADMIN_TEST_PLAYER = "admin-test-player-001";
```

Tests:
- `listWagers_returns200PageWithSeededWager` — GET `/admin/favorites/players/{ADMIN_TEST_PLAYER}/wagers` → 200 → validate `wagers.resp.js` → assert `totalCount >= 1`
- `getWager_returns200WithBody` — use wager id from seed → GET `/admin/favorites/players/{ADMIN_TEST_PLAYER}/wagers/{id}` → 200 → validate `wager-get.resp.js`
- `getWager_unknownId_returns404` — GET wager id 999999 → 404
- `listGroups_returns200PageWithSeededGroup` — GET groups → 200 → validate `groups.resp.js` → assert `totalCount >= 1`
- `getGroup_returns200WithBody` — GET group → 200 → validate `group-get.resp.js`
- `getGroup_unknownGroupNumber_returns404` — GET group 9 (not seeded) → 404

Grep anchor: `favorites`

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/test/java/cz/bsl/favorites/web/AdminFavoritesControllerTest.java \
  -r czh-favorites-app/src/test/resources/favorites/admin/ \
  -w "Tests deferred if norun active. Run ./mvn.sh -pl czh-favorites-app test when clear."
```

---

## Phase 3 — Post-SP sign-off

### Tasks

#### 3.1 — Full test + install (skip if norun active)

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

#### 3.2 — Security

Run `/security-java` on `AdminFavoritesController.java`.

Key areas:
| Area | Expected result |
|------|----------------|
| `playerId` path param flows to service then DAO | DAO uses `?` placeholders — no injection risk |
| No auth enforcement in the controller itself | Correct — admin gate enforced by gateway; document in Javadoc |
| Read-only: no mutation methods | Correct — verify no POST/PUT/DELETE methods exist |

#### 3.3 — Routing map

Run `/routing-map` to add `AdminFavoritesController` to `.requirements/design/routing.md`.

```sh
~/bin/geany-progress done 3 \
  -r .requirements/design/routing.md \
  -w "Tests deferred if norun. Run ./mvn.sh -pl czh-favorites-app test"
```
