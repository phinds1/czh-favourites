# SP3: DAO Layer — Detailed Implementation Plan

**Sub-plan:** SP3 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.czh.favourites`  
**Progress state:** `.planning/state/SP3_DAO_LAYER_PROGRESS.md`

## Overview

Write two db2delta scripts for `czh-favorites-db`, then implement `FavouritesGroupDao` and
`FavouritesWagerDao` in `czh-favorites-app` using `JdbcTemplate`. All SQL is declared as named
`private static final String` constants. The wager DAO stores and retrieves the raw `WAGER_JSON`
string; the caller (service layer, SP5) owns `ObjectMapper` round-trips. Each DAO has a JUnit 5
test against an H2 in-memory database. The SP is done when all tests pass and
`./mvn.sh install` is green.

## Context budget per phase

Each phase fits in one Claude Sonnet 4.6 context window. Read **only** the files listed under
**Files to read** before implementing a phase. Write progress state and confirm the build is green
before continuing to the next phase.

---

## Phase 1 — Write delta scripts

### Goal
Two db2delta scripts exist in `czh-favorites-db/src/main/delta/` with the exact DDL from the
migration plan.

### Files to read before starting
- `.planning/java25-spring-boot-migration.md` (DAO Layer section — exact DDL, db2delta format,
  FLAGS bitmask, WAGER_JSON spec)

### Tasks

#### 1.1 — Write `001-create-gis-fav-group.sql`

**Path:** `czh-favorites-db/src/main/delta/001-create-gis-fav-group.sql`

```sql
terminator=;
continue-on-error=false
author=czhdev
logging=debug

[changeset:create_fav_group_table]

CREATE TABLE GIS_FAV_GROUP (
    FAV_GROUP_ID   BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    PLAYER_ID      VARCHAR(64)   NOT NULL,
    GROUP_NUMBER   VARCHAR(10)   NOT NULL,
    GROUP_NAME     VARCHAR(255)  NOT NULL DEFAULT '',
    FLAGS          INTEGER       NOT NULL DEFAULT 0,
    CREATED_AT     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_FAV_GROUP UNIQUE (PLAYER_ID, GROUP_NUMBER)
);

[validation:fav_group_table_exists]

SELECT COUNT(*) FROM syscat.tables WHERE tabname='GIS_FAV_GROUP';
```

#### 1.2 — Write `002-create-gis-fav-wager.sql`

**Path:** `czh-favorites-db/src/main/delta/002-create-gis-fav-wager.sql`

```sql
terminator=;
continue-on-error=false
author=czhdev
logging=debug

[changeset:create_fav_wager_table]

CREATE TABLE GIS_FAV_WAGER (
    FAV_WAGER_ID   BIGINT         NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    PLAYER_ID      VARCHAR(64)    NOT NULL,
    GROUP_NUMBER   VARCHAR(10)    NOT NULL,
    GAME_NAME      VARCHAR(64)    NOT NULL,
    WAGER_NAME     VARCHAR(255)   NOT NULL DEFAULT '',
    FLAGS          INTEGER        NOT NULL DEFAULT 0,
    CREATED_AT     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    WAGER_JSON     VARCHAR(30000) NOT NULL
);

CREATE INDEX IDX_FAV_WAGER_PLAYER ON GIS_FAV_WAGER (PLAYER_ID);
CREATE INDEX IDX_FAV_WAGER_GROUP  ON GIS_FAV_WAGER (PLAYER_ID, GROUP_NUMBER);
CREATE INDEX IDX_FAV_WAGER_GAME   ON GIS_FAV_WAGER (PLAYER_ID, GAME_NAME);

[validation:fav_wager_table_exists]

SELECT COUNT(*) FROM syscat.tables WHERE tabname='GIS_FAV_WAGER';
```

No test for delta scripts in this phase — they are verified by the H2 schema tests in Phase 3.

---

## Phase 2 — Write `FavouritesGroupDao`

### Goal
`FavouritesGroupDaoImpl` compiles cleanly and all six DAO methods are implemented.

### Files to read before starting
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao/CzhPaymentDao.java`
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao/CzhPaymentDaoImpl.java`
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao/PaymentRowMapper.java`
- `czh-favorites-db/src/main/delta/001-create-gis-fav-group.sql` (column names)

