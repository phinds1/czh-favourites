# US Spelling Cleanup — Detailed Implementation Plan

**Plan:** US_SPELLING (standalone corrective plan, after SP1–SP10)  
**Sanename:** `favourites` → `favorites` (changes in this plan)  
**Root packages:** `cz.bsl.favourites` + `cz.bsl.czh.favourites` → `cz.bsl.favorites` + `cz.bsl.czh.favorites`  
**Progress state:** `.planning/state/US_SPELLING_PROGRESS.md`

## Overview

The service was built with the UK spelling *favourite* everywhere. The house standard is the US
spelling *favorite*, matching the legacy reference APIs. This plan fixes three things:

1. **REST API paths** — the current paths (`/favourites/wagers`, `/admin/favourites/players/…`)
   were hallucinated from the UK spelling. The correct contracts are defined by the reference
   sources:
   - Player API: `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java`
   - Admin API: `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java`
2. **Package and class names** — `cz.bsl.favourites`, `cz.bsl.czh.favourites`, and every
   `Favourite*`/`Favourites*` class name → US spelling (files move/`mv`, no git commands).
3. **Comments, config keys, metric names, log/pid file names, nginx context roots** — every
   remaining UK-spelled string, including UK -ise words (`serialise`, `behaviour`, …).

**What intentionally stays UK-spelled (do NOT change):**

| Item                                                                                                            | Why                                                                      |
|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| Maven master artifactId `czh-favourites` (root + module `<parent>` refs)                                        | Prompt: "master project name and maven names can remain"                 |
| Module directory names `czh-favorites-api/app/db/…`                                                             | Already US spelling                                                      |
| Repo/dir name `/java/czh/czh-favourites`, `czh-favourites.geany`                                                | Master project name                                                      |
| `spring.application.name: czh-favourites`, GUI title `czh-favourites Vision`                                    | Master project name (mirrors artifactId)                                 |
| `README.md` title, `RELEASE_PROCESS.md` GitHub/Azure URLs                                                       | Project/repo name                                                        |
| DB table/index names `GIS_FAV_GROUP`, `GIS_FAV_WAGER`, delta scripts, `dao-test-schema.sql`                     | `fav` is an abbreviation, not UK spelling; schema is deployed            |
| `references/` tree, `ai-prompts/`, `ai-output/`, `.planning/plans/SP*`, `.planning/state/SP*`, `.planning/*.md` | Historical/reference documents — records of past work, not code          |
| `.claude/settings.local.json` permission paths                                                                  | They contain the repo path `/java/czh/czh-favourites/...` (project name) |

### The correct endpoint contracts (from the references)

**Player API — `FavoriteWagerResource`** (player identity via `X-Player-Id` header, as today):

| Method | Path (new)                                                                | Notes                                  |
|--------|---------------------------------------------------------------------------|----------------------------------------|
| POST   | `/{version}/draw-games/favorite-wagers/wagers`                            | create wager                           |
| GET    | `/{version}/draw-games/favorite-wagers/wagers?group={g}&game-names={csv}` | list wagers                            |
| GET    | `/{version}/draw-games/favorite-wagers/wagers/{id}`                       | get wager                              |
| PUT    | `/{version}/draw-games/favorite-wagers/wagers/{id}`                       | update wager                           |
| DELETE | `/{version}/draw-games/favorite-wagers/wagers/{id}`                       | delete wager                           |
| POST   | `/{version}/draw-games/favorite-wagers/groups`                            | create group                           |
| GET    | `/{version}/draw-games/favorite-wagers/groups`                            | list groups                            |
| GET    | `/{version}/draw-games/favorite-wagers/groups/{id}`                       | get group (`{id}` is the group number) |
| PUT    | `/{version}/draw-games/favorite-wagers/groups/{id}`                       | update group                           |
| DELETE | `/{version}/draw-games/favorite-wagers/groups/{id}`                       | delete group                           |

**Admin API — `AdminFavoriteWagerResource`** (read-only, `playerId` trusted path param, as today):

