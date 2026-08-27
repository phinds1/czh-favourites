# czh-favourites Migration — Plan Index

This document divides the `czh-favourites` Spring Boot 4 / Java 25 microservice build into
discrete sub-plans. Each sub-plan is scoped to fit in a single Claude Sonnet 4.6 context window
for detailed planning and implementation. Sub-plans are ordered by dependency: later plans depend
on earlier ones, though SP6 and SP7 can be worked in parallel once SP4 and SP5 are complete.

The sanename for this project is **`favourites`**. Every class, log message, test file, and grep
anchor must use the word `favourites` so that `grep favourites` finds everything related to this
microservice.

---

## Sub-Plan Summary Table

| ID   | Title                 | Key output                                                                     | Depends on |
|------|-----------------------|--------------------------------------------------------------------------------|------------|
| SP1  | Project Scaffold      | Root POM modules fixed, Spring Boot entry, config, JaCoCo, coverage            | —          |
| SP2  | JSON API Models       | `czh-favorites-api` DTOs: wager, group, page wrappers, WagerDto                | SP1        |
| SP3  | DAO Layer             | `GIS_FAV_GROUP` + `GIS_FAV_WAGER`, JdbcTemplate DAOs, delta scripts            | SP1        |
| SP4  | Test Infrastructure   | `AbstractRestTest`, `TestDatabaseConfig` H2, `PlayerContextResolver`           | SP2,SP3    |
| SP5  | Service Layer         | `FavouritesService`, `FavouritesConverter`, `FavouritesValidator`, `CdcReader` | SP2,SP3    |
| SP6  | Favourites Controller | `FavouritesController` (wager + group endpoints), Jamcrest tests               | SP4,SP5    |
| SP7  | Admin API             | `AdminFavouritesController` (read-only admin view), Jamcrest tests             | SP4,SP5    |
| SP8  | Prometheus Metrics    | Health, request-count, success/failure counters wired to service               | SP6,SP7    |
| SP9  | Deployment            | App RPM, DB RPM, GitHub Actions YAML, Azure DevOps YAML, systemd               | SP8        |
| SP10 | Vision GUI            | Dark-theme operator GUI: Server, Operations, Player Lookup tabs                | SP8        |

---

## Post-SP sign-off sequence

After `/opus-exec` completes each sub-plan, run the following before marking it done. The detailed
`SPX_*_PLAN.md` (written by `/opus-plan`) must carry these as its final phases.

1. **Test** — run `./mvn.sh -pl <module> test`, then `./mvn.sh install`. Every test must pass;
   no `@Disabled` or red tests at sign-off. Run Jamcrest integration tests; confirm no
   `suggestJsonDefinition()` output remains in `target/ai.log`.
2. **Coverage** — run `bin/coverage.sh`. Read `target/jacoco-report.csv`. Target 80% instruction
   coverage. Write tests for any uncovered business-logic class. Record accepted exceptions
   (Spring wiring, `main()` entry) in progress notes.
3. **Security** — run `/security-java` on Java added in this SP. Triage recommendations. For each
   accepted finding: write a failing test first, apply the fix, confirm green.
4. **Refactor** — pull common code up, remove dead code, verify comments still match code. Re-run
   step 1 after refactoring.
5. **Routing map** — update `.requirements/design/routing.md` via `/routing-map` to reflect new
   sanenamed components.

Record sign-off in `.planning/state/SPX_<SLUG>_PROGRESS.md`.

---
## SP1: Project Scaffold

### Scope
Fix the root POM module list (currently declares `dgsubs-*` placeholder modules), wire the three
real modules (`czh-favorites-api`, `czh-favorites-app`, `czh-favorites-db`), write the Spring Boot
4 application entry point, `application.yml`, `DataSourceConfig`, `logback-spring.xml`, and
`bin/coverage.sh`. JaCoCo `prepare-agent` is already declared in the root POM — confirm it is
active and producing `jacoco.exec` per module. Ship `bin/coverage.sh` modelled on
`/java/czh/czh-money/bin/coverage.sh` but referencing the `czh-favorites-*` modules.

### Why this is a unit
Pure project skeleton — no business logic. Must compile end-to-end (`./mvn.sh install`) before
any feature work begins. Coverage tooling is a scaffold deliverable so every subsequent SP's tests
are measured from the start.

### Key reference files

| File                                                                                    | Purpose                                                            |
|-----------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `/java/czh/czh-favourites/pom.xml`                                                      | Root POM — JaCoCo already wired; fix `<modules>` section           |
| `/java/czh/czh-money/pom.xml`                                                           | Exact POM structure to mirror (3 modules, BOM, JaCoCo)             |
| `/java/czh/czh-money/money-app/src/main/resources/application.yml`                      | Config style: port, datasource profiles, actuator                  |
| `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/MoneyApplication.java`        | `@SpringBootApplication` entry point style                         |
| `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/config/DataSourceConfig.java` | HikariCP + DB2/H2 profile-switching pattern                        |
| `/java/czh/czh-money/bin/coverage.sh`                                                   | Coverage script to adapt for `czh-favorites-*` modules             |
| `.requirements/design/hld.md`                                                           | Deployment, monitoring, state design decisions                     |
| `.planning/java25-spring-boot-migration.md`                                             | Sanename, package name, config properties block, CDC file location |

### Key classes / files to create

