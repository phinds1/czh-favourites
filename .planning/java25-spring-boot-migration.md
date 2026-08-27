# Java 6 Migration — czh-favourites

This project is a Spring Boot 4 microservice that stores players' favourite wagers and makes them available for replay.

It is ported from the Java 6 GIS monolith available for reading and copying in `./references/gis/`. Significant redesign is required because the original implementation is tightly coupled to GIS internal services (Hibernate ORM, JBoss RESTEasy, JMX, Guava, Joda Time) and runs inside the GIS monolithic application server.

**Sanename:** `favourites`  
All code, test files, and log messages referring to this feature should use the word `favourites` so that `grep favourites` finds everything related to this microservice.

`./references` includes:
- `gis` — the source monolith containing the features we are porting
- `czh-money` — a different GIS component already ported to Java 25 / Spring Boot 4; use as a pattern reference

Current problem with production: the favourites feature is tightly coupled to the GIS server and cannot be scaled, deployed, or tested independently.

---

## Core classes to port

Core code lives in the `czh-favorites-app` sub-module under package `cz.bsl.czh.favourites`.

### Service layer

| Legacy class                       | New class                                                     | Notes                                                                                                                                                                                                                                                                        |
|------------------------------------|---------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DefaultFavoriteWagerService`      | `cz.bsl.czh.favourites.service.FavouritesService`             | Port business logic; replace `@ManagedResource`/`@ManagedAttribute` (JMX) with `@ConfigurationProperties`; replace Guava `FluentIterable`/`Ordering` with Java streams; remove `DTOValidator` / Hibernate `ConstraintViolation` — replace with plain Java validation methods |
| `FavoriteWagerService` (interface) | `cz.bsl.czh.favourites.service.FavouritesService` (interface) | Slim down; keep the six wager operations and four group operations                                                                                                                                                                                                           |
| `DefaultFavoriteWagerConverter`    | `cz.bsl.czh.favourites.service.FavouritesConverter`           | Converts between JSON API DTOs and domain objects; replace Joda `DateTime` with `java.time.Instant`; replace Guava with standard Java                                                                                                                                        |
| `FavoriteWagerValidator`           | `cz.bsl.czh.favourites.service.FavouritesValidator`           | Port all validation rules as plain Java; no Hibernate Validator                                                                                                                                                                                                              |

**Configuration properties** (replacing JMX-managed setters in `DefaultFavoriteWagerService`):

```yaml
favourites:
  min-group-index: 1
  max-group-index: 10
  default-group-name: ""
  max-favorites: 50
  max-favorite-boards: 200
  keno-reserved-group-index: 1
  return-empty-groups: false