| Method | Path (new)                                                                                              | Notes              |
|--------|---------------------------------------------------------------------------------------------------------|--------------------|
| GET    | `/admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers?groupNumber={g}&game-names={csv}` | list player wagers |
| GET    | `/admin/{version}/player/{playerId}/draw-games/favorite-wagers/wagers/{wagerId}`                        | get wager          |
| GET    | `/admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups/{groupNumber}`                    | get group          |

Plus one retained extra (not in the reference, but the Vision GUI Player Lookup tab depends on it):

| GET | `/admin/{version}/player/{playerId}/draw-games/favorite-wagers/groups` | list groups — keep, re-pathed |

Legacy integration tests call these with version literal `v1`
(`/dg/api/v1/draw-games/favorite-wagers/wagers`), and `DGSubsAdminController` in czh-dgsubs uses
the same house pattern (`@RequestMapping("/admin")` + `@GetMapping("/{version}/player/...")`,
version accepted but ignored). Follow that: add `@PathVariable("version") String version`
parameters that are accepted and ignored, documented in Javadoc.

**Decisions already made (flag for human veto, do not re-litigate):**

- POST keeps `201 Created` + `Location` header (reference Javadoc only says `@HTTP 200`; current
  behaviour is more RESTful and its tests assert it). `Location` is re-shaped to the new path,
  echoing the request's version segment.
- Admin list keeps working with **no** query params (GUI calls it bare); `groupNumber` /
  `game-names` are optional filters per the reference, validated only when present.
- nginx context roots `/favourites` and `/favourites-mgmt` (SP10 invention) are renamed to
  `/favorites` and `/favorites-mgmt` in Phase 4: they are URL paths, and the prompt says
  hallucinated UK paths must change. This touches the **local test fixture only** — external
  gateway wiring is out of scope for this plan. The public↔backend path contract other teams
  need is recorded in `.requirements/integrations/gateway-paths.md` (verified against
  `references/gis/`).
- Sanename/grep anchor becomes `favorites` (`// Grep anchor: favorites`).

### Ground rules for every phase

- **No git commands.** Moves are plain `mv`; deletions `rm`. The human reviews and commits.
- Build/test with `./mvn.sh …` (Java 25 environment). Fastest loop for a single test class:
  `./mvn.sh -pl czh-favorites-app -am test -Dtest=ClassName` (api module: `-pl czh-favorites-api`).
- Tests-first where tests change behaviour (Phases 1–2): change the test URLs first, confirm red,
  then fix the controller, confirm green. Phases 3–4 are mechanical renames validated by the
  existing suite.
- `references/`, `.planning/`, `ai-prompts/`, `ai-output/` are never edited.
- **Sed footgun:** `czh-favourites` contains the substring `favourites`. Never run a blanket
  `s/favourites/favorites/g`. Always shield it first, e.g.
  `sed -i 's/czh-favourites/@@PROJ@@/g; s/favourites/favorites/g; s/Favourites/Favorites/g; s/favourite/favorite/g; s/Favourite/Favorite/g; s/@@PROJ@@/czh-favourites/g'`
  — and grep-verify afterwards.

---

## Phase 1 — Player API paths to the reference contract

### Goal
`FavouritesController` serves all 10 player endpoints under
`/{version}/draw-games/favorite-wagers/...`; all player-path test URLs updated; suite green.