```
pom.xml                                        # fix <modules>: czh-favorites-api, czh-favorites-app, czh-favorites-db

czh-favorites-api/
  pom.xml                                      # packaging=jar, no SB dependency

czh-favorites-app/
  pom.xml                                      # SB4 executable jar, depends on czh-favorites-api
  src/main/java/cz/bsl/czh/favourites/
    FavouritesApplication.java                 # @SpringBootApplication entry point
    config/
      DataSourceConfig.java                    # HikariCP + DB2 prod / H2 test profile switching
  src/main/resources/
    application.yml                            # port 9290, datasource, actuator/prometheus, favourites.*
    logback-spring.xml

czh-favorites-db/
  pom.xml                                      # packaging=jar (holds delta SQL only, written in SP3)

bin/
  coverage.sh                                  # merge jacoco.exec across all 3 modules, emit CSV
```

### Dependencies on other sub-plans
None — this is the foundation.

### Suggested detailed-plan prompt

> Read the following before planning:
> - `/java/czh/czh-favourites/pom.xml` (current root POM — note the incorrect `<modules>` and existing JaCoCo config)
> - `/java/czh/czh-money/pom.xml` (root POM shape to follow)
> - `/java/czh/czh-money/money-app/src/main/resources/application.yml` (config style)
> - `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/config/DataSourceConfig.java` (datasource pattern)
> - `/java/czh/czh-money/bin/coverage.sh` (coverage script to adapt)
> - `.requirements/design/hld.md`
> - `.planning/java25-spring-boot-migration.md` (sanename=`favourites`, package=`cz.bsl.favourites`,
>   config property block under `favourites:`, CDC file at `/run/mx/cdc`)
>
> Create a detailed implementation plan for SP1 Project Scaffold:
> - Fix `<modules>` in root `pom.xml` to list `czh-favorites-api`, `czh-favorites-app`, `czh-favorites-db`
> - Write `czh-favorites-api/pom.xml` (jar, inherits root, no SB dependency)
> - Write `czh-favorites-app/pom.xml` (SB4 executable jar, depends on czh-favorites-api)
> - Write `FavouritesApplication.java` in package `cz.bsl.favourites`
> - Write `application.yml` for port 9290 (app) / 9291 (management), datasource (DB2 prod / H2 test),
>   actuator/prometheus, and the `favourites.*` config properties from the migration doc
> - Write `DataSourceConfig.java` wiring HikariCP to DB2 (prod) or H2 (test profile)
> - Write `bin/coverage.sh` adapting `/java/czh/czh-money/bin/coverage.sh` to reference `czh-favorites-*` modules
> - Confirm JaCoCo `prepare-agent` execution is present and active in root POM (it is; just verify)
> - Confirm `./mvn.sh install` compiles cleanly
> Each step must include a unit test where applicable (smoke test for DataSourceConfig at minimum).

---
## SP2: JSON API Models

### Scope
Port the four legacy DTOs from `gateway-draw-games-rest-api` to the `czh-favorites-api` module,
clean of all JAX-RS, Jackson 1.x, and Joda Time. Copy `WagerDto` (the nested wager sub-object)
from the legacy `WagerDTO`. All classes land in `cz.bsl.favourites.api`. Add Jackson 2
annotations and `java.time.Instant` where dates occur. No Hibernate Validator — plain-Java static
validation helper methods only.

### Why this is a unit
The API model jar is a pure data contract with no Spring and no DB dependency. It can be built,
unit-tested, and published before any service or DAO code is written. Every subsequent SP depends
on it.

### Key reference files

| File | Purpose |
|------|---------|
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerDTO.java` | Legacy DTO to port |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupDTO.java` | Legacy DTO to port |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerPageDTO.java` | Legacy DTO to port |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupPageDTO.java` | Legacy DTO to port |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/WagerDTO.java` | Nested wager sub-object to repackage |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/resources/com/gtech/esa/gateway/rest/drawgames/dto/constraints-FavoriteWagerDTO.xml` | Validation rules to port as plain Java |
| `.planning/java25-spring-boot-migration.md` | DTO mapping table; fields to keep vs drop |

### Key classes / files to create

```
czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/
  FavouriteWagerDto.java          # id, playerId, groupNumber, gameName, wagerName, flags, createdAt, wager
  FavouriteGroupDto.java          # id, playerId, groupNumber, groupName, flags, createdAt, updatedAt
  FavouriteWagerPageDto.java      # items: List<FavouriteWagerDto>, totalCount
  FavouriteGroupPageDto.java      # items: List<FavouriteGroupDto>, totalCount
  WagerDto.java                   # gameName, stake, price, duration, serialNumber, boards

czh-favorites-api/src/test/java/cz/bsl/czh/favourites/api/
  FavouriteWagerDtoTest.java      # Jackson round-trip: serialize -> deserialize -> assertEquals
  FavouriteGroupDtoTest.java      # Jackson round-trip
  WagerDtoTest.java               # Jackson round-trip; confirm no data loss on boards/stacks
```

