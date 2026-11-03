# SP6: Favorites Controller — Detailed Implementation Plan

**Sub-plan:** SP6 of 10  
**Sanename:** `favorites`  
**Root package:** `cz.bsl.favorites.web` (controllers); `cz.bsl.czh.favorites.*` (service/DAO)  
**Progress state:** `.planning/state/SP6_FAVORITES_CONTROLLER_PROGRESS.md`

## Overview

Implement `FavoritesController` — the single `@RestController` at `/favorites` — with all 10
endpoint methods (5 wager, 5 group). Wire `PlayerContextResolver` (SP5) for header extraction.
Write `FavoritesErrorHandler` (`@ControllerAdvice`) to translate exceptions to HTTP status codes.
Cover every endpoint with Jamcrest integration tests extending `AbstractRestTest` (SP4).

**Endpoints:**

| Method   | Path                                                      | Returns          |
|----------|-----------------------------------------------------------|------------------|
| `POST`   | `/favorites/wagers`                                      | 201 + body       |
| `GET`    | `/favorites/wagers/{id}`                                 | 200 + body       |
| `PUT`    | `/favorites/wagers/{id}`                                 | 200 + body       |
| `DELETE` | `/favorites/wagers/{id}`                                 | 204 No Content   |
| `GET`    | `/favorites/wagers`                                      | 200 + page body  |
| `POST`   | `/favorites/groups`                                      | 201 + body       |
| `GET`    | `/favorites/groups/{groupNumber}`                        | 200 + body       |
| `PUT`    | `/favorites/groups/{groupNumber}`                        | 200 + body       |
| `DELETE` | `/favorites/groups/{groupNumber}`                        | 204 No Content   |
| `GET`    | `/favorites/groups`                                      | 200 + page body  |

**Error handling:**
- `IllegalArgumentException` → 400 `{"error": "<message>"}`
- `NoSuchElementException` → 404 `{"error": "<message>"}`
- Missing/blank `X-Player-Id` → 400 (thrown by `PlayerContextResolver`)

**Key decisions baked in:**
- Controller package is `cz.bsl.favorites.web` (matches migration doc Section 4 spec)
- `game-names` query param is a comma-separated string split by the controller (not `List<String>` binding — avoid Spring MVC multi-value ambiguity with legacy clients)
- `DELETE` returns `ResponseEntity<Void>` with `204 No Content`
- `POST` returns `ResponseEntity` with `201 Created` and `Location` header pointing to the created resource
- No `@Valid` / Hibernate Validator — all validation is done by `FavoritesValidator` in the service
- `FavoritesErrorHandler` is `@ControllerAdvice` in `cz.bsl.favorites.web`; handles both controller packages
- Test infrastructure: `AbstractRestTest` (SP4), `TestDatabaseConfig` (SP4) — do NOT re-init

---

## Phase 1 — Write `FavoritesErrorHandler`

### Goal
All Spring MVC error responses from `FavoritesController` and `AdminFavoritesController` (SP7)
return a consistent JSON error body `{"error": "<message>"}` with the correct HTTP status.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java` (exception types)
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/PlayerContextResolver.java` (throws `IllegalArgumentException`)

### Tasks

#### 1.1 — Write `FavoritesErrorHandler.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesErrorHandler.java`

```java
// Grep anchor: favorites
@ControllerAdvice
public class FavoritesErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
```

#### 1.2 — Write `FavoritesErrorHandlerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesErrorHandlerTest.java`

Use `@WebMvcTest` or plain unit test with a minimal `MockMvc` setup. Tests:
- `handleBadRequest_returns400WithErrorMessage`
- `handleNotFound_returns404WithErrorMessage`

(Plain unit test instantiating handler + calling methods + asserting ResponseEntity is fine —
no Spring context needed.)

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesErrorHandler.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesErrorHandlerTest.java
```

---

## Phase 2 — Write `FavoritesController`

### Goal
All 10 endpoint methods implemented; controller compiles against SP5 service and SP5 resolver.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/PlayerContextResolver.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java` (validateId)

### Tasks

#### 2.1 — Write `FavoritesController.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java`

`@RestController @RequestMapping("/favorites")`. Constructor-inject `FavoritesService service`,
`PlayerContextResolver playerContext`, `FavoritesValidator validator`.

**Wager methods:**

