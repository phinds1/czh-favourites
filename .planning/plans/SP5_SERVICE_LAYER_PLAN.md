# SP5: Service Layer — Detailed Implementation Plan

**Sub-plan:** SP5 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.czh.favourites`  
**Progress state:** `.planning/state/SP5_SERVICE_LAYER_PROGRESS.md`

## Overview

Port the business logic from `DefaultFavoriteWagerService`, `DefaultFavoriteWagerConverter`, and
`DefaultFavoriteWagerValidator` into clean Java 25 / Spring Boot 4 equivalents. Remove all JMX,
Guava, Joda Time, Hibernate Validator, `GameServiceRegistry`, `WagerMinifier.expand()`, and the
normalised board-stack domain model. The new service works directly with `FavouriteWagerDto` /
`FavouriteGroupDto` (API layer) and `FavouriteWagerRecord` / `FavouriteGroupRecord` (DAO layer).

**`FavouritesProperties` already written in SP1 — do not recreate it.**

**Key design decisions:**
- Converter maps `FavouriteWagerDto` ↔ `FavouriteWagerRecord` (no intermediate domain object)
- Validator throws `IllegalArgumentException` with a human-readable message (controller catches
  it and returns HTTP 400)
- Service throws `NoSuchElementException` for 404 (controller catches it and returns HTTP 404)
- `WagerMinifier.expand()` is dropped — caller sends complete `WagerDto`
- `GameServiceRegistry` and `GameHostService` are dropped — game identity is in the JSON
- Time comes from `java.time.Instant.now()`, not `gameHostService.getCurrentTime()`
- Auto-assign `groupNumber`: when a create-wager request has no group, auto-assign the next
  available slot (1–10); port the Guava `Ordering.natural().max()` logic as Java streams

## Context budget per phase

Each phase fits in one Claude Sonnet 4.6 context window. Read **only** the files listed under
**Files to read** before implementing. Mark phase done with `~/bin/geany-progress done N`.

---

## Phase 1 — Write `FavouritesConverter`

### Goal
`FavouritesConverter` converts between `FavouriteWagerDto`/`FavouriteGroupDto` (API) and
`FavouriteWagerRecord`/`FavouriteGroupRecord` (DAO), serialising/deserialising `WAGER_JSON`
via `ObjectMapper`.

### Files to read before starting
- `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/converter/DefaultFavoriteWagerConverter.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java`
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteWagerRecord.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteGroupRecord.java`

### Tasks

#### 1.1 — Write `FavouritesConverter.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java`

`@Component`. Constructor-inject `ObjectMapper objectMapper`.

Methods:

```java
/** Convert an incoming API DTO to a new DAO record ready for insert. */
FavouriteWagerRecord toRecord(FavouriteWagerDto dto, String playerId);

/** Convert a DAO record back to an API DTO for HTTP responses. */
FavouriteWagerDto toDto(FavouriteWagerRecord record);

/** Convert an incoming group API DTO to a DAO record. */
FavouriteGroupRecord toRecord(FavouriteGroupDto dto, String playerId);

/** Convert a DAO record to a group API DTO. */
FavouriteGroupDto toDto(FavouriteGroupRecord record);

/** Convert a list of wager records to a page DTO. */
FavouriteWagerPageDto toWagerPage(List<FavouriteWagerRecord> records);

/** Convert a list of group records to a page DTO. */
FavouriteGroupPageDto toGroupPage(List<FavouriteGroupRecord> records);
```

Implementation notes:
- `toRecord(FavouriteWagerDto dto, String playerId)`:
  - `record.setWagerJson(objectMapper.writeValueAsString(dto.getWager()))` — wrap `JsonProcessingException` as `IllegalStateException`
  - `record.setPlayerId(playerId)` — always from the header, never from the DTO
  - `record.setGroupNumber(dto.getGroupNumber())`
  - `record.setGameName(dto.getWager() != null ? dto.getWager().getGameName() : "")` — promoted column mirrors wager sub-object
  - `record.setWagerName(dto.getWagerName() != null ? dto.getWagerName() : "")`
  - `record.setFlags(0)` — always zero on create; service layer manages flags