### Tasks

#### 2.1 — Write `FavouriteGroupRecord.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteGroupRecord.java`

Plain Java domain object (not a DTO — no Jackson annotations):
- `favGroupId` — `Long` (null before insert; set after)
- `playerId` — `String`
- `groupNumber` — `String`
- `groupName` — `String`
- `flags` — `int`
- `createdAt` — `java.time.Instant`
- `updatedAt` — `java.time.Instant`

Standard getters/setters. No-arg constructor.

#### 2.2 — Write `FavouritesGroupDao.java` (interface)

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesGroupDao.java`

```java
public interface FavouritesGroupDao {
    /** Insert; returns generated FAV_GROUP_ID. Throws DuplicateKeyException on duplicate (PLAYER_ID, GROUP_NUMBER). */
    long insert(FavouriteGroupRecord group);

    /** All groups for a player, ordered by GROUP_NUMBER. */
    List<FavouriteGroupRecord> findByPlayer(String playerId);

    /** Single group by player + groupNumber. Returns null if absent. */
    FavouriteGroupRecord findByPlayerAndGroup(String playerId, String groupNumber);

    /** Update GROUP_NAME. Sets UPDATED_AT = CURRENT_TIMESTAMP. Returns rows updated. */
    int updateName(long favGroupId, String groupName);

    /** Delete by id. Returns rows deleted. */
    int delete(long favGroupId);

    /** Count of groups for a player. */
    int countByPlayer(String playerId);
}
```

#### 2.3 — Write `FavouritesGroupDaoImpl.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesGroupDaoImpl.java`

Follow `CzhPaymentDaoImpl` exactly:
- `@Repository`
- Constructor-inject `JdbcTemplate jdbcTemplate`
- SQL as `private static final String` constants:

```java
private static final String INSERT =
    "INSERT INTO GIS_FAV_GROUP (PLAYER_ID, GROUP_NUMBER, GROUP_NAME, FLAGS) VALUES (?,?,?,?)";

private static final String FIND_BY_PLAYER =
    "SELECT * FROM GIS_FAV_GROUP WHERE PLAYER_ID = ? ORDER BY GROUP_NUMBER";

private static final String FIND_BY_PLAYER_AND_GROUP =
    "SELECT * FROM GIS_FAV_GROUP WHERE PLAYER_ID = ? AND GROUP_NUMBER = ?";

private static final String UPDATE_NAME =
    "UPDATE GIS_FAV_GROUP SET GROUP_NAME=?, UPDATED_AT=CURRENT_TIMESTAMP WHERE FAV_GROUP_ID=?";

private static final String DELETE =
    "DELETE FROM GIS_FAV_GROUP WHERE FAV_GROUP_ID=?";

private static final String COUNT_BY_PLAYER =
    "SELECT COUNT(*) FROM GIS_FAV_GROUP WHERE PLAYER_ID=?";
```

`insert()` uses `GeneratedKeyHolder` with `new String[]{"FAV_GROUP_ID"}` (same pattern as
`CzhPaymentDaoImpl.save()`).

Inner static class `GroupRowMapper implements RowMapper<FavouriteGroupRecord>`:
- Maps all columns; `CREATED_AT` / `UPDATED_AT` → `rs.getTimestamp("CREATED_AT").toInstant()`
- Both columns are `NOT NULL DEFAULT` so no null-check needed (add a comment)

`findByPlayerAndGroup` wraps `queryForObject` in a try/catch for `EmptyResultDataAccessException`
and returns null — same as `CzhPaymentDaoImpl.findById`.

#### 2.4 — Build verify (compile only, no tests yet)

```bash
./mvn.sh -pl czh-favorites-app compile
```

---

## Phase 3 — Write `FavouritesWagerDao`

### Goal
`FavouritesWagerDaoImpl` compiles cleanly and all eight DAO methods are implemented.

### Files to read before starting
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao/CzhPaymentDaoImpl.java`
- `czh-favorites-db/src/main/delta/002-create-gis-fav-wager.sql` (column names, indexes)
- `czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java` (SP2 — for Javadoc)