### Files to read before starting
- `references/gis/components/czh-gateway-draw-games-rest-api/src/main/java/com/gtech/esa/gateway/rest/drawgames/FavoriteWagerResource.java` (the contract)
- `.requirements/integrations/gateway-paths.md` (the verified public↔backend path contract, §2.1 + §3)
- `czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java`
- `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java`
- `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java`
- `czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java`
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java`

### Tasks (tests first)

1. **Update test URL strings** (classes still keep UK names until Phase 3):
   - `FavouritesWagerControllerTest`: every `"/favourites/wagers…"` →
     `"/v1/draw-games/favorite-wagers/wagers…"` (list, `/wagers/{id}`, create POST target).
   - `FavouritesGroupControllerTest`: `"/favourites/groups…"` →
     `"/v1/draw-games/favorite-wagers/groups…"` (create, get `…/groups/` + GROUP, update, delete, list).
   - `FavouritesValidationTest`: incl. the `"/favourites/wagers?game-names=…"` 400-case URLs →
     `"/v1/draw-games/favorite-wagers/wagers?game-names=…"`, and `"/favourites/groups/9"` →
     `"/v1/draw-games/favorite-wagers/groups/9"`.
   - `FavouritesRestSmokeTest`: the two seeding exchanges `"/favourites/groups"` and
     `"/favourites/wagers"` → `"/v1/draw-games/favorite-wagers/groups"` / `"/v1/draw-games/favorite-wagers/wagers"`.
   - `AdminFavouritesControllerTest` **seeding calls only** (they POST via the player API, ~lines
     36–53): update those player-path URLs too, so this class stays green after Phase 1. Its
     `/admin/favourites/...` assertion URLs are Phase 2 — leave them.
   - `test/vision-test.sh`: the two seed cURLs
     `http://127.0.0.1:$APP_PORT/favourites/groups` and `.../favourites/wagers` →
     `.../v1/draw-games/favorite-wagers/groups|wagers` (the harness itself is not run until Phase 4/5;
     its context-root assertions are Phase 4).
   - Fixture-path strings (`"favourites/wager/create.req.js"` etc.) do NOT change yet — Phase 4.
2. Run the controller tests, confirm they are **red** (404s — paths not yet served).
3. **Fix `FavouritesController`:**
   - Class mapping: `@RequestMapping("/{version}/draw-games/favorite-wagers")`.
   - Method mappings: `/wagers`, `/wagers/{id}`, `/groups`, `/groups/{id}` (reference uses `{id}`
     for player group paths — bind it and pass to `validator.validateGroupNumber`).
   - Add `@PathVariable("version") String version` to every handler (explicit name — house style,
     matches DGSubs). Value is ignored; document in Javadoc: *"version path segment accepted for
     gateway compatibility; currently ignored."*
   - Location headers: `URI.create("/" + version + "/draw-games/favorite-wagers/wagers/" + created.getId())`
     and `…/groups/" + created.getGroupNumber()`.
   - Javadoc: base-path line now describes `/{version}/draw-games/favorite-wagers`.
   - Query params stay `group` + `game-names` (already match the reference).
4. Run the four test classes green:
   `./mvn.sh -pl czh-favorites-app -am test -Dtest='FavouritesWagerControllerTest,FavouritesGroupControllerTest,FavouritesValidationTest,FavouritesRestSmokeTest'`
5. Then `./mvn.sh install` (full suite — `AdminFavouritesControllerTest` must still be green on the
   untouched admin paths).

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/favourites/web/FavouritesController.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesWagerControllerTest.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesGroupControllerTest.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/FavouritesValidationTest.java \
  -w "POST still returns 201+Location; reference Javadoc says 200 — kept 201, Location re-shaped. Veto if the gateway contract needs 200."
