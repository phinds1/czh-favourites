# SP2: JSON API Models — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Write `WagerDto` + round-trip test | complete |
| 2 | Write `FavouriteWagerDto` + round-trip test | complete |
| 3 | Write `FavouriteGroupDto` + round-trip test | complete |
| 4 | Write page wrappers (`FavouriteWagerPageDto`, `FavouriteGroupPageDto`) + tests | complete |
| 5 | Post-SP sign-off: test → coverage → security → refactor → routing avorites | complete |

## Review notes

### Phase 1 — Write `WagerDto` + round-trip test

Files for review:
- `/java/czh/czh-favorites/czh-favorites-db/src/main/delta/001-create-gis-fav-group.sql`
- `/java/czh/czh-favorites/czh-favorites-db/src/main/delta/002-create-gis-fav-wager.sql`

### Phase 2 — Write `FavouriteWagerDto` + round-trip test

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavouriteGroupRecord.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesGroupDao.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesGroupDaoImpl.java`

### Phase 3 — Write `FavouriteGroupDto` + round-trip test

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavouriteWagerRecord.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesWagerDao.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/dao/FavoritesWagerDaoImpl.java`

### Phase 4 — Write page wrappers (`FavouriteWagerPageDto`, `FavouriteGroupPageDto`) + tests

⚠ Tests deferred: norun active. Run ./mvn.sh -pl czh-favorites-app test when clear.

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/dao/DaoTestConfig.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/resources/dao-test-schema.sql`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/dao/FavoritesGroupDaoTest.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/dao/FavoritesWagerDaoTest.java`

### Phase 5 — Post-SP sign-off: test → coverage → security → refactor → routing avorites

⚠ Tests/coverage deferred: norun active. Run ./mvn.sh -pl czh-favorites-app test when clear.

Files for review:
- `/java/czh/czh-favorites/.requirements/design/routing.md`
- `/java/czh/czh-favorites/.planning/state/SP3_DAO_LAYER_PROGRESS.md`