### Dependencies on other sub-plans
SP1 (root POM must declare `czh-favorites-api` as a module).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerDTO.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupDTO.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerPageDTO.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/FavoriteWagerGroupPageDTO.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/dto/WagerDTO.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/resources/com/gtech/esa/gateway/rest/drawgames/dto/constraints-FavoriteWagerDTO.xml`
> - `.planning/java25-spring-boot-migration.md` (DTO mapping table; fields to keep / drop)
>
> Create a detailed implementation plan for SP2: JSON API Models.
> - Port each of the four DTOs plus `WagerDto` into `cz.bsl.favourites.api`
> - Remove all JAX-RS, Jackson 1.x (`org.codehaus.jackson`), Joda Time, and Hibernate Validator imports
> - Add Jackson 2 (`com.fasterxml.jackson.annotation`) annotations; use `java.time.Instant` for timestamps
> - `FavouriteWagerDto` must round-trip cleanly to/from JSON with no data loss on `boards`
> - Write a Jackson round-trip JUnit 5 test for each DTO
> Package: `cz.bsl.favourites.api`

---
## SP3: DAO Layer

### Scope
Create the two new tables (`GIS_FAV_GROUP`, `GIS_FAV_WAGER`) as db2delta scripts, and implement
`FavouritesGroupDao` and `FavouritesWagerDao` using `JdbcTemplate`. All SQL is readable
named-constant strings inside the DAO class. The wager DAO serialises/deserialises the
`FavouriteWagerDto` wager sub-object as JSON into `WAGER_JSON`. Provide `RowMapper` implementations
for both tables.

### Why this is a unit
The DAO layer is an isolated persistence concern. It depends only on the API models (SP2) for JSON
serialisation and on the root scaffold (SP1) for the `JdbcTemplate` bean. Service and controller
code (SP5, SP6) depend on it, so it must exist first.

### Key reference files

| File | Purpose |
|------|---------|
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/dao/FavoriteWagerDAO.java` | Legacy Hibernate DAO — understand operations to port |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/dao/FavoriteWagerGroupDAO.java` | Legacy group DAO |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/domain/CZHFavoriteWager.java` | Legacy domain object — field reference |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/domain/CZHFavoriteWagerGroup.java` | Legacy group domain object |
| `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao/` | JdbcTemplate DAO style reference |
| `.planning/java25-spring-boot-migration.md` | Exact DDL for both tables; delta script format; FLAGS bitmask; WAGER_JSON spec |

### Key classes / files to create

```
czh-favorites-db/src/main/delta/
  001-create-gis-fav-group.sql     # db2delta format: GIS_FAV_GROUP + UQ constraint + validation
  002-create-gis-fav-wager.sql     # db2delta format: GIS_FAV_WAGER + 3 indexes + validation

czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/
  FavouritesGroupDao.java          # CRUD on GIS_FAV_GROUP via JdbcTemplate + RowMapper inner class
                                   #   insert, findByPlayer, findByPlayerAndGroup,
                                   #   updateName, delete, countByPlayer
  FavouritesWagerDao.java          # CRUD on GIS_FAV_WAGER via JdbcTemplate + RowMapper inner class
                                   #   insert, findById, findByPlayer, findByPlayerAndGroup,
                                   #   findByPlayerAndGame, update, delete, countByPlayer
                                   #   serialises/deserialises WAGER_JSON via Jackson ObjectMapper

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/dao/
  FavouritesGroupDaoTest.java      # HSQL in-memory: insert, find, update, delete, count
  FavouritesWagerDaoTest.java      # HSQL in-memory: insert, find by group/game, JSON round-trip
```

### Dependencies on other sub-plans
SP1 (DataSourceConfig provides JdbcTemplate), SP2 (FavouriteWagerDto for WAGER_JSON serialisation).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/dao/FavoriteWagerDAO.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/dao/FavoriteWagerGroupDAO.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/domain/CZHFavoriteWager.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/domain/CZHFavoriteWagerGroup.java`
> - `find /java/czh/czh-money/money-app/src/main/java/cz/bsl/money/dao -name "*.java"` and read those DAO files (JdbcTemplate style to follow)
> - `.planning/java25-spring-boot-migration.md` (exact DDL for both tables; `FLAGS` bitmask; `WAGER_JSON` column; db2delta script format)
>
> Create a detailed implementation plan for SP3: DAO Layer.
> - Write two delta scripts in db2delta format for `czh-favorites-db/src/main/delta/`
>   (use exact DDL from migration doc; include validation changesets)
> - Implement `FavouritesGroupDao` with `JdbcTemplate`; SQL as named String constants; `RowMapper` inner class
> - Implement `FavouritesWagerDao` with `JdbcTemplate`; serialise `WagerDto` sub-object to/from JSON
>   in `WAGER_JSON` using Jackson `ObjectMapper`; `RowMapper` inner class
> - JUnit 5 tests using HSQL in-memory for each DAO method
> - Test must verify JSON round-trip for `WAGER_JSON`: write wager -> read it back -> assert fields equal
> Package: `cz.bsl.favourites.dao`

---
## SP4: Test Infrastructure

### Scope
Implement the shared `AbstractRestTest` base class, `TestDatabaseConfig` (H2 schema initialised
from the delta scripts), and the `asPlayer(playerId)` helper so every controller test can inject
an `X-Player-Id` header. This is the shared test base that SP6 and SP7 depend on.

### Why this is a unit
Test infrastructure is cross-cutting. Defining it before the controller SPs lets each feature plan
assume `AbstractRestTest` exists and is already described. It also validates that the H2 schema
and delta scripts work correctly before the real features are implemented.

### Key reference files

| File | Purpose |
|------|---------|
| `/jamcrest-testing` skill | `AbstractRestTest` contract: boot SB on random port, mock DB, auto-print missing `.resp.js` |
| `/jamcrest` skill | How `.req.js`/`.resp.js` fixtures work with `JamcrestUtils` |
| `references/czh-money/` | Check for existing `AbstractRestTest` style: `find references/czh-money -name "AbstractRestTest.java"` |
| `.planning/java25-spring-boot-migration.md` | AbstractRestTest requirements; `asPlayer(playerId)` helper spec |
| `czh-favorites-db/src/main/delta/` | Delta scripts that H2 init must apply (produced in SP3) |

### Key classes / files to create

