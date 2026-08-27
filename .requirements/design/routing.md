# Routing Map — czh-favourites

Updated after SP9 (Deployment). Grep anchor for all files: `favourites`.

---

## Section 1 — Feature → sanename table

| Feature                  | Sanename               | Module              | Package                               | File                                                                                                          |
|--------------------------|------------------------|---------------------|---------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Application entry point  | FavouritesApplication  | czh-favorites-app   | cz.bsl.favourites                     | czh-favorites-app/src/main/java/cz/bsl/favourites/FavouritesApplication.java                                 |
| Config properties        | FavouritesProperties   | czh-favorites-app   | cz.bsl.favourites.config              | czh-favorites-app/src/main/java/cz/bsl/favourites/config/FavouritesProperties.java                           |
| DataSource (DB2/H2)      | DataSourceConfig       | czh-favorites-app   | cz.bsl.favourites.config              | czh-favorites-app/src/main/java/cz/bsl/favourites/config/DataSourceConfig.java                               |
| Favourite wager API DTO  | FavouriteWagerDto      | czh-favorites-api   | cz.bsl.czh.favourites.api             | czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java                             |
| Favourite group API DTO  | FavouriteGroupDto      | czh-favorites-api   | cz.bsl.czh.favourites.api             | czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java                             |
| Wager sub-object (JSON)  | WagerDto               | czh-favorites-api   | cz.bsl.czh.favourites.api             | czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java                                      |
| Wager list response      | FavouriteWagerPageDto  | czh-favorites-api   | cz.bsl.czh.favourites.api             | czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerPageDto.java                         |
| Group list response      | FavouriteGroupPageDto  | czh-favorites-api   | cz.bsl.czh.favourites.api             | czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupPageDto.java                         |
| Test DB schema config    | TestDatabaseConfig     | czh-favorites-app   | cz.bsl.czh.favourites.config (test)   | czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/TestDatabaseConfig.java                         |
| JSON test utils          | JamcrestUtils          | czh-favorites-app   | cz.bsl.czh.favourites.test            | czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/JamcrestUtils.java                                |
| Base REST integration    | AbstractRestTest       | czh-favorites-app   | cz.bsl.czh.favourites.test            | czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java                             |
| Context smoke test       | FavouritesRestSmokeTest| czh-favorites-app   | cz.bsl.czh.favourites.test            | czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java                      |
| Service interface        | FavouritesService      | czh-favorites-app   | cz.bsl.czh.favourites.service         | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java                         |
| Service implementation   | DefaultFavouritesService | czh-favorites-app | cz.bsl.czh.favourites.service         | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java                  |
| DTO/record converter     | FavouritesConverter    | czh-favorites-app   | cz.bsl.czh.favourites.service         | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java                       |
| Input validation         | FavouritesValidator    | czh-favorites-app   | cz.bsl.czh.favourites.service         | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java                       |
| Player header extraction | PlayerContextResolver  | czh-favorites-app   | cz.bsl.czh.favourites.server          | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java                      |
| CDC draw counter reader  | CdcReader              | czh-favorites-app   | cz.bsl.czh.favourites.server          | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/CdcReader.java                                  |
| REST error handler       | FavouritesErrorHandler | czh-favorites-app   | cz.bsl.favourites.web                 | czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesErrorHandler.java                            |
| Player wager/group REST  | FavouritesController   | czh-favorites-app   | cz.bsl.favourites.web                 | czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java                              |
| Wager controller tests   | FavouritesWagerControllerTest | czh-favorites-app | cz.bsl.favourites.web (test)       | czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java                     |
| Group controller tests   | FavouritesGroupControllerTest | czh-favorites-app | cz.bsl.favourites.web (test)       | czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java                     |
| Validation error tests   | FavouritesValidationTest | czh-favorites-app | cz.bsl.favourites.web (test)          | czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java                          |
| Error handler unit test  | FavouritesErrorHandlerTest | czh-favorites-app | cz.bsl.favourites.web (test)        | czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesErrorHandlerTest.java                        |
| Admin read-only REST     | AdminFavouritesController  | czh-favorites-app   | cz.bsl.favourites.web               | czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java                         |
| Admin controller tests   | AdminFavouritesControllerTest | czh-favorites-app | cz.bsl.favourites.web (test)      | czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java                     |
| Prometheus counters      | FavouritesMetrics          | czh-favorites-app   | cz.bsl.czh.favourites.metrics     | czh-favorites-app/src/main/java/cz/bsl/czh/favourites/metrics/FavouritesMetrics.java                         |
| Metrics unit tests       | FavouritesMetricsTest      | czh-favorites-app   | cz.bsl.czh.favourites.metrics (test) | czh-favorites-app/src/test/java/cz/bsl/czh/favourites/metrics/FavouritesMetricsTest.java                  |
| GUI static resource map  | GuiWebConfig               | czh-favorites-app   | cz.bsl.favourites.config            | czh-favorites-app/src/main/java/cz/bsl/favourites/config/GuiWebConfig.java                                   |
| App RPM packaging        | czh-favorites-app-rpm      | czh-favorites-app-rpm | (deployment; no Java)             | czh-favorites-app-rpm/pom.xml                                                                                 |
| DB delta RPM packaging   | czh-favorites-db-rpm       | czh-favorites-db-rpm  | (deployment; no Java)             | czh-favorites-db-rpm/pom.xml                                                                                  |
| GitHub Actions CI        | build.yml                  | (CI)                | (GitHub Actions YAML)               | .github/workflows/build.yml                                                                                   |

