# SP10: Vision GUI — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Spring MVC resource handler + HTML shell | complete |
| 2 | Operations tab (operations.js) | complete |
| 3 | Player Lookup tab (player-lookup.js) | complete |
| 4 | Sign-off | complete |

## Review notes

### Phase 1 — Spring MVC resource handler + HTML shell

⚠ RPM only builds with -Drpm=true. Normal ./mvn.sh install skips it (maven.packaging.type=pom).

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app-rpm/pom.xml`
- `/java/czh/czh-favourites/czh-favorites-app-rpm/src/main/resources/service/czh-favorites.service`
- `/java/czh/czh-favourites/czh-favorites-app-rpm/src/main/resources/bin/`

### Phase 2 — Operations tab (operations.js)

⚠ RPM only builds with -Drpm=true. Test with: ./mvn.sh install -Drpm=true -pl czh-favorites-db-rpm

Files for review:
- `/java/czh/czh-favourites/czh-favorites-db-rpm/pom.xml`
- `/java/czh/czh-favourites/czh-favorites-db-rpm/src/main/bin/run-delta.sh`

### Phase 3 — Player Lookup tab (player-lookup.js)

⚠ github-ci profile already existed in root POM (SP1). No change needed.

Files for review:
- `/java/czh/czh-favourites/.github/workflows/build.yml`
- `/java/czh/czh-favourites/pom.xml`

### Phase 4 — Sign-off

⚠ norun active — run ./mvn.sh install -Drpm=true to validate full RPM build

Files for review:
- `/java/czh/czh-favourites/pom.xml`
- `/java/czh/czh-favourites/.requirements/design/routing.md`