```
czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/
  AbstractRestTest.java            # @SpringBootTest(RANDOM_PORT), TestRestTemplate, Jamcrest helpers
                                   # asPlayer(playerId) sets X-Player-Id header on requests
                                   # prints suggestJsonDefinition() to stdout when .resp.js missing

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/
  TestDatabaseConfig.java          # @Profile("test"): creates GIS_FAV_GROUP + GIS_FAV_WAGER in H2
                                   # applies delta SQL from czh-favorites-db/src/main/delta/

czh-favorites-app/src/test/resources/
  application-test.yml             # H2 datasource, spring.profiles.active=test
```

### Dependencies on other sub-plans
SP2 (DTO classes needed by test context), SP3 (H2 schema matches GIS_FAV_* DDL from delta scripts).

### Suggested detailed-plan prompt

> Read the following before planning:
> - The `/jamcrest-testing` skill (invoke it or read `.claude/skills/jamcrest-testing.md`)
> - The `/jamcrest` skill (`.claude/skills/jamcrest.md`)
> - `.planning/java25-spring-boot-migration.md` (AbstractRestTest section; `asPlayer()` helper;
>   H2 test database section and delta script location)
> - `find /java/czh/czh-money -name "AbstractRestTest.java"` and read it (style reference)
> - `find /java/czh/czh-money -name "*TestDatabase*" -o -name "application-test.yml"` and read them
>
> Create a detailed implementation plan for SP4: Test Infrastructure.
> - `AbstractRestTest`: `@SpringBootTest(webEnvironment=RANDOM_PORT)`, `TestRestTemplate`, Jamcrest
>   assertion helper that calls `suggestJsonDefinition()` and prints to stdout when a `.resp.js` is absent
> - `asPlayer(String playerId)` helper that sets `X-Player-Id` header on requests
> - `TestDatabaseConfig`: `@Profile("test")` bean that initialises H2 with `GIS_FAV_GROUP` and
>   `GIS_FAV_WAGER` tables from the delta SQL files
> - `application-test.yml` with H2 datasource and `spring.profiles.active=test`
> - A smoke test (`AbstractRestTestSmokeTest`) verifying the Spring context starts and H2 tables exist
> Package: `cz.bsl.favourites.test`

---
## SP5: Service Layer

### Scope
Implement `FavouritesService` (interface + `DefaultFavouritesService`), `FavouritesConverter`,
`FavouritesValidator`, `PlayerContextResolver`, `FavouritesProperties`, and `CdcReader`. Port
business logic from `DefaultFavoriteWagerService`, `DefaultFavoriteWagerConverter`, and
`DefaultFavoriteWagerValidator`. Remove all JMX, Guava, Joda Time, Hibernate Validator, and
`WagerMinifier.expand()` usage. Replace `@ManagedAttribute` properties with
`@ConfigurationProperties(prefix="favourites")`.

### Why this is a unit
The service layer encapsulates all business rules. It sits between the DAO layer (SP3) and the
controller (SP6). Defining it as one SP keeps the business logic cohesive and allows full unit
test coverage before any HTTP code is written.

### Key reference files

| File | Purpose |
|------|---------|
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/DefaultFavoriteWagerService.java` | Primary port target — business logic, JMX config to replace |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/FavoriteWagerService.java` | Interface defining the 10 operations |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/converter/DefaultFavoriteWagerConverter.java` | DTO <-> domain conversion logic |
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/validator/DefaultFavoriteWagerValidator.java` | Validation rules to port as plain Java |
| `references/gis/components/gis/gis-site/src/test/java/com/gtech/gis/site/validator/WagerValidatorImplTest.java` | Existing validator tests to adapt |
| `references/gis/components/gis/gis-site/src/test/java/com/gtech/gis/site/impl/DefaultCZHFavoriteWagerConverterTest.java` | Existing converter tests to adapt |
| `.planning/java25-spring-boot-migration.md` | Config properties block; CdcReader spec; PlayerContextResolver spec; WagerMinifier dropped |

### Key classes / files to create

```
czh-favorites-app/src/main/java/cz/bsl/czh/favourites/
  service/
    FavouritesService.java           # interface: 5 wager ops + 5 group ops
    DefaultFavouritesService.java    # @Service, uses FavouritesGroupDao + FavouritesWagerDao
    FavouritesConverter.java         # DTO <-> domain; Joda->java.time.Instant; Guava->Java streams
    FavouritesValidator.java         # plain-Java validation; throws IllegalArgumentException with message
  config/
    FavouritesProperties.java        # @ConfigurationProperties(prefix="favourites") — 8 fields
  server/
    PlayerContextResolver.java       # extracts playerId from X-Player-Id; HTTP 400 if missing/blank
    CdcReader.java                   # reads /run/mx/cdc; returns int; returns 0 when file absent

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/
  DefaultFavouritesServiceTest.java  # mock DAO; test all 10 operations
  FavouritesConverterTest.java       # port of DefaultCZHFavoriteWagerConverterTest
  FavouritesValidatorTest.java       # port of WagerValidatorImplTest; all validation rules
  CdcReaderTest.java                 # file present -> integer; file absent -> 0
```