### Tasks

#### 3.1 — Write `FavouriteWagerRecord.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteWagerRecord.java`

Plain Java domain object:
- `favWagerId` — `Long` (null before insert)
- `playerId` — `String`
- `groupNumber` — `String`
- `gameName` — `String`
- `wagerName` — `String`
- `flags` — `int`
- `createdAt` — `java.time.Instant`
- `updatedAt` — `java.time.Instant`
- `wagerJson` — `String` (raw JSON string; caller serialises/deserialises `WagerDto`)

Standard getters/setters. No-arg constructor.

#### 3.2 — Write `FavouritesWagerDao.java` (interface)

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesWagerDao.java`

```java
public interface FavouritesWagerDao {
    /** Insert; returns generated FAV_WAGER_ID. */
    long insert(FavouriteWagerRecord wager);

    /** Find by id. Returns null if absent. */
    FavouriteWagerRecord findById(long favWagerId);

    /** All wagers for a player, ordered by FAV_WAGER_ID. */
    List<FavouriteWagerRecord> findByPlayer(String playerId);

    /** All wagers for a player in a specific group, ordered by FAV_WAGER_ID. */
    List<FavouriteWagerRecord> findByPlayerAndGroup(String playerId, String groupNumber);

    /** All wagers for a player in a specific game, ordered by FAV_WAGER_ID. */
    List<FavouriteWagerRecord> findByPlayerAndGame(String playerId, String gameName);

    /** Update mutable columns (wagerName, flags, wagerJson). Sets UPDATED_AT. Returns rows updated. */
    int update(FavouriteWagerRecord wager);

    /** Delete by id. Returns rows deleted. */
    int delete(long favWagerId);

    /** Total wager count for a player. */
    int countByPlayer(String playerId);
}
```

#### 3.3 — Write `FavouritesWagerDaoImpl.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesWagerDaoImpl.java`

- `@Repository`
- Constructor-inject `JdbcTemplate jdbcTemplate`
- SQL constants:

```java
private static final String INSERT =
    "INSERT INTO GIS_FAV_WAGER " +
    "(PLAYER_ID, GROUP_NUMBER, GAME_NAME, WAGER_NAME, FLAGS, WAGER_JSON) " +
    "VALUES (?,?,?,?,?,?)";

private static final String FIND_BY_ID =
    "SELECT * FROM GIS_FAV_WAGER WHERE FAV_WAGER_ID=?";

private static final String FIND_BY_PLAYER =
    "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? ORDER BY FAV_WAGER_ID";

private static final String FIND_BY_PLAYER_AND_GROUP =
    "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? AND GROUP_NUMBER=? ORDER BY FAV_WAGER_ID";

private static final String FIND_BY_PLAYER_AND_GAME =
    "SELECT * FROM GIS_FAV_WAGER WHERE PLAYER_ID=? AND GAME_NAME=? ORDER BY FAV_WAGER_ID";

private static final String UPDATE =
    "UPDATE GIS_FAV_WAGER SET WAGER_NAME=?, FLAGS=?, WAGER_JSON=?, " +
    "UPDATED_AT=CURRENT_TIMESTAMP WHERE FAV_WAGER_ID=?";

private static final String DELETE =
    "DELETE FROM GIS_FAV_WAGER WHERE FAV_WAGER_ID=?";

private static final String COUNT_BY_PLAYER =
    "SELECT COUNT(*) FROM GIS_FAV_WAGER WHERE PLAYER_ID=?";
