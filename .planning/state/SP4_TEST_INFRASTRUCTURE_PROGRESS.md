# SP4: Test Infrastructure — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Port JamcrestUtils + verify deps | complete |
| 2 | Write TestDatabaseConfig | complete |
| 3 | Write AbstractRestTest | complete |
| 4 | Write FavoritesRestSmokeTest | complete |
| 5 | Post-SP sign-off | complete |

## Review notes

### Phase 1 — Port JamcrestUtils + verify deps

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesConverter.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/service/FavoritesConverterTest.java`

### Phase 2 — Write TestDatabaseConfig

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesValidator.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/service/FavoritesValidatorTest.java`

### Phase 3 — Write AbstractRestTest

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/PlayerContextResolver.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/server/CdcReader.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/server/PlayerContextResolverTest.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/server/CdcReaderTest.java`

### Phase 4 — Write FavoritesRestSmokeTest

Files for review:
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/FavoritesService.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/main/java/cz/bsl/czh/favorites/service/DefaultFavoritesService.java`
- `/java/czh/czh-favorites/czh-favorites-app/src/test/java/cz/bsl/czh/favorites/service/DefaultFavoritesServiceTest.java`

### Phase 5 — Post-SP sign-off

⚠ Tests deferred (norun). Run ./mvn.sh -pl czh-favorites-app test to validate SP5.

Files for review:
- `/java/czh/czh-favorites/.requirements/design/routing.md`