### Dependencies on other sub-plans
SP2 (DTO types), SP3 (DAO interfaces).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/FavoriteWagerService.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/service/DefaultFavoriteWagerService.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/converter/DefaultFavoriteWagerConverter.java`
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/validator/DefaultFavoriteWagerValidator.java`
> - `references/gis/components/gis/gis-site/src/test/java/com/gtech/gis/site/validator/WagerValidatorImplTest.java`
> - `references/gis/components/gis/gis-site/src/test/java/com/gtech/gis/site/impl/DefaultCZHFavoriteWagerConverterTest.java`
> - `.planning/java25-spring-boot-migration.md` (config properties block; CdcReader spec;
>   PlayerContextResolver spec; WagerMinifier.expand() dropped; ARTEGameHostServiceImpl dropped;
>   time from java.time.Instant.now(); CDC from /run/mx/cdc)
>
> Create a detailed implementation plan for SP5: Service Layer.
> - `FavouritesService` interface: 5 wager operations (create, get, update, delete, list) + 5 group operations
> - `DefaultFavouritesService`: remove JMX, Guava, Joda Time; use @ConfigurationProperties, Java streams,
>   java.time.Instant; drop WagerMinifier.expand() — caller sends complete data
> - `FavouritesConverter`: replace Joda with java.time.Instant; replace Guava with standard Java
> - `FavouritesValidator`: port all validation rules as plain Java; no Hibernate Validator;
>   throws IllegalArgumentException with human-readable message
> - `FavouritesProperties`: 8 @ConfigurationProperties fields from migration doc
> - `PlayerContextResolver`: extracts X-Player-Id from HttpServletRequest; returns HTTP 400 if missing/blank
> - `CdcReader`: reads /run/mx/cdc as ASCII integer; returns 0 when file absent (test environments)
> - JUnit 5 unit tests for every class with business logic
> Package: `cz.bsl.favourites.service`, `.config`, `.server`

---
## SP6: Favourites Controller

### Scope
Implement `FavouritesController` — the single `@RestController` at `/favourites` — with all 10
endpoint methods (5 wager, 5 group). Wire `PlayerContextResolver` for header extraction. Return
HTTP 400 for validation errors and HTTP 404 when a wager/group is not found or belongs to another
player. Write full Jamcrest integration tests using `AbstractRestTest` for every endpoint.

### Why this is a unit
The controller is the public face of the microservice. It composes the service + validation layers
(SP5) and the test infrastructure (SP4) into verifiable HTTP behaviour. All 10 endpoints share
the same service, validator, and error-handling path, so they form one cohesive unit.

### Key reference files

| File | Purpose |
|------|---------|
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/PlayerFavoriteWagerResource.java` | Legacy JAX-RS resource to port |
| `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java` | Gateway-side DTO shapes and endpoint contracts |
| `references/gis/tools/tests/rest-tests/src/test/java/com/gtech/pd/sites/czh/gis/favorites/SportkaFavoritesIntegrationTest.java` | Legacy integration test — understand happy paths |
| `.planning/java25-spring-boot-migration.md` | Endpoint tables; HTTP 400/404 rules; test classes to write |

### Key classes / files to create

```
czh-favorites-app/src/main/java/cz/bsl/czh/favourites/
  controller/
    FavouritesController.java        # @RestController @RequestMapping("/favourites")
                                     # wager: POST /wagers, GET /wagers/{id}, PUT /wagers/{id},
                                     #   DELETE /wagers/{id}, GET /wagers?group=&game-names=
                                     # group: POST /groups, GET /groups/{n}, PUT /groups/{n},
                                     #   DELETE /groups/{n}, GET /groups
    FavouritesErrorHandler.java      # @ControllerAdvice: IllegalArgumentException->400,
                                     #   NoSuchElementException->404

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/controller/
  FavouritesWagerControllerTest.java # extends AbstractRestTest; Jamcrest for 5 wager endpoints
  FavouritesGroupControllerTest.java # extends AbstractRestTest; Jamcrest for 5 group endpoints
  FavouritesValidationTest.java      # extends AbstractRestTest; invalid id, missing header, limits exceeded

czh-favorites-app/src/test/resources/favourites/
  wager/create.req.js
  wager/create.resp.js
  wager/get.resp.js
  wager/update.req.js
  wager/list.resp.js
  group/create.req.js
  group/create.resp.js
  group/get.resp.js
  group/update.req.js
  group/list.resp.js
```

### Dependencies on other sub-plans
SP4 (AbstractRestTest), SP5 (FavouritesService, PlayerContextResolver, FavouritesValidator).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/PlayerFavoriteWagerResource.java`
> - `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java`
> - `references/gis/tools/tests/rest-tests/src/test/java/com/gtech/pd/sites/czh/gis/favorites/SportkaFavoritesIntegrationTest.java`
>   (understand the happy-path test flow for wager CRUD)
> - `.planning/java25-spring-boot-migration.md` (all endpoint tables; validation rules; HTTP 400/404 rules;
>   test classes to write; WagerMinifier.expand() dropped — caller sends complete data)
>
> Create a detailed implementation plan for SP6: Favourites Controller.
> - `FavouritesController` at `/favourites`; all 10 endpoint methods from the migration doc tables
> - Use `PlayerContextResolver` to extract `X-Player-Id` from every request; return 400 if missing/blank
> - `FavouritesErrorHandler` @ControllerAdvice: `IllegalArgumentException`->400, `NoSuchElementException`->404
> - Write `.req.js`/`.resp.js` fixtures under `src/test/resources/favourites/` for every endpoint
>   (write from scratch based on DTO fields — no surviving GIS test resources with wager JSON)
> - `FavouritesWagerControllerTest` + `FavouritesGroupControllerTest` + `FavouritesValidationTest`
>   extending `AbstractRestTest`; all Jamcrest assertions must pass; no `.resp.js` missing
> Package: `cz.bsl.favourites.controller`

---
## SP7: Admin API

### Scope
Port `AdminFavoriteWagerResource` to a Spring MVC `@RestController` at `/admin/favourites`.
The admin API provides read-only access to a player's wagers and groups by passing `playerId` as
a path parameter (bypassing the `X-Player-Id` player header — document the security boundary).
No mutation operations: read and list only.

### Why this is a unit
The admin endpoints share the same `FavouritesService` (SP5) and `FavouritesConverter` as the
player controller but have a different auth path and a different HTTP base path. Keeping them
separate keeps each SP's scope small and avoids bloating SP6.

