# SP1: Project Scaffold — Progress

| Phase | Description                                                             | Status  |
|-------|-------------------------------------------------------------------------|---------|
| 1     | Fix root POM modules + write three child module POMs                    | complete |
| 2     | Application entry point, FavouritesProperties, application.yml, logback | complete |
| 3     | DataSourceConfig, application-test.yml (H2), smoke test                 | complete |
| 4     | bin/coverage.sh written and executable                                  | complete |
| 5     | Post-SP sign-off: test → coverage → security → refactor → routing map   | complete |

### Phase 1 — Fix root POM modules + write three child module POMs

Files written:
- pom.xml (<modules> fixed)
- czh-favorites-api/pom.xml
- czh-favorites-db/pom.xml (pom packaging, delta dir created)
- czh-favorites-app/pom.xml
- ./mvn.sh install: BUILD SUCCESS

### Phase 2 — Application entry point, FavouritesProperties, application.yml, logback

Files written:
- czh-favorites-app/src/main/java/cz/bsl/favourites/FavouritesApplication.java
- czh-favorites-app/src/main/java/cz/bsl/favourites/config/FavouritesProperties.java
- czh-favorites-app/src/main/resources/application.yml (port 9290 / mgmt 9291)
- czh-favorites-app/src/main/resources/logback-spring.xml
- ./mvn.sh install: BUILD SUCCESS

### Phase 3 — DataSourceConfig, application-test.yml (HSQLDB), smoke test

Files written:
- czh-favorites-app/src/main/java/cz/bsl/favourites/config/DataSourceConfig.java
- czh-favorites-app/src/test/resources/application-test.yml (HSQLDB, sql.init.mode=never)
- czh-favorites-app/src/test/java/cz/bsl/favourites/FavouritesApplicationSmokeTest.java
- ./mvn.sh install: BUILD SUCCESS — FavouritesApplicationSmokeTest ×2 green

### Phase 4 — bin/coverage.sh written and executable

- bin/coverage.sh written and executable
- bin/coverage.sh ran successfully; target/jacoco-report.csv produced
⚠ FavouritesProperties getter/setter coverage low at SP1 — accepted; closes in SP5 service tests

### Phase 5 — Post-SP sign-off

- All tests green: FavouritesApplicationSmokeTest ×2
- bin/coverage.sh ran; target/jacoco-report.csv produced
⚠ Coverage exceptions: FavouritesApplication.main() (Spring entry point — accepted),
  FavouritesProperties getters/setters (no service logic yet — closes SP5)
- /security-java: no findings (pure config classes, no user input, no resource allocation)
- Refactor pass: clean; application.yml relaxed-binding verified
- .requirements/design/routing.md updated with SP1 package layout
- SP1 COMPLETE — proceed to SP2 (JSON API Models) and SP3 (DAO Layer) in parallel