- `toDto(FavouriteWagerRecord record)`:
  - Deserialise `record.getWagerJson()` → `WagerDto` via `objectMapper.readValue`; wrap `JsonProcessingException` as `IllegalStateException`
  - Set `id`, `playerId`, `groupNumber`, `gameName`, `wagerName`, `flags`, `createdAt` from record
- `toRecord(FavouriteGroupDto, playerId)` / `toDto(FavouriteGroupRecord)`: straightforward field mapping; no JSON
- `toWagerPage` / `toGroupPage`: `new FavouriteWagerPageDto(records.stream().map(this::toDto).toList(), records.size())`

Grep anchor: `favourites`

#### 1.2 — Write `FavouritesConverterTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesConverterTest.java`

Use `new ObjectMapper().registerModule(new JavaTimeModule())`. No Spring context needed.

Tests:
- `toRecord_wager_setsAllFields`: full `FavouriteWagerDto` with boards → `FavouriteWagerRecord` → assert `wagerJson` contains `gameName`; assert `playerId` from parameter not DTO
- `toDto_wager_roundTrip`: `FavouriteWagerRecord` with valid JSON → `FavouriteWagerDto` → assert `wager.boards` intact
- `toRecord_wager_flagsAlwaysZero`: dto with `flags=99` → record → `flags==0`
- `toRecord_group_setsAllFields`
- `toDto_group_roundTrip`
- `toWagerPage_setsItemsAndCount`
- `toGroupPage_setsItemsAndCount`

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesConverterTest.java
```

---

## Phase 2 — Write `FavouritesValidator`

### Goal
`FavouritesValidator` enforces all pre-condition rules as plain Java, throwing
`IllegalArgumentException` with a human-readable message on failure.

### Files to read before starting
- `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/validator/DefaultFavoriteWagerValidator.java`
- `czh-favorites-app/src/main/java/cz/bsl/favourites/config/FavouritesProperties.java`
- `.planning/java25-spring-boot-migration.md` (validation rules section, lines 163–172)

### Tasks

#### 2.1 — Write `FavouritesValidator.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java`

`@Component`. Constructor-inject `FavouritesProperties properties`.

Methods and rules:

```java
/**
 * Validate a wager create/update request.
 * Throws IllegalArgumentException with a message the controller returns as HTTP 400.
 */
void validateWager(FavouriteWagerDto dto);

/**
 * Validate a group create/update request.
 */
void validateGroup(FavouriteGroupDto dto);

/**
 * Validate that a path-variable id is a positive long. Returns the parsed value.
 * Throws IllegalArgumentException if blank or not a positive integer.
 */
long validateId(String id);

/**
 * Validate groupNumber is numeric, non-blank, and within [minGroupIndex, maxGroupIndex].
 */
void validateGroupNumber(String groupNumber);
```

Rules for `validateWager`:
- `dto` must not be null
- `dto.getWager()` must not be null
- `dto.getWager().getGameName()` must be non-blank
- `dto.getWager().getGameName().length()` must be ≤ 64
- `dto.getWagerName()` if non-null, length ≤ 255
- `dto.getGroupNumber()` must be non-blank and numeric
- `dto.getGroupNumber()` parsed int must be in `[properties.getMinGroupIndex(), properties.getMaxGroupIndex()]`

Rules for `validateGroup`:
- `dto` must not be null
- `dto.getGroupName()` must not be null (empty string is valid)
- `dto.getGroupName().length()` ≤ 255
- `dto.getGroupNumber()` if non-null, must be numeric and in `[minGroupIndex, maxGroupIndex]`

Rules for `validateId`:
- blank or null → throw
- not parseable as positive long → throw

**No Hibernate Validator, no `ConstraintViolation`. Plain `if` checks + `throw new IllegalArgumentException("message")`.**

#### 2.2 — Write `FavouritesValidatorTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesValidatorTest.java`

Create `FavouritesProperties` with defaults (minGroupIndex=1, maxGroupIndex=10, etc.) and pass to
the constructor — no Spring context.

Tests (one per rule):
- `validateWager_nullWager_throws`
- `validateWager_blankGameName_throws`
- `validateWager_gameNameTooLong_throws` (65 chars)
- `validateWager_wagerNameTooLong_throws` (256 chars)
- `validateWager_groupNumberNonNumeric_throws`
- `validateWager_groupNumberBelowMin_throws` (0)
- `validateWager_groupNumberAboveMax_throws` (11)
- `validateWager_valid_noException`
- `validateGroup_nullGroupName_throws`
- `validateGroup_groupNameTooLong_throws`
- `validateGroup_groupNumberNonNumeric_throws`
- `validateGroup_valid_noException`
- `validateId_blank_throws`
- `validateId_negative_throws`
- `validateId_valid_returnsLong`

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesValidatorTest.java
```