```

`ARTEGameHostServiceImpl` is **not** ported. Current time comes from `java.time.Instant.now()`. CDC (current draw counter) is read from `/run/mx/cdc` which contains a single ASCII integer optionally followed by `\n`. Provide a `CdcReader` service that reads this file; stub it to return `0` when the file is absent in test environments.

Wagering is **not** part of this microservice. `WagerMinifier.expand()` (which populated game-default fields on the wager DTO) is dropped — the caller is responsible for sending complete wager data. The `GameHostService`, `GameServiceRegistry`, and `GameDefinition` dependencies are eliminated; game identity is carried directly in the stored JSON.

The `PlayerAuthentication` and `PlayerContextFactory` classes are replaced by a `PlayerContextResolver` that extracts the player ID from the pre-authenticated HTTP header `X-Player-Id`. No outbound authentication calls are made.

---

## Quartz jobs

The original GIS application had no Quartz jobs associated with the favourites feature. There are **no scheduled jobs** to port.

---

## JSON API

The relevant DTOs from `gateway-draw-games-rest-api-2.6.43.0.jar` are:

| Legacy DTO                  | New class                                     | Location                   |
|-----------------------------|-----------------------------------------------|----------------------------|
| `FavoriteWagerDTO`          | `cz.bsl.favourites.api.FavouriteWagerDto`     | `czh-favorites-api` module |
| `FavoriteWagerGroupDTO`     | `cz.bsl.favourites.api.FavouriteGroupDto`     | `czh-favorites-api` module |
| `FavoriteWagerPageDTO`      | `cz.bsl.favourites.api.FavouriteWagerPageDto` | `czh-favorites-api` module |
| `FavoriteWagerGroupPageDTO` | `cz.bsl.favourites.api.FavouriteGroupPageDto` | `czh-favorites-api` module |

Copy only these four DTOs. Change all package names to `cz.bsl.czh.favourites.api`. Remove all JAX-RS, Jackson 1.x, and Joda Time imports; use Jackson 2 (`com.fasterxml.jackson.annotation`) and `java.time`. The `WagerDTO` (nested inside `FavoriteWagerDTO`) must also be copied and repackaged; it carries `gameName`, `stake`, `price`, `duration`, `serialNumber`, and `boards`.

---

## DAO Layer

The database remains IBM DB2 in production, H2 in test.

The legacy DAO layer uses Hibernate ORM with three normalised tables: `CZH_FAVORITE_WAGER_GRP` (groups), `CZH_FAVORITE_WAGER` (wagers), and `CZH_FAVORITE_BOARD_STACK` + `CZH_FAVORITE_BOARD` (board detail). **This 3NF design is replaced by a single JSON document table.**

### New table design

Two tables replace the four legacy tables:

**`GIS_FAV_GROUP`** — favourite wager groups  
Each group is a lightweight record; no wager data is stored here.

```sql
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
```

**`GIS_FAV_WAGER`** — favourite wagers (JSON document store)  
Indexed columns are promoted out of the JSON blob. The complete wager structure (board stacks, boards, picks, stake, draws, etc.) is serialised as JSON into `WAGER_JSON`.

```sql
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
```

`WAGER_JSON` holds the serialised `FavouriteWagerDto` wager sub-object (game name, stake, price, number of draws, board stacks and boards). The JSON must deserialise cleanly back to a `FavouriteWagerDto` with no loss.

`FLAGS` is a 32-bit bitmask reserved for future soft-delete / status bits (bit 0 = deleted).

### DAO classes

| New class                                      | Responsibility                                                                |
|------------------------------------------------|-------------------------------------------------------------------------------|
| `cz.bsl.czh.favourites.dao.FavouritesGroupDao` | CRUD on `GIS_FAV_GROUP` via `JdbcTemplate`                                    |
| `cz.bsl.czh.favourites.dao.FavouritesWagerDao` | CRUD on `GIS_FAV_WAGER` via `JdbcTemplate`; serialises/deserialises JSON blob |

All SQL is written as readable named-constant strings in the DAO class (no `NamedParameterJdbcTemplate` magic strings scattered in service code). The `FavouritesWagerDao.getFavoritesCount(playerId)` and `getFavoriteBoardsCount(playerId)` methods are replaced by a single `countWagers(playerId)` — the board-level count from the legacy code was a proxy for the same limit and is simplified.

---

## HTTP endpoints

One Spring MVC `@RestController` replaces the legacy JAX-RS `PlayerFavoriteWagerResource`.

**Controller:** `cz.bsl.favourites.web.FavouritesController`  
**Base path:** `/favourites`

All methods require the header `X-Player-Id` (player authentication, pre-authenticated by the gateway).

### Wager endpoints

| Method   | Path                                                      | Legacy method         | Auth header required |
|----------|-----------------------------------------------------------|-----------------------|----------------------|
| `POST`   | `/favourites/wagers`                                      | `createFavoriteWager` | `X-Player-Id`        |
| `GET`    | `/favourites/wagers/{id}`                                 | `getFavoriteWager`    | `X-Player-Id`        |
| `PUT`    | `/favourites/wagers/{id}`                                 | `updateFavoriteWager` | `X-Player-Id`        |
| `DELETE` | `/favourites/wagers/{id}`                                 | `deleteFavoriteWager` | `X-Player-Id`        |
| `GET`    | `/favourites/wagers?group={groupNumber}&game-names={csv}` | `getFavoriteWagers`   | `X-Player-Id`        |

### Group endpoints

| Method   | Path                               | Legacy method                   | Auth header required |
|----------|------------------------------------|---------------------------------|----------------------|
| `POST`   | `/favourites/groups`               | `createFavoriteWagerGroup`      | `X-Player-Id`        |
| `GET`    | `/favourites/groups/{groupNumber}` | `getFavoriteWagerGroup`         | `X-Player-Id`        |
| `PUT`    | `/favourites/groups/{groupNumber}` | `updateFavoriteWagerGroup`      | `X-Player-Id`        |
| `DELETE` | `/favourites/groups/{groupNumber}` | `deleteFavoriteWagerGroup`      | `X-Player-Id`        |
| `GET`    | `/favourites/groups`               | `getFavoriteWagerGroupss` (sic) | `X-Player-Id`        |

### Validation rules (port from `FavoriteWagerValidator`)

- `id` / `groupNumber` parameters must be non-null, non-blank, parseable as a positive integer
- `groupNumber` must be between `minGroupIndex` (1) and `maxGroupIndex` (10) inclusive
- `playerId` extracted from `X-Player-Id` must be non-null and non-blank
- On create wager: enforce `maxFavorites` limit per player
- On create group: enforce `maxGroupIndex` groups per player; reject duplicate `groupNumber` for same player
- `FavouriteWagerDto.wager.gameName` must be non-blank
- Return HTTP 400 for validation errors, HTTP 404 when a wager/group is not found or belongs to another player

`WagerMinifier.expand()` calls from the legacy resource are **dropped** — the new microservice stores and returns the exact wager JSON it receives. The caller must send complete data.

---

## Jamcrest testing

HTTP resource testing uses Spring's `MockMvc` (or `TestRestTemplate`) and Jamcrest for JSON assertion, as per `/jamcrest-testing`.

### AbstractRestTest setup

- Starts Spring Boot test context with H2 in-memory database (`spring.profiles.active=test`)
- Provides helper methods `asPlayer(playerId)` to set `X-Player-Id` header on requests
- Detects when a `*.resp.js` file does not exist and prints the actual JSON to stdout so it can be captured as the baseline
- `*.req.js` request templates live under `src/test/resources/favourites/`

### Tests to write

Every endpoint method in `FavouritesController` needs a Jamcrest test:

| Test class                    | Test methods                                                                                             |
|-------------------------------|----------------------------------------------------------------------------------------------------------|
| `FavouritesWagerResourceTest` | `createWager`, `getWager`, `updateWager`, `deleteWager`, `listWagersByGroup`, `listWagersByGroupAndGame` |
| `FavouritesGroupResourceTest` | `createGroup`, `getGroup`, `updateGroup`, `deleteGroup`, `listGroups`                                    |
| `FavouritesValidationTest`    | invalid id, missing player header, group limit exceeded, wager limit exceeded, duplicate group           |

The legacy GIS codebase has no surviving test resources with wager JSON examples in `references/gis`. Request/response `.req.js` and `.resp.js` fixtures must be written from scratch based on the DTO structure.

### H2 test database

The H2 schema is initialised by a `TestDatabaseConfig` Spring `@Configuration` that only loads when `spring.profiles.active=test`. It applies the delta scripts from `czh-favorites-db/src/main/delta/` by invoking the `db2delta` tool.

Delta script location: `czh-favorites-db/src/main/delta/`

```
terminator=;
continue-on-error=false
author=Alice
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

[validation:fav_wager_table_exists]

SELECT COUNT(*) FROM syscat.tables WHERE tabname='GIS_FAV_WAGER';
```