---

## Section 2 — Brief code map

```
czh-favorites-api/   cz.bsl.czh.favourites.api    JSON request/response DTOs; no Spring, no DB.
                                                   WagerDto is the WAGER_JSON blob shape.
                                                   Page wrappers carry items + totalCount.

czh-favorites-app/   cz.bsl.favourites             FavouritesApplication (@SpringBootApplication)
                     cz.bsl.favourites.config      FavouritesProperties (@ConfigurationProperties
                                                     prefix=favourites), DataSourceConfig
                                                     (HikariCP; DB2 prod / HSQLDB test profile)
                     cz.bsl.czh.favourites.dao     JdbcTemplate DAOs — FavouritesGroupDaoImpl
                                                     (GIS_FAV_GROUP), FavouritesWagerDaoImpl
                                                     (GIS_FAV_WAGER); RowMapper inner classes;
                                                     FavouriteGroupRecord, FavouriteWagerRecord
                     cz.bsl.czh.favourites.service (SP5) FavouritesService, FavouritesValidator,
                                                     FavouritesConverter
                     cz.bsl.czh.favourites.server  PlayerContextResolver (X-Player-Id header),
                                                     CdcReader (CDC draw file reader)
                     cz.bsl.czh.favourites.metrics (SP8) FavouritesMetrics — wager + group
                                                     Prometheus counters (favourites_wager_
                                                     operation_total / favourites_group_
                                                     operation_total)
                     cz.bsl.favourites.web         (SP6) FavouritesController — 10 endpoints:
                                                     5 wager (CRUD + list), 5 group (CRUD + list)
                                                     FavouritesErrorHandler (@ControllerAdvice:
                                                       IAE→400, NSE→404)
                                                   (SP7) AdminFavouritesController — 4 read-only
                                                     admin endpoints at /admin/favourites/players/
                                                     {playerId}/wagers|groups
                     cz.bsl.favourites.config      FavouritesProperties, DataSourceConfig,
                                                   (SP10) GuiWebConfig — maps /gui/** to
                                                     classpath:/gui/ (Bootstrap 5 Darkly dark-theme
                                                     operator dashboard; 3 tabs: Server, Operations,
                                                     Player Lookup)

czh-favorites-app/   cz.bsl.czh.favourites.test    Test infrastructure (test scope only):
  (test)               (test)                        JamcrestUtils — GraalVM JS JSON validation
                                                     AbstractRestTest — @SpringBootTest base,
                                                       non-throwing RestTemplate, asPlayer()
                                                     FavouritesRestSmokeTest — context + health smoke
                     cz.bsl.czh.favourites.config  TestDatabaseConfig (@TestConfiguration @Profile
                       (test)                         "test") — runs dao-test-schema.sql on HSQLDB,
                                                       guards with INFORMATION_SCHEMA double-create
                                                       check, exposes JamcrestUtils bean

czh-favorites-db/    src/main/delta/               db2delta SQL scripts — no Java.
                                                   001-create-gis-fav-group.sql
                                                   002-create-gis-fav-wager.sql

czh-favorites-app-rpm/  src/main/resources/       (SP9) App RPM: fat-jar → /opt/czh-favorites/lib/
                         bin/                        start/stop/status/kill/version scripts
                         config/                     logback-spring.xml for /var/log/czh-favorites/
                         service/                    czh-favorites.service (systemd; /opt/jdk-25/bin/java)

czh-favorites-db-rpm/   src/main/bin/             (SP9) DB delta RPM: SQL → /opt/czh-favorites-db/structure/
                                                   run-delta.sh — applies db2delta migrations

.github/workflows/       build.yml                (SP9) GitHub Actions CI: JDK 25 Temurin,
                                                   mvn install -Drpm=true -P github-ci on develop
```

