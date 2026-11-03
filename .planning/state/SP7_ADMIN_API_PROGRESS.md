# SP7: Admin API — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | AdminFavoritesController (4 read-only endpoints) | complete |
| 2 | Admin fixtures + AdminFavoritesControllerTest | complete |
| 3 | Sign-off (routing map) | complete |

## Review notes

### Phase 1 — AdminFavoritesController (4 read-only endpoints)

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java`

### Phase 2 — Admin fixtures + AdminFavoritesControllerTest

⚠ norun active — run ./mvn.sh -pl czh-favorites-app test to validate

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/favorites/web/AdminFavoritesControllerTest.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/resources/favorites/admin/`

### Phase 3 — Sign-off (routing map)

⚠ 1 medium security fix applied (playerId length guard). 2 low findings remain (see review above). norun active — run ./mvn.sh -pl czh-favorites-app test

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/favorites/web/AdminFavoritesController.java`
- `/java/czh/czh-favorites/.requirements/design/routing.md`
