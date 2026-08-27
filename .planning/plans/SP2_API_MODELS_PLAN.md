# SP2: JSON API Models — Detailed Implementation Plan

**Sub-plan:** SP2 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.czh.favourites`  
**Progress state:** `.planning/state/SP2_API_MODELS_PROGRESS.md`

## Overview

Port the four legacy DTOs from `gateway-draw-games-rest-api` into `czh-favorites-api`, clean of
all JAX-RS, Jackson 1.x, and Joda Time. Add `WagerDto` (the nested wager sub-object). All classes
land in `cz.bsl.czh.favourites.api`. Jackson 2 annotations and `java.time.Instant` throughout.
No Hibernate Validator. The SP is done when every DTO round-trips cleanly through Jackson and
`./mvn.sh -pl czh-favorites-api test` is green.

## Context budget per phase

Each phase fits in one Claude Sonnet 4.6 context window. Read **only** the files listed under
**Files to read** before implementing a phase. Write progress state and confirm the build is green
before continuing to the next phase.

---

## Phase 1 — Write `WagerDto`

### Goal
`WagerDto` serialises to JSON and deserialises back with no data loss on `boards`.

### Files to read before starting
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/WagerDTO.java`
- `.planning/java25-spring-boot-migration.md` (JSON API section — DTO mapping table)

### Tasks

#### 1.1 — Write `WagerDto.java`

**Path:** `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java`

Rules:
- Package: `cz.bsl.czh.favourites.api`
- Remove all `com.gtech`, `org.codehaus.jackson`, Joda Time imports — use Jackson 2
  (`com.fasterxml.jackson.annotation`) only
- Fields to keep (from migration plan):
  - `gameName` — `String`
  - `stake` — `long`
  - `price` — `long`
  - `duration` — `int` (number of draws)
  - `serialNumber` — `String` (nullable)
  - `boards` — `List<List<Integer>>` (board stacks containing boards containing pick integers;
    preserve nesting exactly — this is the most critical field)
- Annotate with `@JsonProperty` only where the JSON wire name differs from the Java field name
- `@JsonIgnoreProperties(ignoreUnknown = true)` on the class
- No-arg constructor + all-args constructor (or builder matching legacy style — check reference)
- Standard getters/setters

#### 1.2 — Write `WagerDtoTest.java`

**Path:** `czh-favorites-api/src/test/java/cz/bsl/czh/favourites/api/WagerDtoTest.java`

Use an `ObjectMapper` instance (no Spring context needed — just `new ObjectMapper()`; no
`JavaTimeModule` needed here since there are no `Instant` fields in `WagerDto`).

Tests:
- `roundTrip_withBoards`: populate 2 board stacks, 3 boards each, 6 picks each → serialize →
  deserialize → assert all fields equal including exact `boards` nesting
- `roundTrip_emptyBoards`: `boards = []` → round-trip → assert empty
- `roundTrip_nullSerialNumber`: `serialNumber = null` → round-trip → assert null preserved
- `unknownFieldsIgnored`: deserialize JSON that has an extra unknown field → no exception,
  known fields populated correctly

#### 1.3 — Build verify

```bash
./mvn.sh -pl czh-favorites-api test
```

All tests green before moving to Phase 2.

---

## Phase 2 — Write `FavouriteWagerDto`

### Goal
`FavouriteWagerDto` round-trips cleanly including the nested `WagerDto` sub-object and
`java.time.Instant` timestamps.

### Files to read before starting
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerDTO.java`
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/resources/com/gtech/esa/gateway/rest/drawgames/dto/constraints-FavoriteWagerDTO.xml`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java` (Phase 1 output)

### Tasks

#### 2.1 — Write `FavouriteWagerDto.java`

**Path:** `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java`

Rules:
- Package: `cz.bsl.czh.favourites.api`
- Remove all JAX-RS, `org.codehaus.jackson`, Joda Time, Hibernate Validator imports
- Fields (from migration plan DTO mapping table):
  - `id` — `Long` (null on create, set on read)
  - `playerId` — `String`
  - `groupNumber` — `String`
  - `gameName` — `String`
  - `wagerName` — `String` (defaults to `""`)
  - `flags` — `int` (bitmask, default `0`)
  - `createdAt` — `java.time.Instant` (null on create)
  - `wager` — `WagerDto` (nested sub-object; this is what is stored as `WAGER_JSON` in the DAO)
- `@JsonIgnoreProperties(ignoreUnknown = true)` on the class
- For `Instant` fields: register `JavaTimeModule` in the `ObjectMapper` in tests; do **not** add
  custom serializer annotations on the class itself — let `JavaTimeModule` handle it transparently
- No Hibernate Validator annotations — validation is the service layer's responsibility (SP5)

#### 2.2 — Write `FavouriteWagerDtoTest.java`

**Path:** `czh-favorites-api/src/test/java/cz/bsl/czh/favourites/api/FavouriteWagerDtoTest.java`

Use `new ObjectMapper().registerModule(new JavaTimeModule())`. Add
`jackson-datatype-jsr310` test dependency to `czh-favorites-api/pom.xml` if not already there
(it is in the root BOM — just add scope test).

Tests:
- `fullRoundTrip`: all fields set including `createdAt` and a fully populated `WagerDto` →
  serialize → deserialize → assert every field
- `nullableFieldsRoundTrip`: `id = null`, `createdAt = null` (create-time scenario) → round-trip
  → nulls preserved
- `unknownFieldsIgnored`: JSON with extra field → no exception
- `wagerSubObjectRoundTrip`: nested `WagerDto` with 2 board stacks survives the round-trip with
  no data loss on `boards`

#### 2.3 — Build verify

```bash
./mvn.sh -pl czh-favorites-api test
```

---

## Phase 3 — Write `FavouriteGroupDto`

### Goal
`FavouriteGroupDto` round-trips cleanly with `java.time.Instant` `createdAt` and `updatedAt`.

### Files to read before starting
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupDTO.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java` (Phase 2 output — for style consistency)