```

---

## Phase 2 — Admin API paths + GUI calls

### Goal
`AdminFavouritesController` serves the reference admin contract under
`/admin/{version}/player/{playerId}/draw-games/favorite-wagers/...`; GUI Player Lookup calls the
new paths; suite green.

### Files to read before starting
- `references/gis/components/gis/gis-site/src/main/java/com/gtech/gis/site/resources/AdminFavoriteWagerResource.java` (the contract)
- `.requirements/integrations/gateway-paths.md` (the verified public↔backend path contract, §2.2 + §7.1 — admin list filter is `groupNumber`, per the Java reference)
- `czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java`
- `czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java`
- `czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js`
- `czh-favorites-app/src/main/java/cz/bsl/favourites/config/GuiResourceConfig.java` (Javadoc mentions `/admin/favourites/*`)

### Tasks (tests first)

1. **Update `AdminFavouritesControllerTest`** admin URLs:
   - `/admin/favourites/players/{p}/wagers` → `/admin/v1/player/{p}/draw-games/favorite-wagers/wagers`
   - `/admin/favourites/players/{p}/wagers/{id}` → `/admin/v1/player/{p}/draw-games/favorite-wagers/wagers/{id}`
     (incl. the 404 case `wagers/999999999`)
   - `/admin/favourites/players/{p}/groups` → `/admin/v1/player/{p}/draw-games/favorite-wagers/groups`
   - `/admin/favourites/players/{p}/groups/{GROUP}` → `/admin/v1/player/{p}/draw-games/favorite-wagers/groups/{GROUP}`
   - Optionally add one filtered list case (`?groupNumber=…`) asserting the filter works.
   Run it red.
2. **Fix `AdminFavouritesController`:**
   - Class mapping: `@RequestMapping("/admin")` (matches DGSubsAdminController style).
   - Mappings (all with explicit `@PathVariable("version")`, `@PathVariable("playerId")`, …):
     - `@GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/wagers")` — add optional
       `@RequestParam(name = "groupNumber", required = false)` and `game-names` filters per the
       reference. When `groupNumber` is present, format-validate via `validator.validateId` (no
       range validation — admin boundary doc stays). Parse `game-names` through the shared helper
       (task 3). No params → list all (current behaviour, GUI relies on it).
     - `@GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/wagers/{wagerId}")`
     - `@GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/groups")` (retained extra)
     - `@GetMapping("/{version}/player/{playerId}/draw-games/favorite-wagers/groups/{groupNumber}")`
   - Update Javadoc: new base path, note the retained list-groups endpoint is an addition beyond
     the legacy reference (required by the Vision GUI).
3. **Extract the CSV parse** so player and admin controllers share it (currently inline in
   `FavouritesController.listWagers`): add
   `public static List<String> parseGameNamesCsv(String csv)` to `FavouritesValidator` —
   null/blank → `List.of()`, else split on `,`, strip, drop empties, throw `IllegalArgumentException`
   when > 50 entries. **Test-first:** add cases to `FavouritesValidatorTest` (null → empty, blank →
   empty, strips spaces and drops empties, 50 ok, 51 → IAE). Switch both controllers to it; the
   existing `FavouritesValidationTest` game-names cases must stay green.
4. **GUI:** `player-lookup.js` fetches
   `'/admin/v1/player/' + encoded + '/draw-games/favorite-wagers/wagers'` and `'…/groups'`.
   Update the file-header comment that names the old paths.
5. `GuiResourceConfig` Javadoc: `/admin/favourites/*` mention → `/admin/v1/player/*/draw-games/favorite-wagers/*`.
6. `./mvn.sh -pl czh-favorites-app -am test -Dtest='AdminFavouritesControllerTest,FavouritesValidatorTest,FavouritesWagerControllerTest,FavouritesGroupControllerTest'`
   then `./mvn.sh install`.

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java \
  -r czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java \
  -r czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java \
  -w "Admin list-groups endpoint kept although absent from the legacy reference (Vision GUI needs it) — re-pathed, documented in Javadoc." \
  -w "Admin list gains optional groupNumber + game-names filters per the reference; bare call still lists all."
```

---

## Phase 3 — Package and class renames (mechanical sweep)

### Goal
Every Java package, class, file, and package-name string uses US spelling; full build + suite green.
No behaviour change.

### Rename map

Packages (directories move with `mv -p` style `mkdir`+`mv`):
- `czh-favorites-app/src/main/java/cz/bsl/favourites/**` → `cz/bsl/favorites/**`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/**` → `cz/bsl/czh/favorites/**`
- Same for `src/test/java/**` in `czh-favorites-app` and `czh-favorites-api`.

Classes (file + class name + every reference):

| Old                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | New                                                                                          |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `FavouritesApplication`                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `FavoritesApplication`                                                                       |
| `FavouritesProperties`                                                                                                                                                                                                                                                                                                                                                                                                                                                       | `FavoritesProperties`                                                                        |
| `FavouritesController`                                                                                                                                                                                                                                                                                                                                                                                                                                                       | `FavoritesController`                                                                        |
| `AdminFavouritesController`                                                                                                                                                                                                                                                                                                                                                                                                                                                  | `AdminFavoritesController`                                                                   |
| `FavouritesErrorHandler`                                                                                                                                                                                                                                                                                                                                                                                                                                                     | `FavoritesErrorHandler`                                                                      |
| `FavouriteWagerDto` / `FavouriteGroupDto` / `FavouriteWagerPageDto` / `FavouriteGroupPageDto`                                                                                                                                                                                                                                                                                                                                                                                | `FavoriteWagerDto` / `FavoriteGroupDto` / `FavoriteWagerPageDto` / `FavoriteGroupPageDto`    |
| `FavouriteWagerRecord` / `FavouriteGroupRecord`                                                                                                                                                                                                                                                                                                                                                                                                                              | `FavoriteWagerRecord` / `FavoriteGroupRecord`                                                |
| `FavouritesWagerDao(Impl)` / `FavouritesGroupDao(Impl)`                                                                                                                                                                                                                                                                                                                                                                                                                      | `FavoritesWagerDao(Impl)` / `FavoritesGroupDao(Impl)`                                        |
| `FavouritesService` / `DefaultFavouritesService` / `FavouritesConverter` / `FavouritesValidator`                                                                                                                                                                                                                                                                                                                                                                             | `FavoritesService` / `DefaultFavoritesService` / `FavoritesConverter` / `FavoritesValidator` |
| `FavouritesMetrics`                                                                                                                                                                                                                                                                                                                                                                                                                                                          | `FavoritesMetrics`                                                                           |
| Tests: `FavouriteWagerDtoTest`, `FavouriteGroupDtoTest`, `FavouritePageDtoTest`, `FavouritesWagerDaoTest`, `FavouritesGroupDaoTest`, `FavouritesMetricsTest`, `DefaultFavouritesServiceTest`, `FavouritesConverterTest`, `FavouritesValidatorTest`, `FavouritesRestSmokeTest`, `FavouritesApplicationSmokeTest`, `AdminFavouritesControllerTest`, `FavouritesErrorHandlerTest`, `FavouritesGroupControllerTest`, `FavouritesValidationTest`, `FavouritesWagerControllerTest` | same with `Favorite`/`Favorites`                                                             |
| Move-only (package change, name already fine): `WagerDto`, `WagerDtoTest`, `DataSourceConfig`, `GuiResourceConfig`, `VisionBaseContextFilter`, `CdcReader`, `PlayerContextResolver`, `CdcReaderTest`, `PlayerContextResolverTest`, `TestDatabaseConfig`, `DaoTestConfig`, `JamcrestUtils`, `AbstractRestTest`, `VisionHarnessMain`, `VisionBaseTest`                                                                                                                         | —                                                                                            |

### Tasks

1. **Move directories** with `mkdir` + `mv` (no git): the five package roots above (main + test ×
   2 modules). Rename class files with `mv Old.java New.java`.
2. **Rewrite contents** — in every `.java` under `czh-favorites-api/` and `czh-favorites-app/`
   (src/main + src/test): package lines, imports, type names, `// Grep anchor: favourites` →
   `favorites`, local variables/fields with `favourite` in the name (grep afterwards: `grep -rn "favour" --include='*.java' czh-favorites-api/src czh-favorites-app/src` must return nothing).
   Use the shielded sed from the ground rules (`czh-favourites` protection NOT needed inside .java
   files, but harmless; **it is needed for the non-Java files below**).
3. **Non-Java occurrences of the package strings** (must move in lockstep or logging/scan breaks):
   - `czh-favorites-app/pom.xml` — `<mainClass>cz.bsl.favourites.FavouritesApplication</mainClass>` → `cz.bsl.favorites.FavoritesApplication`.
   - `czh-favorites-app/src/main/java/cz/bsl/favourites/FavouritesApplication.java` (now moved) —
     `scanBasePackages = {"cz.bsl.favourites", "cz.bsl.czh.favourites"}` → favorites variants.
   - `czh-favorites-app/src/main/resources/application.yml` — logger `cz.bsl.favourites:` → `cz.bsl.favorites:`.
   - `czh-favorites-app/src/main/resources/logback-spring.xml` — logger name `cz.bsl.favourites` → `cz.bsl.favorites`.
   - `czh-favorites-app/src/test/resources/application-test.yml` — logger `cz.bsl.favourites:` → `cz.bsl.favorites:`.
   - `czh-favorites-app-rpm/src/main/resources/config/logback-spring.xml` — both loggers (`cz.bsl.favourites`, `cz.bsl.czh.favourites`) → favorites.
   - `czh-favorites-app-rpm/src/main/resources/config/application.yml` — logger `cz.bsl.favourites:` → `cz.bsl.favorites:`.
   - `test/vision-test.sh` — `harness_main=cz.bsl.czh.favourites.test.VisionHarnessMain` → `cz.bsl.czh.favorites.test.VisionHarnessMain`.
   - `czh-favorites-app-rpm/src/main/resources/bin/kill-service.sh`, `stop-service.sh`, `status-service.sh`, `start-service.sh` — no package strings, but verify with grep.
   - Javadoc `{@link FavouritesX}` references and `cz.bsl.favourites.web` prose in the two
     controllers, error handler, `GuiResourceConfig`, `VisionBaseContextFilter`, `AbstractRestTest`
     — follow the rename.
   - `DaoTestConfig` bean methods `favouritesGroupDao`/`favouritesWagerDao` → `favoritesGroupDao`/`favoritesWagerDao`.
4. **Validate:** `./mvn.sh install` — full reactor, all tests green. This is the phase gate; do not
   proceed on red.
5. **Grep gate:** `grep -rn "favour" --include='*.java' czh-favorites-api czh-favorites-app | grep -v target` → empty.

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/main/java/cz/bsl/favorites/FavoritesApplication.java \
  -r czh-favorites-app/src/main/java/cz/bsl/favorites/web/FavoritesController.java \
  -r czh-favorites-api/src/main/java/cz/bsl/czh/favorites/api/FavoriteWagerDto.java \
  -r czh-favorites-app/pom.xml \
  -w "Pure mechanical rename (packages, classes, files, package strings in yml/xml/sh). Maven artifactId czh-favourites intentionally unchanged."
```

---

## Phase 4 — Config, metrics, URL-prefix and prose spelling sweep

### Goal
Every remaining UK-spelled *string* is US: config prefix, metric names, pool/pid/log names, nginx
context roots, fixture directory, comment prose (incl. -ise words). Suite + vision harness green.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/favorites/config/FavoritesProperties.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/metrics/FavoritesMetrics.java`
- `czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/CdcReader.java`
- `czh-favorites-app/src/main/resources/application.yml`, `logback-spring.xml`
- `czh-favorites-app/src/test/resources/application-test.yml`
- `czh-favorites-app-rpm/src/main/resources/config/application.yml`, `logback-spring.xml`
- `test/fixtures/vision-nginx/conf.d/vision-demo.conf`, `test/vision-test.sh`
- `czh-favorites-app/src/test/java/cz/bsl/czh/favorites/test/it/VisionBaseTest.java`

### Tasks

1. **Config prefix:** `FavoritesProperties` `@ConfigurationProperties(prefix = "favourites")` →
   `"favorites"`; `application.yml` (app) `favourites:` block → `favorites:`. `CdcReader`
   `@Value("${favourites.cdc-path:/run/mx/cdc}")` → `${favorites.cdc-path:...}` (check
   `CdcReaderTest` for any property override string).
2. **Metrics:** `FavoritesMetrics.WAGER_COUNTER` `"favourites.wager.operation"` →
   `"favorites.wager.operation"`; `GROUP_COUNTER` likewise; Javadoc `favourites_wager_operation_total`
   → `favorites_…`. `FavouritesRestSmokeTest`→`FavoritesRestSmokeTest` assertion strings
   `favourites_wager_operation_total` → `favorites_wager_operation_total` (2 places + comment).
   GUI `operations.js`: discovery names `favourites_wager_operation_total`/`favourites_group_operation_total`
   (3 places) + comments. `FavouritesMetricsTest`/`DefaultFavouritesServiceTest` use the constants — no string edits.
3. **Pool / DB / pid / log names:**
   - `application.yml` (app): `pool-name: FavouritesHikariPool` → `FavoritesHikariPool`;
     `jdbc:hsqldb:mem:favourites_test` (test yml) → `favorites_test`;
     `FavouritesTestPool` (test yml + rpm yml) → `FavoritesTestPool`;
     `favourites_dao_test` (`DaoTestConfig`) → `favorites_dao_test`.
   - `FavoritesApplication`: `PidFile.createPid("/var/run/czh", "favourites.pid")` → `"favorites.pid"`
     — this also fixes `kill-service.sh` (it already reads `favorites.pid`; the app used to write
     `favourites.pid` so kill never matched).
   - `czh-favorites-app/src/main/resources/logback-spring.xml`:
     `/var/log/gtech/favourites/favourites.log` and the rolling pattern →
     `/var/log/gtech/favorites/favorites.log` / `favorites.%d{...}.log`.
     (RPM logback already writes `/var/log/favorites/spring.log` — US; the gtech/favorites vs
     favorites directory discrepancy is pre-existing, leave the structure, respell only.)
4. **nginx context roots + vision harness:** `test/fixtures/vision-nginx/conf.d/vision-demo.conf`:
   `/favourites-mgmt` → `/favorites-mgmt` (all locations, comments, `X-Vision-Base`), `/favourites/`
   → `/favorites/`. `test/vision-test.sh`: every `/favourites-mgmt` and `/favourites/` URL in
   assertions (incl. `admin/favourites` → `admin/v1/player/...` wait — no: the proxy strips the
   context, so the browser URL becomes `/favorites/admin/v1/player/...`; update all assertion URLs
   accordingly). `VisionBaseTest`: `BASE = "/favourites-mgmt"` → `"/favorites-mgmt"` (+ Javadoc).
   `VisionBaseContextFilter` + `GuiResourceConfig` Javadoc examples → `/favorites-mgmt`.
5. **Fixture directory:** `mv czh-favorites-app/src/test/resources/favourites czh-favorites-app/src/test/resources/favorites`
   and update every fixture path string in tests: `"favourites/wager/…"`, `"favourites/group/…"`,
   `"favourites/admin/…"` → `"favorites/…"`, plus the `JamcrestUtils` Javadoc examples
   (`favourites/list-wagers.resp.js` etc.).
6. **Prose sweep:** in `czh-favorites-api/`, `czh-favorites-app/` (main + test + resources/gui),
   and `test/`: `favourite`→`favorite`, `Favourite`→`Favorite`, `serialise/deserialise`→`serialize/deserialize`,
   `behaviour`→`behavior` (known spots: `FavoriteWagerRecord`, `FavoritesWagerDaoImpl`,
   `FavoriteWagerDto`, `WagerDto`, `JamcrestUtils`, `VisionBaseTest`, `api.js`, `player-lookup.js`).
   Shield `czh-favourites` (ground-rules sed). `spring.application.name: czh-favourites`, GUI title
   `czh-favourites Vision`, and the smoke-test title assertion stay.
7. **Validate:** `./mvn.sh install` green; then `test/vision-test.sh` end-to-end (nginx binary at
   `/java/czh/czh-nginx-gateway/src/nginx/install/sbin/nginx` — verified present).
8. **Grep gate:** `grep -rni "favour" czh-favorites-api czh-favorites-app czh-favorites-db czh-favorites-app-rpm czh-favorites-db-rpm test bin js pom.xml | grep -v target | grep -v "czh-favourites"` → empty.

```sh
~/bin/geany-progress done 4 \
  -r czh-favorites-app/src/main/resources/application.yml \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favorites/metrics/FavoritesMetrics.java \
  -r test/fixtures/vision-nginx/conf.d/vision-demo.conf \
  -r test/vision-test.sh \
  -w "nginx context roots renamed /favourites -> /favorites and /favourites-mgmt -> /favorites-mgmt (local test fixture only). External gateway wiring is out of scope — contract recorded in .requirements/integrations/gateway-paths.md." \
  -w "pid file now favorites.pid which incidentally fixes kill-service.sh; stop-service.sh and status-service.sh still reference different pid names (pre-existing bug, left alone)." \
  -w "Metric names changed to favorites_*_operation_total — dashboards/alerts keyed on the old names must be updated."
```

---

## Phase 5 — Routing map rewrite + final verification

### Goal
`.requirements/design/routing.md` reflects the renamed world; a final sweep proves no UK spelling
remains outside the allowed list; full build (incl. RPM) validated; sign-off.

### Tasks

1. **Rewrite `.requirements/design/routing.md`** per the `routing-map` skill: re-read the real tree
   (`find`/`grep`, do not trust this plan), rebuild Section 1 (feature → sanename `favorites` table
   with verified paths), Section 2 (code map with new packages/endpoints/metric names), and the
   ports + nginx tables (`/favorites`, `/favorites-mgmt`, new endpoint shapes). Update the grep
   anchor line to `favorites`.
2. **Final grep sweep** — each must return nothing (or only the allowed items):
   - `grep -rni "favour" czh-favorites-api czh-favorites-app czh-favorites-db czh-favorites-app-rpm czh-favorites-db-rpm test bin js .github pom.xml README.md 2>/dev/null | grep -v target | grep -v "czh-favourites"`
     → empty. (`README.md` title and `spring.application.name`/GUI title contain `czh-favourites`
     only, which the grep -v shields.)
   - `grep -rn "serialis\|behaviour\|colour\|initialis\|customis\|analyse\|catalogue" czh-favorites-api czh-favorites-app test 2>/dev/null | grep -v target` → empty.
   - `grep -rn "favourites/" czh-favorites-app/src/test --include='*.java'` → empty (fixture paths moved).
3. **Full validation:**
   - `./mvn.sh install` (whole reactor, all tests)
   - `./mvn.sh install -Drpm=true` (RPM modules; needs rpmbuild — if unavailable, note it as skipped)
   - `test/vision-test.sh` (already run in Phase 4; re-run as the final gate)
4. **Note for the human reviewer:** `czh-favourites.geany` still lists old file paths in its
   `FILE_NAME_*` session entries (Geany-managed; harmless — Geany drops missing files). Not edited.

```sh
~/bin/geany-progress done 5 \
  -r .requirements/design/routing.md \
  -w "Historical .planning/SP* docs and ai-prompts/ still mention old names by design (records of past work)." \
  -w "Maven czh-favourites artifactId, spring.application.name, GUI title intentionally still UK project name per prompt."
```

---

## Appendix — quick reference of new public strings

| Kind                   | Old                                                                   | New                                                                 |
|------------------------|-----------------------------------------------------------------------|---------------------------------------------------------------------|
| Player base path       | `/favourites`                                                         | `/{version}/draw-games/favorite-wagers`                             |
| Admin base path        | `/admin/favourites`                                                   | `/admin/{version}/player/{playerId}/draw-games/favorite-wagers`     |
| Admin list filters     | (none)                                                                | `groupNumber`, `game-names` (optional)                              |
| Config prefix          | `favourites:`                                                         | `favorites:`                                                        |
| CDC property           | `favourites.cdc-path`                                                 | `favorites.cdc-path`                                                |
| Metrics                | `favourites.{wager,group}.operation` → `favourites_*_operation_total` | `favorites.{wager,group}.operation` → `favorites_*_operation_total` |
| Hikari pools           | `FavouritesHikariPool` / `FavouritesTestPool`                         | `FavoritesHikariPool` / `FavoritesTestPool`                         |
| Log file               | `/var/log/gtech/favourites/favourites.log`                            | `/var/log/gtech/favorites/favorites.log`                            |
| Pid file               | `/var/run/czh/favourites.pid`                                         | `/var/run/czh/favorites.pid`                                        |
| nginx contexts         | `/favourites`, `/favourites-mgmt`                                     | `/favorites`, `/favorites-mgmt`                                     |
| Test fixtures dir      | `src/test/resources/favourites/`                                      | `src/test/resources/favorites/`                                     |
| Packages               | `cz.bsl.favourites`, `cz.bsl.czh.favourites`                          | `cz.bsl.favorites`, `cz.bsl.czh.favorites`                          |
| Grep anchor / sanename | `favourites`                                                          | `favorites`                                                         |