```java
// POST /favorites/wagers → 201 Created
@PostMapping("/wagers")
ResponseEntity<FavouriteWagerDto> createWager(
    @RequestBody FavouriteWagerDto dto,
    HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  FavouriteWagerDto created = service.createWager(dto, playerId);
  URI location = URI.create("/favorites/wagers/" + created.getId());
  return ResponseEntity.created(location).body(created);
}

// GET /favorites/wagers/{id} → 200
@GetMapping("/wagers/{id}")
FavouriteWagerDto getWager(@PathVariable String id, HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  return service.getWager(validator.validateId(id), playerId);
}

// PUT /favorites/wagers/{id} → 200
@PutMapping("/wagers/{id}")
FavouriteWagerDto updateWager(
    @PathVariable String id,
    @RequestBody FavouriteWagerDto dto,
    HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  return service.updateWager(validator.validateId(id), dto, playerId);
}

// DELETE /favorites/wagers/{id} → 204
@DeleteMapping("/wagers/{id}")
ResponseEntity<Void> deleteWager(@PathVariable String id, HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  service.deleteWager(validator.validateId(id), playerId);
  return ResponseEntity.noContent().build();
}

// GET /favorites/wagers?group=&game-names= → 200
@GetMapping("/wagers")
FavouriteWagerPageDto listWagers(
    @RequestParam(name = "group", required = false) String group,
    @RequestParam(name = "game-names", required = false) String gameNamesCsv,
    HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  List<String> gameNames = (gameNamesCsv == null || gameNamesCsv.isBlank())
      ? List.of()
      : Arrays.stream(gameNamesCsv.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
  return service.listWagers(playerId, group, gameNames);
}
```

**Group methods** (mirror the wager pattern):

```java
// POST /favorites/groups → 201
@PostMapping("/groups")
ResponseEntity<FavouriteGroupDto> createGroup(
    @RequestBody FavouriteGroupDto dto, HttpServletRequest request);

// GET /favorites/groups/{groupNumber} → 200
@GetMapping("/groups/{groupNumber}")
FavouriteGroupDto getGroup(@PathVariable String groupNumber, HttpServletRequest request);

// PUT /favorites/groups/{groupNumber} → 200
@PutMapping("/groups/{groupNumber}")
FavouriteGroupDto updateGroup(
    @PathVariable String groupNumber,
    @RequestBody FavouriteGroupDto dto,
    HttpServletRequest request);

// DELETE /favorites/groups/{groupNumber} → 204
@DeleteMapping("/groups/{groupNumber}")
ResponseEntity<Void> deleteGroup(@PathVariable String groupNumber, HttpServletRequest request);

// GET /favorites/groups → 200
@GetMapping("/groups")
FavouriteGroupPageDto listGroups(HttpServletRequest request);
```

For group `groupNumber` path variable: do NOT call `validator.validateId` (groupNumber is
`String` in the DB, not a `long`). Call `validator.validateGroupNumber(groupNumber)` instead.

Grep anchor: `favorites`

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java
```

---

## Phase 3 — Write wager test fixtures + `FavoritesWagerControllerTest`

### Goal
All 5 wager endpoints covered by Jamcrest integration tests; each test validates both the HTTP
status and the JSON response shape against a `.resp.js` fixture.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/AbstractRestTest.java` (base class)
- `czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavouriteWagerDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/WagerDto.java`

### Tasks

#### 3.1 — Write request/response fixtures

All files under `czh-favorites-app/src/test/resources/favorites/wager/`:

**`create.req.js`** — minimal valid create-wager body:
```js
t = {
  groupNumber: groupNumber,
  wagerName: "My Lotto Ticket",
  wager: {
    gameName: gameName,
    stake: 100,
    price: 100,
    duration: 1,
    serialNumber: "SN001",
    boards: [[1, 2, 3, 4, 5, 6]]
  }
};
```
(inject `groupNumber` and `gameName` as template variables)

**`create.resp.js`** — validate created wager response shape:
```js
jsonDefinition = {
  id:          notNull(),
  playerId:    notNull(),
  groupNumber: notNull(),
  gameName:    notNull(),
  wagerName:   notNull(),
  flags:       0,
  createdAt:   notNull(),
  wager: {
    gameName:     notNull(),
    stake:        100,
    price:        100,
    duration:     1,
    serialNumber: "SN001",
    boards:       notNull()
  }
};
```

**`get.resp.js`** — same shape as create.resp.js (reuse or use `validateExactMatch`)

**`update.req.js`** — changed wagerName:
```js
t = {
  groupNumber: groupNumber,
  wagerName:   "Updated Name",
  wager: {
    gameName:     gameName,
    stake:        200,
    price:        200,
    duration:     2,
    serialNumber: "SN002",
    boards:       [[7, 8, 9]]
  }
};
```

**`list.resp.js`** — page wrapper:
```js
jsonDefinition = {
  items:      notNull(),
  totalCount: notNull()
};
```

#### 3.2 — Write `FavoritesWagerControllerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesWagerControllerTest.java`

