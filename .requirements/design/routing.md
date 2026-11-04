# Routing Map — czh-favorites


---

## Section 1 — Feature → sanename table

| Feature                  | Sanename                     | Module                | Package                             | File                                                                                      |
|--------------------------|------------------------------|-----------------------|-------------------------------------|-------------------------------------------------------------------------------------------|
| Application entry point  | FavoritesApplication         | czh-favorites-app     | cz.bsl.favorites                    | czh-favorites-app/src/main/java/cz/bsl/favorites/FavoritesApplication.java                |
| Config properties        | FavoritesProperties          | czh-favorites-app     | cz.bsl.favorites.config             | czh-favorites-app/src/main/java/cz/bsl/favorites/config/FavoritesProperties.java          |
| DataSource (DB2/H2)      | DataSourceConfig             | czh-favorites-app     | cz.bsl.favorites.config             | czh-favorites-app/src/main/java/cz/bsl/favorites/config/DataSourceConfig.java             |
| Favorite wager API DTO   | FavoriteWagerDto             | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerDto.java            |
| Wager group ref (id+name)| FavoriteWagerGroupDto        | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerGroupDto.java      |
| Favorite group API DTO   | FavoriteGroupDto             | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteGroupDto.java            |
| Wager sub-object (JSON)  | WagerDto                     | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/WagerDto.java                    |
| Board sub-object         | BoardDto                     | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/BoardDto.java                    |
| Wager DTO closure        | StakeDto, TaxDto, TextDto, AddressDto, IdDocumentDto, PrizeDto, PromotionDto, TicketSecurityDto, ErrorDto, FieldErrorDto, WagerType, PrizeStatus, PrizeType | czh-favorites-api | cz.bsl.czh.favorites.api | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/ (mirrors of gisorch; see bin/dto-drift-check.py) |
| Wager list response      | FavoriteWagerPageDto         | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerPageDto.java        |
| Group list response      | FavoriteGroupPageDto         | czh-favorites-api     | cz.bsl.czh.favorites.api            | czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteGroupPageDto.java        |
| DTO drift check (script) | dto-drift-check.py           | (bash/python)        | (tool)                              | bin/dto-drift-check.py                                                                   |
| Test DB schema config    | TestDatabaseConfig           | czh-favorites-app     | cz.bsl.czh.favorites.config (test)  | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/config/TestDatabaseConfig.java       |
| JSON test utils          | JamcrestUtils                | czh-favorites-app     | cz.bsl.czh.favorites.test           | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/JamcrestUtils.java              |
| Base REST integration    | AbstractRestTest             | czh-favorites-app     | cz.bsl.czh.favorites.test           | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/AbstractRestTest.java           |
| Context smoke test       | FavoritesRestSmokeTest       | czh-favorites-app     | cz.bsl.czh.favorites.test           | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/FavoritesRestSmokeTest.java     |
| Service interface        | FavoritesService             | czh-favorites-app     | cz.bsl.czh.favorites.service        | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java        |
| Service implementation   | DefaultFavoritesService      | czh-favorites-app     | cz.bsl.czh.favorites.service        | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/DefaultFavoritesService.java |
| DTO/record converter     | FavoritesConverter           | czh-favorites-app     | cz.bsl.czh.favorites.service        | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesConverter.java      |
| Input validation         | FavoritesValidator           | czh-favorites-app     | cz.bsl.czh.favorites.service        | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java      |
| Player header extraction | PlayerContextResolver        | czh-favorites-app     | cz.bsl.czh.favorites.server         | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/PlayerContextResolver.java    |
| CDC draw counter reader  | CdcReader                    | czh-favorites-app     | cz.bsl.czh.favorites.server         | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/CdcReader.java                |
| REST error handler       | FavoritesErrorHandler        | czh-favorites-app     | cz.bsl.favorites.web                | czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesErrorHandler.java           |
| Player wager/group REST  | FavoritesController          | czh-favorites-app     | cz.bsl.favorites.web                | czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java             |
| Wager controller tests   | FavoritesWagerControllerTest | czh-favorites-app     | cz.bsl.favorites.web (test)         | czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesWagerControllerTest.java    |
| Group controller tests   | FavoritesGroupControllerTest | czh-favorites-app     | cz.bsl.favorites.web (test)         | czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesGroupControllerTest.java    |
| Validation error tests   | FavoritesValidationTest      | czh-favorites-app     | cz.bsl.favorites.web (test)         | czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesValidationTest.java         |
| Error handler unit test  | FavoritesErrorHandlerTest    | czh-favorites-app     | cz.bsl.favorites.web (test)         | czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesErrorHandlerTest.java       |
| Admin read-only REST     | AdminFavoritesController     | czh-favorites-app     | cz.bsl.favorites.web                | czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java        |
| Admin controller tests   | AdminFavoritesControllerTest | czh-favorites-app     | cz.bsl.favorites.web (test)         | czh-favorites-app/src/test/java/cz/bsl/favorites/web/AdminFavoritesControllerTest.java    |
| Prometheus counters      | FavoritesMetrics             | czh-favorites-app     | cz.bsl.czh.favorites.metrics        | czh-favorites-app/src/main/java/cz/bsl/czh/favorites/metrics/FavoritesMetrics.java        |
| Metrics unit tests       | FavoritesMetricsTest         | czh-favorites-app     | cz.bsl.czh.favorites.metrics (test) | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/metrics/FavoritesMetricsTest.java    |
| GUI static resource map  | GuiResourceConfig            | czh-favorites-app     | cz.bsl.favorites.config             | czh-favorites-app/src/main/java/cz/bsl/favorites/config/GuiResourceConfig.java            |
| GUI context-root rewrite | VisionBaseContextFilter      | czh-favorites-app     | cz.bsl.favorites.config             | czh-favorites-app/src/main/java/cz/bsl/favorites/config/VisionBaseContextFilter.java      |
| GUI filter unit tests    | VisionBaseTest               | czh-favorites-app     | cz.bsl.czh.favorites.test.it (test) | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/it/VisionBaseTest.java          |
| Vision nginx harness     | vision-test.sh               | (bash)                | (test)                              | test/vision-test.sh                                                                       |
| Vision harness booter    | VisionHarnessMain            | czh-favorites-app     | cz.bsl.czh.favorites.test (test)    | czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/VisionHarnessMain.java          |
| Vision nginx fixture     | vision-nginx                 | (nginx)               | (test fixture)                      | test/fixtures/vision-nginx/nginx.conf, test/fixtures/vision-nginx/conf.d/vision-demo.conf |
| App RPM packaging        | czh-favorites-app-rpm        | czh-favorites-app-rpm | (deployment; no Java)               | czh-favorites-app-rpm/pom.xml                                                             |
| DB delta RPM packaging   | czh-favorites-db-rpm         | czh-favorites-db-rpm  | (deployment; no Java)               | czh-favorites-db-rpm/pom.xml                                                              |
| GitHub Actions CI        | maven.yml                    | (CI)                  | (GitHub Actions YAML)               | .github/workflows/maven.yml                                                               |