---

## Phase 3 — Write `PlayerContextResolver` and `CdcReader`

### Goal
`PlayerContextResolver` extracts `X-Player-Id` from the incoming request and throws
`IllegalArgumentException` on missing/blank. `CdcReader` reads `/run/mx/cdc` as an ASCII integer
and returns 0 when the file is absent.

### Files to read before starting
- `.planning/java25-spring-boot-migration.md` (PlayerContextResolver spec; CdcReader spec, CDC file location `/run/mx/cdc`)

### Tasks

#### 3.1 — Write `PlayerContextResolver.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java`

`@Component`.

```java
/**
 * Extracts the pre-authenticated player identity from the {@code X-Player-Id} HTTP header.
 * The gateway authenticates the player before forwarding; this service trusts the header.
 * Returns the player id string.
 * Throws IllegalArgumentException("Missing or blank X-Player-Id header") if absent or blank.
 *
 * Grep anchor: favourites
 */
public String resolve(HttpServletRequest request) {
    String playerId = request.getHeader("X-Player-Id");
    if (playerId == null || playerId.isBlank()) {
        throw new IllegalArgumentException("Missing or blank X-Player-Id header");
    }
    return playerId.strip();
}
```

#### 3.2 — Write `CdcReader.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/CdcReader.java`

`@Component`.

```java
/**
 * Reads the current draw counter from {@code /run/mx/cdc}.
 * The file contains a single ASCII integer optionally followed by a newline.
 * Returns 0 when the file is absent (test environments, dev machines).
 *
 * Grep anchor: favourites
 */
public int read() {
    Path path = Path.of("/run/mx/cdc");
    if (!Files.exists(path)) {
        return 0;
    }
    try {
        return Integer.parseInt(Files.readString(path).strip());
    } catch (IOException | NumberFormatException e) {
        return 0;
    }
}
```

#### 3.3 — Write tests

**`PlayerContextResolverTest.java`**  
Path: `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/PlayerContextResolverTest.java`

Use `MockHttpServletRequest`. Tests:
- `resolve_returnsPlayerId`
- `resolve_stripsWhitespace`
- `resolve_missingHeader_throws`
- `resolve_blankHeader_throws`

**`CdcReaderTest.java`**  
Path: `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/CdcReaderTest.java`

Tests:
- `read_fileAbsent_returnsZero` — use a `CdcReader` that reads from a temp path that doesn't exist
- `read_filePresent_returnsInteger` — write a temp file with "42\n", assert 42
- `read_filePresentNoNewline_returnsInteger` — write "7", assert 7

Override the file path for testing by making the path configurable (e.g. package-private setter or
constructor param) rather than hardcoded, so tests can inject a temp path. Keep the default
`/run/mx/cdc` as the production value via `@Value("${favourites.cdc-path:/run/mx/cdc}")`.

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/CdcReader.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/PlayerContextResolverTest.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/CdcReaderTest.java
```

---

## Phase 4 — Write `FavouritesService`

### Goal
`FavouritesService` interface + `DefaultFavouritesService` implement all 10 operations (5 wager +
5 group), fully unit-tested with mocked DAOs.

### Files to read before starting
- `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/DefaultFavoriteWagerService.java`
- `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/FavoriteWagerService.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java` (Phase 1 output)
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java` (Phase 2 output)
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesGroupDao.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesWagerDao.java`
- `czh-favorites-app/src/main/java/cz/bsl/favourites/config/FavouritesProperties.java`

### Tasks

#### 4.1 — Write `FavouritesService.java` (interface)

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java`

