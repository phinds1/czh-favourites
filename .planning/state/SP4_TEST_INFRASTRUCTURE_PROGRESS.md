# SP4: Test Infrastructure — Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Port JamcrestUtils + verify deps | complete |
| 2 | Write TestDatabaseConfig | complete |
| 3 | Write AbstractRestTest | complete |
| 4 | Write FavouritesRestSmokeTest | complete |
| 5 | Post-SP sign-off | complete |

## Review notes

### Phase 1 — Port JamcrestUtils + verify deps

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesConverter.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesConverterTest.java`

### Phase 2 — Write TestDatabaseConfig

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesValidator.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/FavouritesValidatorTest.java`

### Phase 3 — Write AbstractRestTest

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/PlayerContextResolver.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/server/CdcReader.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/PlayerContextResolverTest.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/czh/favourites/server/CdcReaderTest.java`

### Phase 4 — Write FavouritesRestSmokeTest

Files for review:
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/FavouritesService.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java`
- `/java/czh/czh-favourites/czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/DefaultFavouritesServiceTest.java`

### Phase 5 — Post-SP sign-off

⚠ Tests deferred (norun). Run ./mvn.sh -pl czh-favorites-app test to validate SP5.

Files for review:
- `/java/czh/czh-favourites/.requirements/design/routing.md`
