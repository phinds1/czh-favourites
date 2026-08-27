# SP6: Favourites Controller — Detailed Implementation Plan

**Sub-plan:** SP6 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.favourites.web` (controllers); `cz.bsl.czh.favourites.*` (service/DAO)  
**Progress state:** `.planning/state/SP6_FAVOURITES_CONTROLLER_PROGRESS.md`

## Overview

Implement `FavouritesController` — the single `@RestController` at `/favourites` — with all 10
endpoint methods (5 wager, 5 group). Wire `PlayerContextResolver` (SP5) for header extraction.
Write `FavouritesErrorHandler` (`@ControllerAdvice`) to translate exceptions to HTTP status codes.
Cover every endpoint with Jamcrest integration tests extending `AbstractRestTest` (SP4).

**Endpoints:**

| Method   | Path                                                      | Returns          |
|----------|-----------------------------------------------------------|------------------|
| `POST`   | `/favourites/wagers`                                      | 201 + body       |
| `GET`    | `/favourites/wagers/{id}`                                 | 200 + body       |
| `PUT`    | `/favourites/wagers/{id}`                                 | 200 + body       |
| `DELETE` | `/favourites/wagers/{id}`                                 | 204 No Content   |
| `GET`    | `/favourites/wagers`                                      | 200 + page body  |
| `POST`   | `/favourites/groups`                                      | 201 + body       |
| `GET`    | `/favourites/groups/{groupNumber}`                        | 200 + body       |
| `PUT`    | `/favourites/groups/{groupNumber}`                        | 200 + body       |
| `DELETE` | `/favourites/groups/{groupNumber}`                        | 204 No Content   |
| `GET`    | `/favourites/groups`                                      | 200 + page body  |

**Error handling:**
- `IllegalArgumentException` → 400 `{"error": "<message>"}`
- `NoSuchElementException` → 404 `{"error": "<message>"}`
- Missing/blank `X-Player-Id` → 400 (thrown by `PlayerContextResolver`)

**Key decisions baked in:**
- Controller package is `cz.bsl.favourites.web` (matches migration doc Section 4 spec)
- `game-names` query param is a comma-separated string split by the controller (not `List<String>` binding — avoid Spring MVC multi-value ambiguity with legacy clients)
- `DELETE` returns `ResponseEntity<Void>` with `204 No Content`
- `POST` returns `ResponseEntity` with `201 Created` and `Location` header pointing to the created resource
- No `@Valid` / Hibernate Validator — all validation is done by `FavouritesValidator` in the service
- `FavouritesErrorHandler` is `@ControllerAdvice` in `cz.bsl.favourites.web`; handles both controller packages
- Test infrastructure: `AbstractRestTest` (SP4), `TestDatabaseConfig` (SP4) — do NOT re-init

---

## Phase 1 — Write `FavouritesErrorHandler`

### Goal
All Spring MVC error responses from `FavouritesController` and `AdminFavouritesController` (SP7)
return a consistent JSON error body `{"error": "<message>"}` with the correct HTTP status.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java` (exception types)
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java` (throws `IllegalArgumentException`)

### Tasks

#### 1.1 — Write `FavouritesErrorHandler.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesErrorHandler.java`

```java
// Grep anchor: favourites
@ControllerAdvice
public class FavouritesErrorHandler {

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

#### 1.2 — Write `FavouritesErrorHandlerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesErrorHandlerTest.java`

Use `@WebMvcTest` or plain unit test with a minimal `MockMvc` setup. Tests:
- `handleBadRequest_returns400WithErrorMessage`
- `handleNotFound_returns404WithErrorMessage`

(Plain unit test instantiating handler + calling methods + asserting ResponseEntity is fine —
no Spring context needed.)

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesErrorHandler.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesErrorHandlerTest.java
```

---

## Phase 2 — Write `FavouritesController`

### Goal
All 10 endpoint methods implemented; controller compiles against SP5 service and SP5 resolver.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java` (validateId)

### Tasks

#### 2.1 — Write `FavouritesController.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java`

`@RestController @RequestMapping("/favourites")`. Constructor-inject `FavouritesService service`,
`PlayerContextResolver playerContext`, `FavouritesValidator validator`.

**Wager methods:**

```java
// POST /favourites/wagers → 201 Created
@PostMapping("/wagers")
ResponseEntity<FavouriteWagerDto> createWager(
    @RequestBody FavouriteWagerDto dto,
    HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  FavouriteWagerDto created = service.createWager(dto, playerId);
  URI location = URI.create("/favourites/wagers/" + created.getId());
  return ResponseEntity.created(location).body(created);
}

// GET /favourites/wagers/{id} → 200
@GetMapping("/wagers/{id}")
FavouriteWagerDto getWager(@PathVariable String id, HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  return service.getWager(validator.validateId(id), playerId);
}

// PUT /favourites/wagers/{id} → 200
@PutMapping("/wagers/{id}")
FavouriteWagerDto updateWager(
    @PathVariable String id,
    @RequestBody FavouriteWagerDto dto,
    HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  return service.updateWager(validator.validateId(id), dto, playerId);
}

// DELETE /favourites/wagers/{id} → 204
@DeleteMapping("/wagers/{id}")
ResponseEntity<Void> deleteWager(@PathVariable String id, HttpServletRequest request) {
  String playerId = playerContext.resolve(request);
  service.deleteWager(validator.validateId(id), playerId);
  return ResponseEntity.noContent().build();
}

// GET /favourites/wagers?group=&game-names= → 200
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
// POST /favourites/groups → 201
@PostMapping("/groups")
ResponseEntity<FavouriteGroupDto> createGroup(
    @RequestBody FavouriteGroupDto dto, HttpServletRequest request);

// GET /favourites/groups/{groupNumber} → 200
@GetMapping("/groups/{groupNumber}")
FavouriteGroupDto getGroup(@PathVariable String groupNumber, HttpServletRequest request);

// PUT /favourites/groups/{groupNumber} → 200
@PutMapping("/groups/{groupNumber}")
FavouriteGroupDto updateGroup(
    @PathVariable String groupNumber,
    @RequestBody FavouriteGroupDto dto,
    HttpServletRequest request);

// DELETE /favourites/groups/{groupNumber} → 204
@DeleteMapping("/groups/{groupNumber}")
ResponseEntity<Void> deleteGroup(@PathVariable String groupNumber, HttpServletRequest request);

// GET /favourites/groups → 200
@GetMapping("/groups")
FavouriteGroupPageDto listGroups(HttpServletRequest request);
```

For group `groupNumber` path variable: do NOT call `validator.validateId` (groupNumber is
`String` in the DB, not a `long`). Call `validator.validateGroupNumber(groupNumber)` instead.

Grep anchor: `favourites`

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java
```

---

## Phase 3 — Write wager test fixtures + `FavouritesWagerControllerTest`

### Goal
All 5 wager endpoints covered by Jamcrest integration tests; each test validates both the HTTP
status and the JSON response shape against a `.resp.js` fixture.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java` (base class)
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java`

### Tasks

#### 3.1 — Write request/response fixtures

All files under `czh-favorites-app/src/test/resources/favourites/wager/`:

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

#### 3.2 — Write `FavouritesWagerControllerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java`

Extends `AbstractRestTest`. `@BeforeEach` creates a group for the test player so wager CRUD
can proceed (POST `/favourites/groups` with `groupNumber=1`).

Tests:
- `createWager_returns201WithBody` — POST → 201 → validate body with `create.resp.js`
- `getWager_returns200WithBody` — create then GET → 200 → `get.resp.js`
- `updateWager_returns200WithUpdatedFields` — create then PUT → 200 → assert `wagerName` changed
- `deleteWager_returns204` — create then DELETE → 204 → GET → 404
- `listWagers_returns200PageWithItems` — create two wagers then GET /wagers → page with 2 items

Use `HttpEntity` with `asPlayer(PLAYER_ID)` headers. Parse IDs from the create response using
`jamcrest.access("$.id")`. Grep anchor: `favourites`

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java \
  -r czh-favorites-app/src/test/resources/favourites/wager/ \
  -w "Tests run with ./mvn.sh -pl czh-favorites-app test (norun — skip if norun active)"
```

---

## Phase 4 — Group fixtures + `FavouritesGroupControllerTest` + `FavouritesValidationTest`

### Goal
All 5 group endpoints and validation edge cases covered by Jamcrest tests.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java`
- `czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java` (Phase 2 output)

### Tasks

#### 4.1 — Write group test fixtures

All files under `czh-favorites-app/src/test/resources/favourites/group/`:

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

#### 4.2 — Write `FavouritesGroupControllerTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java`

Tests:
- `createGroup_returns201WithBody`
- `getGroup_returns200WithBody`
- `updateGroup_returns200WithUpdatedName`
- `deleteGroup_returns204`
- `listGroups_returns200PageWithItems`

#### 4.3 — Write `FavouritesValidationTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java`

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
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java \
  -r czh-favorites-app/src/test/resources/favourites/group/ \
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

Run `/security-java` on `FavouritesController.java` and `FavouritesErrorHandler.java`.

Expected areas:
| Area | Expected result |
|------|----------------|
| `game-names` CSV splitting (split on `,` from user input) | `Arrays.stream(csv.split(","))` with strip + blank filter is safe — no injection risk since values flow to DAO `?` placeholders |
| Error messages echoed to client in `{"error": msg}` | Messages come from `IllegalArgumentException` thrown by the service with controlled messages — no stack traces, no internal details exposed |
| `playerId` from `X-Player-Id` flows to service | All DAO calls use `?` placeholders; `PlayerContextResolver.strip()` prevents whitespace games |

#### 5.3 — Routing map

Run `/routing-map` to add `cz.bsl.favourites.web` to `.requirements/design/routing.md`.

```sh
~/bin/geany-progress done 5 \
  -r .requirements/design/routing.md \
  -w "Tests deferred if norun. Run ./mvn.sh -pl czh-favorites-app test"
```