### Tasks

#### 3.1 — Write `FavouriteGroupDto.java`

**Path:** `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java`

Fields:
- `id` — `Long` (null on create)
- `playerId` — `String`
- `groupNumber` — `String`
- `groupName` — `String` (defaults to `""`)
- `flags` — `int` (default `0`)
- `createdAt` — `java.time.Instant` (nullable)
- `updatedAt` — `java.time.Instant` (nullable)

Same Jackson 2 / `java.time` conventions as `FavouriteWagerDto`. `@JsonIgnoreProperties(ignoreUnknown = true)`.

#### 3.2 — Write `FavouriteGroupDtoTest.java`

**Path:** `czh-favorites-api/src/test/java/cz/bsl/czh/favourites/api/FavouriteGroupDtoTest.java`

Tests:
- `fullRoundTrip`: all fields set including both timestamps
- `nullTimestampsRoundTrip`: `id`, `createdAt`, `updatedAt` all null
- `unknownFieldsIgnored`

#### 3.3 — Build verify

```bash
./mvn.sh -pl czh-favorites-api test
```

---

## Phase 4 — Write page wrappers

### Goal
`FavouriteWagerPageDto` and `FavouriteGroupPageDto` round-trip with their `items` lists.

### Files to read before starting
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerPageDTO.java`
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupPageDTO.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java`

### Tasks

#### 4.1 — Write `FavouriteWagerPageDto.java`

**Path:** `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerPageDto.java`

Fields:
- `items` — `List<FavouriteWagerDto>`
- `totalCount` — `int`

`@JsonIgnoreProperties(ignoreUnknown = true)`. No-arg constructor + all-args constructor.

#### 4.2 — Write `FavouriteGroupPageDto.java`

**Path:** `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupPageDto.java`

Fields:
- `items` — `List<FavouriteGroupDto>`
- `totalCount` — `int`

Same conventions.

#### 4.3 — Write page DTO tests

Add tests to `FavouriteWagerDtoTest` or write `FavouriteWagerPageDtoTest.java` — either is fine.

Tests (both page types):
- `pageWithItemsRoundTrip`: page with 2 items, `totalCount = 2` → round-trip → assert
- `emptyPageRoundTrip`: `items = []`, `totalCount = 0` → round-trip → assert

#### 4.4 — Build verify

```bash
./mvn.sh -pl czh-favorites-api test
./mvn.sh install
```

Both commands must be green before Phase 5.

---

## Phase 5 — Post-SP sign-off

### Tasks

#### 5.1 — Full test + install

```bash
./mvn.sh -pl czh-favorites-api test
./mvn.sh install
```

Zero failures. Zero `@Disabled`. Fix any Jackson serialisation issues now.

#### 5.2 — Coverage

```bash
bin/coverage.sh
```

Read `target/jacoco-report.csv`. Target ≥ 80% instruction coverage for `czh-favorites-api` classes.
`WagerDto`, `FavouriteWagerDto`, `FavouriteGroupDto`, and the page wrappers should all be covered
by the round-trip tests. Record any accepted exceptions (e.g. unused no-arg constructors needed
by Jackson).

#### 5.3 — Security

Run `/security-java` on new `czh-favorites-api` Java files. Expected findings:

| Risk | Response |
|------|----------|
| Deserialisation of untrusted JSON | `@JsonIgnoreProperties(ignoreUnknown=true)` is present; no polymorphic types — no action needed |
| `boards` list depth | Input validation is the service layer's responsibility (SP5); note this in code comment |

For any HIGH finding: write failing test, apply fix, confirm green.

#### 5.4 — Refactor

Check all five DTO classes for:
- Duplicate Jackson annotation patterns → extract to shared base if >2 classes share them
- Any leftover legacy imports
- Comments that match the code

Re-run `./mvn.sh -pl czh-favorites-api test` after any change.

#### 5.5 — Routing map

Run `/routing-map` to record the new `cz.bsl.czh.favourites.api` package in
`.requirements/design/routing.md`.

#### 5.6 — Write progress state

Update `.planning/state/SP2_API_MODELS_PROGRESS.md` to mark Phase 5 complete and SP2 DONE.