---

## Section 3 — Sanename → file quick-lookup

```
favourites-app          → czh-favorites-app/src/main/java/cz/bsl/favourites/FavouritesApplication.java
favourites-properties   → czh-favorites-app/src/main/java/cz/bsl/favourites/config/FavouritesProperties.java
favourites-datasource   → czh-favorites-app/src/main/java/cz/bsl/favourites/config/DataSourceConfig.java

FavouriteWagerDto       → czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerDto.java
FavouriteGroupDto       → czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupDto.java
WagerDto                → czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/WagerDto.java
FavouriteWagerPageDto   → czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteWagerPageDto.java
FavouriteGroupPageDto   → czh-favorites-api/src/main/java/cz/bsl/czh/favourites/api/FavouriteGroupPageDto.java

FavouritesGroupDao      → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesGroupDaoImpl.java
FavouritesWagerDao      → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouritesWagerDaoImpl.java
FavouriteGroupRecord    → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteGroupRecord.java
FavouriteWagerRecord    → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/dao/FavouriteWagerRecord.java

delta-group             → czh-favorites-db/src/main/delta/001-create-gis-fav-group.sql
delta-wager             → czh-favorites-db/src/main/delta/002-create-gis-fav-wager.sql

TestDatabaseConfig      → czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/TestDatabaseConfig.java
JamcrestUtils           → czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/JamcrestUtils.java
AbstractRestTest        → czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java
FavouritesRestSmokeTest → czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java

FavouritesService       → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java
DefaultFavouritesService → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java
FavouritesConverter     → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java
FavouritesValidator     → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java

PlayerContextResolver   → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java
CdcReader               → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/CdcReader.java

FavouritesController    → czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java
FavouritesErrorHandler  → czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesErrorHandler.java
FavouritesWagerControllerTest → czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java
FavouritesGroupControllerTest → czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java
FavouritesValidationTest → czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java
FavouritesErrorHandlerTest → czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesErrorHandlerTest.java

AdminFavouritesController → czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java
AdminFavouritesControllerTest → czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java

FavouritesMetrics       → czh-favorites-app/src/main/java/cz/bsl/czh/favourites/metrics/FavouritesMetrics.java
FavouritesMetricsTest   → czh-favorites-app/src/test/java/cz/bsl/czh/favourites/metrics/FavouritesMetricsTest.java

GuiWebConfig            → czh-favorites-app/src/main/java/cz/bsl/favourites/config/GuiWebConfig.java
gui/index.html          → czh-favorites-app/src/main/resources/gui/index.html
gui/operations.js       → czh-favorites-app/src/main/resources/gui/js/snapshots/operations.js
gui/player-lookup.js    → czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js
gui/server.js           → czh-favorites-app/src/main/resources/gui/js/snapshots/server.js

czh-favorites-app-rpm   → czh-favorites-app-rpm/pom.xml
czh-favorites.service   → czh-favorites-app-rpm/src/main/resources/service/czh-favorites.service
start-service.sh        → czh-favorites-app-rpm/src/main/resources/bin/start-service.sh
stop-service.sh         → czh-favorites-app-rpm/src/main/resources/bin/stop-service.sh
status-service.sh       → czh-favorites-app-rpm/src/main/resources/bin/status-service.sh
logback-rpm             → czh-favorites-app-rpm/src/main/resources/config/logback-spring.xml

czh-favorites-db-rpm    → czh-favorites-db-rpm/pom.xml
run-delta.sh            → czh-favorites-db-rpm/src/main/bin/run-delta.sh

build.yml               → .github/workflows/build.yml
```

---

## Ports

| Service    | Port |
|------------|------|
| HTTP (app) | 9290 |
| Management | 9291 |