---

## Section 2 — Brief code map

```
czh-favorites-api/   cz.bsl.czh.favorites.api    JSON request/response DTOs; no Spring, no DB.
                                                   WagerDto (+BoardDto and the wager closure) mirrors
                                                   the front-end gisorch WagerDTO - WAGER_JSON blob shape.
                                                   Wire contract: id/name strings, group {id,name},
                                                   favoriteWagers/favoriteWagerGroups wrappers.
                                                   Favorites extensions: @JsonIgnoreProperties(ignoreUnknown),
                                                   @JsonInclude(NON_NULL); see .requirements/design/dto-alignment-baseline.md.

czh-favorites-app/   cz.bsl.favorites             FavoritesApplication (@SpringBootApplication)
                     cz.bsl.favorites.config      FavoritesProperties (@ConfigurationProperties
                                                     prefix=favorites), DataSourceConfig
                                                     (HikariCP; DB2 prod / HSQLDB test profile)
                     cz.bsl.czh.favorites.dao     JdbcTemplate DAOs — FavoritesGroupDaoImpl
                                                     (GIS_FAV_GROUP), FavoritesWagerDaoImpl
                                                     (GIS_FAV_WAGER); RowMapper inner classes;
                                                     FavoriteGroupRecord, FavoriteWagerRecord
                     cz.bsl.czh.favorites.service (SP5) FavoritesService, FavoritesValidator,
                                                     FavoritesConverter
                     cz.bsl.czh.favorites.server  PlayerContextResolver (X-Player-Id header),
                                                     CdcReader (CDC draw file reader)
                     cz.bsl.czh.favorites.metrics (SP8) FavoritesMetrics — wager + group
                                                     Prometheus counters (favorites_wager_
                                                     operation_total / favorites_group_
                                                     operation_total)
                     cz.bsl.favorites.web         (SP6) FavoritesController — 10 endpoints under
                                                     /{version}/draw-games/favorite-wagers:
                                                     5 wager (CRUD + list), 5 group (CRUD + list)
                                                     FavoritesErrorHandler (@ControllerAdvice:
                                                       IAE→400, NSE→404)
                                                   (SP7) AdminFavoritesController — 4 read-only
                                                     admin endpoints at /admin/{version}/player/
                                                     {playerId}/draw-games/favorite-wagers/
                                                     wagers|wagers/{id}|groups|groups/{n}
                                                     (list-groups is a GUI-only extra)
                     cz.bsl.favorites.config      FavoritesProperties, DataSourceConfig,
                                                   (SP10) GuiResourceConfig — @ManagementContextConfiguration
                                                     maps /gui/** to classpath:/gui/ on the management
                                                     context only (port 9291); forwards / -> /gui/index.html;
                                                     registers VisionBaseContextFilter. (Bootstrap 5 Darkly
                                                     dark-theme operator dashboard; 3 tabs: Server,
                                                     Operations, Player Lookup)
                                                   (SP10) VisionBaseContextFilter — reads the X-Vision-Base
                                                     request header from the nginx reverse proxy and rewrites
                                                     the GUI HTML (fills <meta name="vision-base"> + prefixes
                                                     /gui/ asset refs) so the browser fetches under the
                                                     context root. No-op direct-on-port.

czh-favorites-app/   cz.bsl.czh.favorites.test    Test infrastructure (test scope only):
  (test)               (test)                        JamcrestUtils — GraalVM JS JSON validation
                                                     AbstractRestTest — @SpringBootTest base,
                                                       non-throwing RestTemplate, asPlayer()
                                                     FavoritesRestSmokeTest — context + health + GUI
                                                       index smoke (management port, vision-base meta)
                                                     VisionBaseTest — VisionBaseContextFilter coverage
                                                       (no-header pass-through, with-header rewrite,
                                                       root direct-serve, JS byte-for-byte, actuator
                                                       pass-through, frame-loadable)
                                                     VisionHarnessMain — standalone main() that boots
                                                       FavoritesApplication on 9290/9291 with the test
                                                       profile for the vision-test.sh nginx harness
                     cz.bsl.czh.favorites.config  TestDatabaseConfig (@TestConfiguration @Profile
                       (test)                         "test") — runs dao-test-schema.sql on HSQLDB,
                                                       guards with INFORMATION_SCHEMA double-create
                                                       check, exposes JamcrestUtils bean

test/                 (bash)                       test-functions.sh — shared bash helpers (pass/fail,
                                                     assert_*, test_sleep, test_summary)
                                                   vision-test.sh — nginx reverse-proxy harness: boots the
                                                     Java backend on 9290/9291, starts nginx on 8080 with
                                                     the vision-nginx fixture, asserts the GUI + admin API
                                                     + actuator work through the /favorites-mgmt and
                                                     /favorites context roots. --hold for manual browser.
                   test/fixtures/vision-nginx/      nginx.conf + conf.d/vision-demo.conf — reverse-proxy
                                                     fixture mapping /favorites-mgmt -> mgmt 9291 (gui +
                                                     actuator) / app 9290 (admin), /favorites -> app 9290;
                                                     sets X-Vision-Base on every proxied request.

czh-favorites-db/    src/main/delta/               db2delta SQL scripts — no Java.
                                                   001-create-gis-fav-group.delta
                                                   002-create-gis-fav-wager.delta

czh-favorites-app-rpm/  src/main/resources/       (SP9) App RPM: fat-jar → /opt/czh-favorites/lib/
                         bin/                        start/stop/status/kill/version scripts
                         config/                     logback-spring.xml for /var/log/favorites/
                         service/                    czh-favorites.service (systemd; /opt/jdk-25/bin/java)

czh-favorites-db-rpm/   src/main/bin/             (SP9) DB delta RPM: SQL → /opt/czh-favorites-db/structure/
                                                   run-delta.sh — applies db2delta migrations

.github/workflows/       maven.yml                (SP9) GitHub Actions CI: JDK 25 Temurin,
                                                   mvn install -Drpm=true -P github-ci on develop
```