```

`insert()` uses `GeneratedKeyHolder` with `new String[]{"FAV_WAGER_ID"}`.

`findById()` wraps `queryForObject` in try/catch `EmptyResultDataAccessException` → return null.

Inner static class `WagerRowMapper implements RowMapper<FavouriteWagerRecord>`: maps all columns
including `WAGER_JSON` as a plain `String`.

#### 3.4 — Build verify (compile only)

```bash
./mvn.sh -pl czh-favorites-app compile
```

---

## Phase 4 — Write DAO tests

### Goal
`FavouritesGroupDaoTest` and `FavouritesWagerDaoTest` pass against H2 in-memory.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesGroupDaoImpl.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesWagerDaoImpl.java`
- `czh-favorites-app/src/test/resources/application-test.yml` (H2 datasource properties — from SP1)
- `/java/czh/czh-money/money-app/src/test/java/cz/bsl/money/dao/CzhPaymentDaoCrudTest.java` (test style reference)

### Tasks

#### 4.1 — H2-compatible DDL note

DB2 uses `GENERATED ALWAYS AS IDENTITY`. H2 does not support `GENERATED ALWAYS` for inserts — use
`GENERATED BY DEFAULT AS IDENTITY` in the H2 test schema. Write the H2 DDL as constants directly
in the test config class, **not** by running the delta scripts (which contain DB2-specific syntax
and db2delta headers).