```java
/** Grep anchor: favourites */
public interface FavouritesService {

    // --- Wager operations ---

    /** Create and persist a favourite wager. Validates limits before insert. */
    FavouriteWagerDto createWager(FavouriteWagerDto dto, String playerId);

    /** Get a single wager by id. Throws NoSuchElementException if not found or not owned by player. */
    FavouriteWagerDto getWager(long wagerId, String playerId);

    /** Update mutable fields (wagerName, groupNumber, wager JSON) of an existing wager. */
    FavouriteWagerDto updateWager(long wagerId, FavouriteWagerDto dto, String playerId);

    /** Delete a wager by id. Throws NoSuchElementException if not found or not owned by player. */
    void deleteWager(long wagerId, String playerId);

    /**
     * List wagers for a player, optionally filtered by groupNumber and/or gameName(s).
     * Returns a page DTO (all results, no pagination cursor — totalCount = items.size()).
     */
    FavouriteWagerPageDto listWagers(String playerId, String groupNumber, List<String> gameNames);

    // --- Group operations ---

    /** Create a new favourite wager group. Enforces maxGroupIndex limit and duplicate check. */
    FavouriteGroupDto createGroup(FavouriteGroupDto dto, String playerId);

    /** Get a single group by groupNumber. Throws NoSuchElementException if not found. */
    FavouriteGroupDto getGroup(String groupNumber, String playerId);

    /** Update the group name. Throws NoSuchElementException if not found. */
    FavouriteGroupDto updateGroup(String groupNumber, FavouriteGroupDto dto, String playerId);

    /** Delete a group by groupNumber. Throws NoSuchElementException if not found. */
    void deleteGroup(String groupNumber, String playerId);

    /** List all groups for a player. */
    FavouriteGroupPageDto listGroups(String playerId);
}
```

#### 4.2 — Write `DefaultFavouritesService.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java`

`@Service`. Constructor-inject `FavouritesGroupDao groupDao`, `FavouritesWagerDao wagerDao`,
`FavouritesConverter converter`, `FavouritesValidator validator`, `FavouritesProperties properties`.

Key logic per operation (port from `DefaultFavoriteWagerService`):

**`createWager`:**
1. `validator.validateWager(dto)`
2. `validator.validateGroupNumber(dto.getGroupNumber())`
3. Check `wagerDao.countByPlayer(playerId) < properties.getMaxFavorites()` — throw `IllegalArgumentException` if exceeded
4. Check group exists: `groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber())` — if null, throw `NoSuchElementException("Group not found")`
5. `FavouriteWagerRecord record = converter.toRecord(dto, playerId)`
6. `wagerDao.insert(record)`
7. Return `converter.toDto(wagerDao.findById(record.getFavWagerId()))`

**`getWager`:**
1. `FavouriteWagerRecord r = wagerDao.findById(wagerId)` — null → `NoSuchElementException`
2. `!r.getPlayerId().equals(playerId)` → `NoSuchElementException` (same message — do not leak ownership)
3. Return `converter.toDto(r)`

**`updateWager`:**
1. Load existing record (same ownership check as `getWager`)
2. `validator.validateWager(dto)` 
3. Verify target group exists
4. Update record fields from dto: `wagerName`, `groupNumber`, `wagerJson` via `converter.toRecord`
5. `wagerDao.update(record)`
6. Return `converter.toDto(wagerDao.findById(wagerId))`

**`deleteWager`:**
1. Ownership check — throw `NoSuchElementException` if absent or wrong player
2. `wagerDao.delete(wagerId)`

**`listWagers`:**
1. If `gameNames` non-empty and `groupNumber` non-null: `wagerDao.findByPlayerAndGroup` then filter by game name in Java
2. If `groupNumber` non-null only: `wagerDao.findByPlayerAndGroup`
3. If `gameNames` non-empty only: iterate games, collect results (or `wagerDao.findByPlayer` + filter)
4. Otherwise: `wagerDao.findByPlayer`
5. Return `converter.toWagerPage(results)`

**`createGroup`:**
1. `validator.validateGroup(dto)`
2. `int count = groupDao.countByPlayer(playerId)` — `>= properties.getMaxGroupIndex()` → throw `IllegalArgumentException("Group limit exceeded")`
3. `groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber())` non-null → throw `IllegalArgumentException("Group already exists")`
4. `FavouriteGroupRecord record = converter.toRecord(dto, playerId)`
5. `groupDao.insert(record)` — let `DuplicateKeyException` propagate (race condition backstop)
6. Return `converter.toDto(groupDao.findByPlayerAndGroup(playerId, dto.getGroupNumber()))`