---

## Section 3 — Sanename → file quick-lookup

```
favorites-app          → czh-favorites-app/src/main/java/cz/bsl/favorites/FavoritesApplication.java
favorites-properties   → czh-favorites-app/src/main/java/cz/bsl/favorites/config/FavoritesProperties.java
favorites-datasource   → czh-favorites-app/src/main/java/cz/bsl/favorites/config/DataSourceConfig.java

FavoriteWagerDto       → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerDto.java
FavoriteWagerGroupDto  → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerGroupDto.java
FavoriteGroupDto       → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteGroupDto.java
WagerDto                → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/WagerDto.java
BoardDto                → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/BoardDto.java
wager-dto-closure      → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/ (StakeDto, TaxDto, TextDto, AddressDto, IdDocumentDto, PrizeDto, PromotionDto, TicketSecurityDto, ErrorDto, FieldErrorDto, WagerType, PrizeStatus, PrizeType)
FavoriteWagerPageDto   → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerPageDto.java
FavoriteGroupPageDto   → czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteGroupPageDto.java
dto-drift-check        → bin/dto-drift-check.py

FavoritesGroupDao      → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesGroupDaoImpl.java
FavoritesWagerDao      → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesWagerDaoImpl.java
FavoriteGroupRecord    → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoriteGroupRecord.java
FavoriteWagerRecord    → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoriteWagerRecord.java

delta-group             → czh-favorites-db/src/main/delta/001-create-gis-fav-group.delta
delta-wager             → czh-favorites-db/src/main/delta/002-create-gis-fav-wager.delta

TestDatabaseConfig      → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/config/TestDatabaseConfig.java
JamcrestUtils           → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/JamcrestUtils.java
AbstractRestTest        → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/AbstractRestTest.java
FavoritesRestSmokeTest → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/FavoritesRestSmokeTest.java
LegacySportkaRoundTripTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/LegacySportkaRoundTripTest.java (acceptance: legacy sportka.json flow)
legacy-fixtures         → czh-favorites-api/src/test/resources/legacy/ (byte-identical legacy bodies)

FavoritesService       → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java
DefaultFavoritesService → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/DefaultFavoritesService.java
FavoritesConverter     → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesConverter.java
FavoritesValidator     → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java

PlayerContextResolver   → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/PlayerContextResolver.java
CdcReader               → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/CdcReader.java

FavoritesController    → czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java
FavoritesErrorHandler  → czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesErrorHandler.java
FavoritesWagerControllerTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesWagerControllerTest.java
FavoritesGroupControllerTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesGroupControllerTest.java
FavoritesValidationTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesValidationTest.java
FavoritesErrorHandlerTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/FavoritesErrorHandlerTest.java

AdminFavoritesController → czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java
AdminFavoritesControllerTest → czh-favorites-app/src/test/java/cz/bsl/favorites/web/AdminFavoritesControllerTest.java

FavoritesMetrics       → czh-favorites-app/src/main/java/cz/bsl/czh/favorites/metrics/FavoritesMetrics.java
FavoritesMetricsTest   → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/metrics/FavoritesMetricsTest.java

GuiResourceConfig       → czh-favorites-app/src/main/java/cz/bsl/favorites/config/GuiResourceConfig.java
VisionBaseContextFilter → czh-favorites-app/src/main/java/cz/bsl/favorites/config/VisionBaseContextFilter.java
VisionBaseTest          → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/it/VisionBaseTest.java
VisionHarnessMain       → czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/VisionHarnessMain.java
vision-test             → test/vision-test.sh
vision-nginx-fixture    → test/fixtures/vision-nginx/nginx.conf, test/fixtures/vision-nginx/conf.d/vision-demo.conf
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

maven.yml               → .github/workflows/maven.yml
```