### Key reference files

| File | Purpose |
|------|---------|
| `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java` | Legacy admin resource — 3 GET endpoints to port |
| `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/controller/FavouritesController.java` | Player controller (SP6) — align error handling style |
| `.planning/java25-spring-boot-migration.md` | Auth header conventions |

### Key classes / files to create

```
czh-favorites-app/src/main/java/cz/bsl/czh/favourites/
  controller/
    AdminFavouritesController.java   # @RestController @RequestMapping("/admin/favourites")
                                     # GET /admin/favourites/players/{playerId}/wagers
                                     # GET /admin/favourites/players/{playerId}/wagers/{id}
                                     # GET /admin/favourites/players/{playerId}/groups
                                     # GET /admin/favourites/players/{playerId}/groups/{groupNumber}

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/controller/
  AdminFavouritesControllerTest.java # extends AbstractRestTest; Jamcrest for 4 admin read endpoints

czh-favorites-app/src/test/resources/favourites/admin/
  wagers.resp.js
  wager-get.resp.js
  groups.resp.js
  group-get.resp.js
```

### Dependencies on other sub-plans
SP4 (AbstractRestTest), SP5 (FavouritesService), SP6 (FavouritesErrorHandler reused).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java`
>   (note @Path("/admin") base; port only the GET read endpoints; no mutation endpoints needed)
> - The completed `FavouritesController.java` from SP6 (error-handling and service-call style to mirror)
> - `.planning/java25-spring-boot-migration.md` (auth header conventions)
>
> Create a detailed implementation plan for SP7: Admin API.
> - `AdminFavouritesController` at `/admin/favourites`; read-only: list wagers, get wager, list groups, get group
> - `playerId` as path parameter (admin bypasses player header); document the security boundary in Javadoc
> - Reuse `FavouritesErrorHandler` from SP6 (no new error handling needed)
> - Jamcrest tests for all 4 endpoints extending `AbstractRestTest`
> Package: `cz.bsl.favourites.controller`

---

## SP8: Prometheus Metrics

### Scope
Wire Prometheus counters into `DefaultFavouritesService` via a `FavouritesMetrics` bean: per-operation
`favourites.wager.*` and `favourites.group.*` counters for requests, successes, and failures.
Confirm the `/actuator/prometheus` endpoint exposes all counters when the application starts.

### Why this is a unit
Metrics are cross-cutting instrumentation added after the core behaviour is stable. Adding them
after SP6/SP7 avoids noisy metric wiring during feature development, and keeps the service layer
change minimal and reviewable on its own.

### Key reference files

| File | Purpose |
|------|---------|
| `find /java/czh/czh-money/money-app/src/main/java/cz/bsl/money/metrics -name "*.java"` | Prometheus counter/timer wiring style reference |
| `czh-favorites-app/src/main/resources/application.yml` | Actuator `management.endpoints.web.exposure` config |
| `.requirements/design/hld.md` | "limit to core metrics; health via prometheus" |

### Key classes / files to create

```
czh-favorites-app/src/main/java/cz/bsl/czh/favourites/
  metrics/
    FavouritesMetrics.java           # MeterRegistry wrappers:
                                     #   favourites.wager.requests / .success / .failures
                                     #   favourites.group.requests / .success / .failures

czh-favorites-app/src/test/java/cz/bsl/czh/favourites/metrics/
  FavouritesMetricsTest.java         # verify counters increment on service calls
```

### Dependencies on other sub-plans
SP6, SP7 (service layer must be stable before wiring metric calls).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `find /java/czh/czh-money/money-app/src/main/java/cz/bsl/money/metrics -name "*.java"` and read those files
> - `czh-favorites-app/src/main/resources/application.yml` (actuator management port / endpoints config)
> - `.requirements/design/hld.md` ("limit to core metrics; health via prometheus")
>
> Create a detailed implementation plan for SP8: Prometheus Metrics.
> - `FavouritesMetrics` bean: inject `MeterRegistry`; expose counters
>   `favourites.wager.requests`, `favourites.wager.success`, `favourites.wager.failures`,
>   `favourites.group.requests`, `favourites.group.success`, `favourites.group.failures`
> - Wire `FavouritesMetrics` into `DefaultFavouritesService` via constructor injection;
>   increment request on entry, success on normal return, failures on exception
> - Confirm `/actuator/prometheus` exposes the counters (integration test or manual curl)
> - JUnit 5 test: call service methods through the mock, verify counter increments
> Package: `cz.bsl.favourites.metrics`

---

## SP9: Deployment

### Scope
Create the project-specific deployment artifacts: `czh-favorites-app-rpm` (Spring Boot fat-jar
RPM installed to `/opt/czh-favorites/`) and `czh-favorites-db-rpm` (delta scripts RPM). Add
GitHub Actions YAML CI pipelines (`build.yml`, `release.yml`). Write `bin/start.sh`,
`bin/stop.sh`, `bin/status.sh`, and the systemd unit file. This is the project-specific instance
of the deployment guide with `czh-favourites` substituted for the generic `<app>` placeholder.

### Why this is a unit
Deployment is orthogonal to feature development and can only be completed once the application jar
is stable. All deployment tooling lives in separate RPM modules to keep the main code uncluttered.

### Key reference files

| File | Purpose |
|------|---------|
| `/java/czh/czh-money/money-app-rpm/` | App RPM module structure to mirror |
| `/java/czh/czh-money/czh-money-db-rpm/` | DB RPM module structure to mirror |
| `find /java/czh/czh-money/.github -name "*.yml"` | GitHub Actions YAML style reference |
| `man db2delta` | Verify exact CLI flags for DB migration — do not guess |
| `bin/install-rpm.sh`, `bin/install-rpm-remote.sh` | Install helper scripts already present in this project |
| `.requirements/design/hld.md` | "RPM deployments to /opt; start/stop/status in /opt/xxx/bin" |

### Key classes / files to create

```
czh-favorites-app-rpm/
  pom.xml                              # rpm-maven-plugin; packages fat-jar into RPM
  src/main/rpm/
    SPECS/czh-favorites-app.spec       # installs to /opt/czh-favorites/
    SOURCES/
      czh-favorites.service            # systemd unit: ExecStart=/opt/jdk-25/bin/java -jar ...
      start.sh                         # /opt/czh-favorites/bin/start.sh
      stop.sh
      status.sh