#### 4.2 — Write shared test database config

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/dao/DaoTestConfig.java`

```java
// @Configuration — shared by FavouritesGroupDaoTest and FavouritesWagerDaoTest
// Beans: DataSource (H2 in-memory, DB_CLOSE_DELAY=-1), JdbcTemplate, ObjectMapper
// @PostConstruct or CommandLineRunner: runs CREATE TABLE statements for GIS_FAV_GROUP and GIS_FAV_WAGER
// Use GENERATED BY DEFAULT AS IDENTITY for H2 compatibility
// Drop tables first (DROP TABLE IF EXISTS) so tests that create a fresh context start clean
```

H2 CREATE TABLE for `GIS_FAV_GROUP`:
```sql
DROP TABLE IF EXISTS GIS_FAV_GROUP;
CREATE TABLE GIS_FAV_GROUP (
    FAV_GROUP_ID   BIGINT        GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    PLAYER_ID      VARCHAR(64)   NOT NULL,
    GROUP_NUMBER   VARCHAR(10)   NOT NULL,
    GROUP_NAME     VARCHAR(255)  NOT NULL DEFAULT '',
    FLAGS          INTEGER       NOT NULL DEFAULT 0,
    CREATED_AT     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_FAV_GROUP UNIQUE (PLAYER_ID, GROUP_NUMBER)
);
```

H2 CREATE TABLE for `GIS_FAV_WAGER`:
```sql
DROP TABLE IF EXISTS GIS_FAV_WAGER;
CREATE TABLE GIS_FAV_WAGER (
    FAV_WAGER_ID   BIGINT         GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    PLAYER_ID      VARCHAR(64)    NOT NULL,
    GROUP_NUMBER   VARCHAR(10)    NOT NULL,
    GAME_NAME      VARCHAR(64)    NOT NULL,
    WAGER_NAME     VARCHAR(255)   NOT NULL DEFAULT '',
    FLAGS          INTEGER        NOT NULL DEFAULT 0,
    CREATED_AT     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    WAGER_JSON     VARCHAR(30000) NOT NULL
);
```

#### 4.3 — Write `FavouritesGroupDaoTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/dao/FavouritesGroupDaoTest.java`

Use `@SpringJUnitConfig(DaoTestConfig.class)`. Inject `FavouritesGroupDaoImpl` directly (or the
interface — wire a bean in `DaoTestConfig`). Each test starts with a clean table (use
`@BeforeEach` to `DELETE FROM GIS_FAV_GROUP`).

Tests:
- `insert_generatesId`: insert a group, assert returned id > 0
- `findByPlayer_returnsAllOrderedByGroupNumber`: insert groups "3", "1", "2"; find → assert order
- `findByPlayerAndGroup_returnsCorrectGroup`: insert 2 groups, find by player+groupNumber → assert all fields
- `findByPlayerAndGroup_returnsNullWhenAbsent`: find for unknown combination → assert null
- `insert_throwsDuplicateKeyOnDuplicateGroupNumber`: insert same player+groupNumber twice → expect `DuplicateKeyException`
- `updateName_changesGroupName`: insert, updateName, findByPlayerAndGroup → assert new name; assert UPDATED_AT ≥ CREATED_AT
- `delete_removesRow`: insert, delete, findByPlayer → empty list
- `countByPlayer_countsOnlyTargetPlayer`: insert 3 for player A, 1 for player B; countByPlayer(A) = 3

#### 4.4 — Write `FavouritesWagerDaoTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/dao/FavouritesWagerDaoTest.java`

Same setup as group test. Each test `@BeforeEach` deletes from `GIS_FAV_WAGER`.

Tests:
- `insert_generatesId`: insert wager, assert id > 0
- `findById_returnsWager`: insert, findById → assert all scalar fields and raw wagerJson string
- `findById_returnsNullWhenAbsent`: findById(999) → null
- `findByPlayer_returnsAllOrderedById`: insert 2 wagers, findByPlayer → ordered by FAV_WAGER_ID
- `findByPlayerAndGroup_filtersCorrectly`: insert wagers in group "1" and group "2"; findByPlayerAndGroup(group "1") returns only group 1 wagers
- `findByPlayerAndGame_filtersCorrectly`: insert wagers for game "LOTTO" and "KENO"; filter by "LOTTO" returns only LOTTO
- `update_changesFields`: insert, call update with new wagerName + flags + wagerJson, findById → assert updated values; UPDATED_AT ≥ CREATED_AT
- `delete_removesRow`: insert, delete, findById → null
- `countByPlayer_countsOnlyTargetPlayer`: 4 for player A, 1 for B; count(A) = 4
- `wagerJsonRoundTrip`: build a `WagerDto` with 2 board stacks, serialize to JSON string, insert as `wagerJson`, retrieve, deserialize back to `WagerDto`, assert all fields equal (boards, gameName, stake, price, duration)

For `wagerJsonRoundTrip`: inject `ObjectMapper` from `DaoTestConfig`; use
`objectMapper.writeValueAsString(wagerDto)` and `objectMapper.readValue(record.getWagerJson(), WagerDto.class)`.

#### 4.5 — Build and test verify

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

All tests green before Phase 5.

---

## Phase 5 — Post-SP sign-off

### Tasks

#### 5.1 — Full test + install

```bash
./mvn.sh -pl czh-favorites-api,czh-favorites-app test
./mvn.sh install
```

Zero failures. Zero `@Disabled`.

#### 5.2 — Coverage

```bash
bin/coverage.sh
```

Read `target/jacoco-report.csv`. Target ≥ 80% instruction coverage for new DAO classes.
If below threshold, add missing tests (e.g. `findById` null path, `update` returning 0 rows).
Record accepted exceptions (e.g. `FavouriteGroupRecord` / `FavouriteWagerRecord` getters not
covered if only the insert path is tested — cover by adding a read test).

#### 5.3 — Security

Run `/security-java` on new DAO files. Triage:

| Risk | Response |
|------|----------|
| SQL injection | Not applicable — all queries use `?` placeholders; note in code |
| `WAGER_JSON` size > 30 000 chars | Validation is the service layer's responsibility (SP5); DAO stores what it receives |
| NPE on `rs.getTimestamp(...).toInstant()` | `CREATED_AT` / `UPDATED_AT` are `NOT NULL DEFAULT` — no NPE possible; add comment |
| ObjectMapper in test deserialising untrusted data | Test-only; no action needed |

For any HIGH finding: write failing test, apply fix, confirm green.

#### 5.4 — Refactor

Check for:
- Duplicated try/catch `EmptyResultDataAccessException` → extract to a shared private helper if
  it appears in both DAOs
- `GeneratedKeyHolder` boilerplate repeated → acceptable given two DAOs; no extraction needed
- Any leftover legacy imports or dead code

Re-run `./mvn.sh -pl czh-favorites-app test` after any change.

#### 5.5 — Routing map

Run `/routing-map` to add the new `cz.bsl.czh.favourites.dao` package components to
`.requirements/design/routing.md`.

#### 5.6 — Write progress state

Update `.planning/state/SP3_DAO_LAYER_PROGRESS.md` to mark Phase 5 complete and SP3 DONE.