---

## Ports

| Service    | Port |
|------------|------|
| HTTP (app) | 9290 |
| Management | 9291 |

---

## Vision GUI reverse-proxy routing (nginx context roots)

The operator Vision GUI is served on the **management port (9291)** by `GuiResourceConfig` and is
designed to sit behind an nginx reverse proxy that routes by the first URL segment. nginx strips the
context prefix before proxying and sets an `X-Vision-Base` request header; `VisionBaseContextFilter`
reads it and rewrites the GUI HTML so the browser's absolute site paths resolve under the context
root. Direct-on-port (no header) is a no-op — the GUI works unchanged at `/gui/` on port 9291.

| Browser URL (through nginx)                                        | Proxied to            | Context root (`X-Vision-Base`) |
|--------------------------------------------------------------------|-----------------------|--------------------------------|
| `/favorites-mgmt/gui/**`                                           | mgmt 9291 `/gui/**`   | `/favorites-mgmt`              |
| `/favorites-mgmt/actuator/**`                                      | mgmt 9291 `/actuator` | `/favorites-mgmt`              |
| `/favorites-mgmt/admin/**`                                         | app 9290 `/admin/**`  | `/favorites-mgmt`              |
| `/favorites/admin/**` (direct/cron)                                | app 9290 `/admin/**`  | `/favorites`                   |
| `/favorites/{version}/draw-games/favorite-wagers/**` (direct/cron) | app 9290              | `/favorites`                   |

The GUI's `api.js` reads `<meta name="vision-base">` (filled by the filter) at load time and prefixes
every fetch (`/admin/...`, `/actuator/...`) with it, so all browser requests stay same-origin on the
nginx listener — no cross-port, no CORS. The nginx fixture (`test/fixtures/vision-nginx/`) and the
`test/vision-test.sh` harness prove this end-to-end.

