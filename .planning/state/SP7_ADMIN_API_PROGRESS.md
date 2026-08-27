# SP7: Admin API — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | AdminFavouritesController (4 read-only endpoints) | complete |
| 2 | Admin fixtures + AdminFavouritesControllerTest | complete |
| 3 | Sign-off (routing map) | complete |

## Review notes

### Phase 1 — AdminFavouritesController (4 read-only endpoints)

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java`

### Phase 2 — Admin fixtures + AdminFavouritesControllerTest

⚠ norun active — run ./mvn.sh -pl czh-favorites-app test to validate

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/favourites/web/AdminFavouritesControllerTest.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/resources/favourites/admin/`

### Phase 3 — Sign-off (routing map)

⚠ 1 medium security fix applied (playerId length guard). 2 low findings remain (see review above). norun active — run ./mvn.sh -pl czh-favorites-app test

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java`
- `/java/czh/czh-favourites/.requirements/design/routing.md`