**`getGroup`:**
1. `groupDao.findByPlayerAndGroup(playerId, groupNumber)` — null → `NoSuchElementException`
2. Return `converter.toDto(record)`

**`updateGroup`:**
1. Load existing record — `NoSuchElementException` if absent
2. `validator.validateGroup(dto)` (name validation only)
3. `groupDao.updateName(record.getFavGroupId(), dto.getGroupName())`
4. Return `converter.toDto(groupDao.findByPlayerAndGroup(playerId, groupNumber))`

**`deleteGroup`:**
1. Load existing record — `NoSuchElementException` if absent
2. `groupDao.delete(record.getFavGroupId())`

**`listGroups`:**
1. `groupDao.findByPlayer(playerId)`
2. Return `converter.toGroupPage(records)`

Grep anchor: `favourites`

#### 4.3 — Write `DefaultFavouritesServiceTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/DefaultFavouritesServiceTest.java`

Use Mockito (`@ExtendWith(MockitoExtension.class)`). Mock `FavouritesGroupDao`, `FavouritesWagerDao`.
Use real `FavouritesConverter` (with `new ObjectMapper().registerModule(new JavaTimeModule())`).
Use real `FavouritesValidator` (with default `FavouritesProperties`).

Tests (one per operation + key error paths):
- `createWager_persistsAndReturnsDto`
- `createWager_limitExceeded_throws`
- `createWager_groupNotFound_throws`
- `getWager_found_returnsDto`
- `getWager_notFound_throwsNoSuchElement`
- `getWager_wrongPlayer_throwsNoSuchElement`
- `updateWager_updatesAndReturns`
- `deleteWager_deletesRow`
- `deleteWager_notFound_throws`
- `listWagers_noFilter_returnsAll`
- `listWagers_byGroup_filters`
- `createGroup_persistsAndReturns`
- `createGroup_limitExceeded_throws`
- `createGroup_duplicate_throws`
- `getGroup_found_returnsDto`
- `getGroup_notFound_throws`
- `updateGroup_updatesName`
- `deleteGroup_deletesRow`
- `listGroups_returnsPage`

```sh
~/bin/geany-progress done 4 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/DefaultFavouritesServiceTest.java
```

---

## Phase 5 — Post-SP sign-off

### Tasks

#### 5.1 — Full test + install (skip if norun active)

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

#### 5.2 — Coverage

```bash
bin/coverage.sh
```

Target ≥ 80% instruction coverage for all four new service classes. `CdcReader.read()` for the
`/run/mx/cdc` production path will not be covered in unit tests — accept this exception and note
it. `DefaultFavouritesService` all 10 operation methods must be covered.

#### 5.3 — Security

Run `/security-java` on new service files. Expected areas to check:

| Area | Expected result |
|------|----------------|
| `X-Player-Id` as SQL input via `playerId` parameter | All DAO calls use `?` placeholders — no risk |
| `wagerJson` size before DB write | `FavouritesConverter.toRecord` serialises `WagerDto` — add `if (json.length() > 30000) throw IllegalArgumentException` |
| `groupNumber` as path variable flows to DAO | `validateGroupNumber` is called before every DAO query — covered |
| `CdcReader` reading from configurable path | Path is `@Value`-injected and defaults to `/run/mx/cdc`; no user input controls the path |

For any HIGH finding: write failing test → fix → confirm green.

#### 5.4 — Refactor

Check `DefaultFavouritesService` for:
- Repeated ownership-check pattern (load + null check + player check) → extract `private FavouriteWagerRecord requireWager(long id, String playerId)` helper
- Same for groups → `private FavouriteGroupRecord requireGroup(String groupNumber, String playerId)`

Re-run tests after refactor.

#### 5.5 — Routing map

Run `/routing-map` to add `cz.bsl.czh.favourites.service` and `cz.bsl.czh.favourites.server`
packages to `.requirements/design/routing.md`.

#### 5.6 — Write progress state

```sh
~/bin/geany-progress done 5 \
  -r .requirements/design/routing.md \
  -r .planning/state/SP5_SERVICE_LAYER_PROGRESS.md
```
