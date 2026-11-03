# US Spelling Cleanup — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Player API paths to the reference contract | complete |
| 2 | Admin API paths + GUI calls | complete |
| 3 | Package and class renames | complete |
| 4 | Config, metrics, URL-prefix and prose spelling sweep | complete |
| 5 | Routing map rewrite + final verification | complete |

## Review notes

### Phase 1 — Player API paths to the reference contract

⚠ POST still returns 201+Location; reference Javadoc says 200 — kept 201, Location re-shaped. Veto if the gateway contract needs 200.

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java`

### Phase 2 — Admin API paths + GUI calls

⚠ Admin list-groups endpoint kept although absent from the legacy reference (Vision GUI needs it) — re-pathed, documented in Javadoc.

⚠ Admin list gains optional groupNumber + game-names filters per the reference; bare call still lists all.

⚠ Pre-existing test-order bug: HSQLDB identity starts at 0, so FavouritesWagerControllerTest.getWager_returns200WithBody fails on a fresh DB when run alone; full install is green. dao-test-schema.sql untouched per plan.

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java`

### Phase 3 — Package and class renames

⚠ Pure mechanical rename (packages, classes, files, package strings in yml/xml/sh). Maven artifactId czh-favourites intentionally unchanged.

⚠ The blanket sed also renamed config/metric/pool/log strings inside .java and the listed yml/xml files, and the fixture dir src/test/resources/favourites -> favorites was moved here (Phase 4 items 1-3,5 partly done early); full build green.

⚠ Missed by plan: META-INF/spring/...ManagementContextConfiguration.imports referenced cz.bsl.favourites.config.GuiResourceConfig — fixed.

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favorites/FavoritesApplication.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java`
- `/java/czh/czh-favourites/czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerDto.java`
- `/java/czh/czh-favourites/czh-favorites-app/pom.xml`

### Phase 4 — Config, metrics, URL-prefix and prose spelling sweep

⚠ nginx context roots renamed /favourites -> /favorites and /favourites-mgmt -> /favorites-mgmt (local test fixture only). External gateway wiring is out of scope — contract recorded in .requirements/integrations/gateway-paths.md.

⚠ pid file now favorites.pid which incidentally fixes kill-service.sh; stop-service.sh and status-service.sh still reference different pid names (pre-existing bug, left alone).

⚠ Metric names changed to favorites_*_operation_total — dashboards/alerts keyed on the old names must be updated.

⚠ vision-test.sh defaults to Maven profile github-ci which needs unreachable host xxmaven here; ran it with VISION_MVN_PROFILES= (empty) and PATH set for mvn. 21/21 assertions pass.

⚠ vision-test.sh admin URLs and nginx comments also moved to /admin/v1/player/{id}/draw-games/favorite-wagers/ (Phase 2 follow-through).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/resources/application.yml`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/metrics/FavoritesMetrics.java`
- `/java/czh/czh-favourites/test/fixtures/vision-nginx/conf.d/vision-demo.conf`
- `/java/czh/czh-favourites/test/vision-test.sh`

### Phase 5 — Routing map rewrite + final verification

⚠ Historical .planning/SP* docs and ai-prompts/ still mention old names by design (records of past work).

⚠ Maven czh-favourites artifactId, spring.application.name, GUI title intentionally still UK project name per prompt.

⚠ czh-favourites.geany still lists old file paths in FILE_NAME_* session entries (Geany-managed, harmless; not edited).

⚠ routing.md had stale paths beyond spelling (delta files are .delta not .sql; CI file is maven.yml not build.yml) - corrected.

⚠ Final gates: all three greps empty; ./mvn.sh install and install -Drpm=true green (131 tests); vision-test.sh 21/21 (needs VISION_MVN_PROFILES= and mvn on PATH in this sandbox).

Files for review:
- `/java/czh/czh-favourites/.requirements/design/routing.md`