czh-favorites-db-rpm/
  pom.xml                              # rpm-maven-plugin; packages delta scripts
  src/main/rpm/
    SPECS/czh-favorites-db.spec        # installs delta SQL to /opt/czh-favorites-db/

.github/workflows/
  build.yml                            # GitHub Actions: Java 25, ./mvn.sh install -P github-ci
  release.yml                          # GitHub Actions: build RPMs on version tag push
```

### Dependencies on other sub-plans
SP8 (application must be complete before deployment packaging).

### Suggested detailed-plan prompt

> Read the following before planning:
> - `ls /java/czh/czh-money/money-app-rpm/` and read `money-app-rpm/pom.xml` and its RPM spec file
> - `ls /java/czh/czh-money/czh-money-db-rpm/` and read `czh-money-db-rpm/pom.xml`
> - `find /java/czh/czh-money/.github -name "*.yml"` and read the CI YAML files (GitHub Actions style)
> - `man db2delta` (verify exact CLI flags used in the DB RPM spec — do not guess)
> - `bin/install-rpm.sh` and `bin/install-rpm-remote.sh` (already present; understand what they expect)
> - `.requirements/design/hld.md` (deployment section)
>
> Create a detailed implementation plan for SP9: Deployment.
> - `czh-favorites-app-rpm/pom.xml` using `rpm-maven-plugin`; install fat-jar to `/opt/czh-favorites/lib/`
> - `czh-favorites.service` systemd unit using `/opt/jdk-25/bin/java` (not `java` from PATH)
> - `start.sh`, `stop.sh`, `status.sh` in `/opt/czh-favorites/bin/`
> - `czh-favorites-db-rpm/pom.xml`; install delta scripts to `/opt/czh-favorites-db/delta/`
> - GitHub Actions `build.yml`: `./mvn.sh install -P github-ci` on push/PR
> - GitHub Actions `release.yml`: build RPMs on version tag push
> - Add `czh-favorites-app-rpm` and `czh-favorites-db-rpm` to root POM `<modules>` (deployment-only profile
>   or unconditional, matching how czh-money does it)

---
## SP10: Vision GUI

### Scope
Implement the operator Vision GUI — a single-page dark-theme dashboard served from
`czh-favorites-app` at `/gui/` on the management port. Same Bootstrap 5 Darkly look and feel as
`/java/czh/czh-money/money-app/src/main/resources/gui/`. Three tabs specific to czh-favourites:
**Server** (health status, uptime, JVM heap — identical in structure to czh-money's `server.js`),
**Operations** (per-operation TPM bar graphs driven dynamically from `favourites_wager_*` and
`favourites_group_*` Prometheus counters added in SP8), and **Player Lookup** (enter a `playerId`,
call the admin API from SP7 to show the player's wager count, group count, and a brief list of
their favourites). No server-side template engine — pure static HTML + vanilla JS served from
`src/main/resources/gui/`.

### Why this is a unit
The GUI is a self-contained operational tool. It depends on the Prometheus metrics (SP8) for the
Operations tab and on the admin API (SP7) for the Player Lookup tab. It does not change any Java
code — only static resources and a Spring MVC `ResourceHandlerRegistry` mapping. Keeping it as
one SP avoids fragmenting the front-end work across multiple plans.

### Key reference files

| File | Purpose |
|------|---------|
| `references/czh-money/money-app/src/main/resources/gui/index.html` | HTML shell to copy and retitle |
| `references/czh-money/money-app/src/main/resources/gui/js/app.js` | Tab + refresh + CMD shell — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/js/api.js` | Fetch helper — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/js/clock.js` | GTMS clock widget — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/js/prom.js` | Prometheus text-format parser — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/js/chart.js` | Chart helpers — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/css/gui.css` | Dark theme CSS — copy verbatim |
| `references/czh-money/money-app/src/main/resources/gui/js/snapshots/server.js` | Server tab implementation — copy and retitle |
| `references/czh-money/money-app/src/main/resources/gui/js/snapshots/operations.js` | Operations tab — adapt metric name from `money_operation_total` to `favourites_wager_total` / `favourites_group_total` |
| `references/czh-money/money-app/src/main/resources/gui/js/snapshots/metrics.js` | Metrics tab — adapt or replace with Player Lookup tab |
| `.planning/java25-spring-boot-migration.md` | Sanename; admin API paths (/admin/favourites/players/{id}/...) |

### Key classes / files to create