Extends `AbstractRestTest`. `@BeforeEach` creates a group for the test player so wager CRUD
can proceed (POST `/favorites/groups` with `groupNumber=1`).

Tests:
- `createWager_returns201WithBody` — POST → 201 → validate body with `create.resp.js`
- `getWager_returns200WithBody` — create then GET → 200 → `get.resp.js`
- `updateWager_returns200WithUpdatedFields` — create then PUT → 200 → assert `wagerName` changed
- `deleteWager_returns204` — create then DELETE → 204 → GET → 404
- `listWagers_returns200PageWithItems` — create two wagers then GET /wagers → page with 2 items

Use `HttpEntity` with `asPlayer(PLAYER_ID)` headers. Parse IDs from the create response using
`jamcrest.access("$.id")`. Grep anchor: `favorites`

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesWagerControllerTest.java \
  -r czh-favorites-app/src/test/resources/favorites/wager/ \
  -w "Tests run with ./mvn.sh -pl czh-favorites-app test (norun — skip if norun active)"
```

---

## Phase 4 — Group fixtures + `FavoritesGroupControllerTest` + `FavoritesValidationTest`

### Goal
All 5 group endpoints and validation edge cases covered by Jamcrest tests.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/AbstractRestTest.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavouriteGroupDto.java`
- `czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java` (Phase 2 output)

### Tasks

#### 4.1 — Write group test fixtures

All files under `czh-favorites-app/src/test/resources/favorites/group/`:

**`create.req.js`:**
```js
t = {
  groupNumber: groupNumber,
  groupName:   "My Lotto Group"
};
```

**`create.resp.js`:**
```js
jsonDefinition = {
  id:          notNull(),
  playerId:    notNull(),
  groupNumber: notNull(),
  groupName:   notNull(),
  flags:       0,
  createdAt:   notNull(),
  updatedAt:   notNull()
};
```

**`get.resp.js`** — same shape as create.resp.js

**`update.req.js`:**
```js
t = {
  groupNumber: groupNumber,
  groupName:   "Updated Group Name"
};
```

**`list.resp.js`:**
```js
jsonDefinition = {
  items:      notNull(),
  totalCount: notNull()
};
```

#### 4.2 — Write `FavoritesGroupControllerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesGroupControllerTest.java`

Tests:
- `createGroup_returns201WithBody`
- `getGroup_returns200WithBody`
- `updateGroup_returns200WithUpdatedName`
- `deleteGroup_returns204`
- `listGroups_returns200PageWithItems`

#### 4.3 — Write `FavoritesValidationTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesValidationTest.java`

Tests:
- `missingPlayerHeader_returns400` — GET /wagers without `X-Player-Id`
- `blankPlayerId_returns400` — `X-Player-Id: " "`
- `invalidWagerId_returns400` — GET /wagers/abc → 400
- `wagerNotFound_returns404` — GET /wagers/999999 → 404
- `groupNotFound_returns404` — GET /groups/5 (no group created) → 404
- `createWager_groupDoesNotExist_returns404` — POST wager with non-existent groupNumber → 404
- `createGroup_duplicate_returns400` — POST same groupNumber twice → 400
- `createWager_limitExceeded_returns400` — 50 wagers already exist (mock or use properties config)

```sh
~/bin/geany-progress done 4 \
  -r czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesGroupControllerTest.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesValidationTest.java \
  -r czh-favorites-app/src/test/resources/favorites/group/ \
  -w "Tests run with ./mvn.sh -pl czh-favorites-app test (norun — skip if norun active)"
```

---

## Phase 5 — Post-SP sign-off

### Tasks

#### 5.1 — Full test + install (skip if norun active)

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

#### 5.2 — Security

Run `/security-java` on `FavoritesController.java` and `FavoritesErrorHandler.java`.

Expected areas:
| Area | Expected result |
|------|----------------|
| `game-names` CSV splitting (split on `,` from user input) | `Arrays.stream(csv.split(","))` with strip + blank filter is safe — no injection risk since values flow to DAO `?` placeholders |
| Error messages echoed to client in `{"error": msg}` | Messages come from `IllegalArgumentException` thrown by the service with controlled messages — no stack traces, no internal details exposed |
| `playerId` from `X-Player-Id` flows to service | All DAO calls use `?` placeholders; `PlayerContextResolver.strip()` prevents whitespace games |

#### 5.3 — Routing map

Run `/routing-map` to add `cz.bsl.favorites.web` to `.requirements/design/routing.md`.

```sh
~/bin/geany-progress done 5 \
  -r .requirements/design/routing.md \
  -w "Tests deferred if norun. Run ./mvn.sh -pl czh-favorites-app test"
```