```
czh-favorites-app/src/main/resources/gui/
  index.html                             # retitled "czh-favourites Vision 👀"; same 3-tab structure
  favicon.png                            # copy from czh-money reference
  css/
    gui.css                              # copy verbatim from czh-money reference
  js/
    api.js                               # copy verbatim
    app.js                               # copy verbatim
    clock.js                             # copy verbatim
    prom.js                              # copy verbatim
    chart.js                             # copy verbatim
    snapshots/
      server.js                          # copy from czh-money; retitle; no logic changes needed
      operations.js                      # adapt from czh-money operations.js:
                                         #   change metric name from money_operation_total
                                         #   to favourites_wager_total and favourites_group_total;
                                         #   ops discovered dynamically from labels (no hardcoding)
      player-lookup.js                   # NEW: text input for playerId; on submit calls
                                         #   GET /admin/favourites/players/{id}/wagers
                                         #   GET /admin/favourites/players/{id}/groups
                                         #   renders wager count, group count, wager name list

czh-favorites-app/src/main/java/cz/bsl/czh/favourites/
  config/
    GuiWebConfig.java                    # implements WebMvcConfigurer; maps /gui/** to classpath:/gui/
```

### Tabs in detail

| Tab             | Data source                       | Key display elements                                                                 |
|-----------------|-----------------------------------|--------------------------------------------------------------------------------------|
| Server          | `/actuator/health`, `/actuator/prometheus` | UP/DOWN badge, db status, uptime, JVM heap bar (used/committed/max)       |
| Operations      | `/actuator/prometheus`            | Per-op TPM bar charts, one per op label from `favourites_wager_total` / `favourites_group_total`; ok=green, failure=red; lazy — shows "no operations yet" until traffic |
| Player Lookup   | `/admin/favourites/players/{id}/wagers`, `/admin/favourites/players/{id}/groups` | playerId input field; on submit: wager count, group count, table of wager names + game names |

### Dependencies on other sub-plans
SP7 (admin API for Player Lookup tab), SP8 (Prometheus counters for Operations tab). SP10 can run
in parallel with SP9 (Deployment) once SP7 and SP8 are complete.

### Suggested detailed-plan prompt

> Read the following before planning:
> - `references/czh-money/money-app/src/main/resources/gui/index.html`
> - `references/czh-money/money-app/src/main/resources/gui/js/app.js`
> - `references/czh-money/money-app/src/main/resources/gui/js/api.js`
> - `references/czh-money/money-app/src/main/resources/gui/js/snapshots/server.js`
> - `references/czh-money/money-app/src/main/resources/gui/js/snapshots/operations.js`
> - `references/czh-money/money-app/src/main/resources/gui/css/gui.css`
> - `.planning/java25-spring-boot-migration.md` (sanename=`favourites`; admin API paths; SP8 metric names)
>
> Create a detailed implementation plan for SP10: Vision GUI.
> - Copy `api.js`, `app.js`, `clock.js`, `prom.js`, `chart.js`, `gui.css`, `favicon.png` verbatim from
>   `references/czh-money/money-app/src/main/resources/gui/` — no changes to shared infrastructure
> - Copy `server.js` verbatim; only change the title string from "czh-money" to "czh-favourites"
> - Adapt `operations.js`: replace metric name `money_operation_total` with `favourites_wager_total`
>   for wager ops and `favourites_group_total` for group ops; keep dynamic label discovery (no
>   hardcoded op list); ops from both metrics appear as separate graph cards
> - Write `player-lookup.js`: input box for playerId, submit button, calls admin API endpoints from SP7
>   (`/admin/favourites/players/{id}/wagers` and `/admin/favourites/players/{id}/groups`), renders a
>   count summary and a scrollable table of wager name + game name rows; handles 404 (player not found)
>   and network errors gracefully
> - Write `index.html`: same structure as czh-money; title "czh-favourites Vision 👀"; register three
>   tabs: 'server' (Server), 'operations' (Operations), 'player' (Player Lookup)
> - Write `GuiWebConfig.java`: `addResourceHandlers` mapping `/gui/**` to `classpath:/gui/`
>   so the static files are served by Spring MVC
> - Smoke test: start the application and confirm `GET /gui/index.html` returns 200
> Package: `cz.bsl.favourites.config`

---
## Progress Tracking

### File-location conventions

| Artifact               | Path                                          |
|------------------------|-----------------------------------------------|
| Detailed plan (per SP) | `.planning/plans/SPX_<SLUG>_PLAN.md`          |
| Progress state (per SP)| `.planning/state/SPX_<SLUG>_PROGRESS.md`      |
| This index             | `.planning/migration-plan-index.md`           |

### Slug mapping

| SP  | Slug        |
|-----|-------------|
| SP1 | SCAFFOLD    |
| SP2 | API_MODELS  |
| SP3 | DAO         |
| SP4 | TEST_INFRA  |
| SP5 | SERVICE     |
| SP6 | CONTROLLER  |
| SP7 | ADMIN_API   |
| SP8 | METRICS     |
| SP9 | DEPLOYMENT  |
| SP10 | GUI        |

### `geany-progress` commands

```bash
# Register a plan in the Geany sidebar (run once per SP when the detailed plan is written)
geany-progress init .planning/state/SPX_<SLUG>_PROGRESS.md

# Record completion of a phase within an SP
geany-progress done N -r "phase N done: <summary>" -w .planning/state/SPX_<SLUG>_PROGRESS.md
```

Skip silently if the `geany-progress` socket is absent (CI environments).

### Resume in a new session

1. Read `.planning/state/SPX_<SLUG>_PROGRESS.md` — find the last completed phase number.
2. Read `.planning/migration-plan-index.md` (this file) — locate the current SP section.
3. Read `.planning/plans/SPX_<SLUG>_PLAN.md` — find the next phase.
4. Invoke `/opus-exec` with the plan file; it skips completed phases and continues from where
   the progress state left off.

### Workflow sequence (per SP)

```
/opus-plan  ->  review SPX_*_PLAN.md  ->  /opus-exec  ->  post-SP sign-off
                                                           (test -> coverage -> security -> refactor -> routing-map)
```
