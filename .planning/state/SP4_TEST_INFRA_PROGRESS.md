# SP4: Test Infrastructure — Progress

| Phase | Description                                      | Status  |
|-------|--------------------------------------------------|---------|
| 1     | Port `JamcrestUtils` + verify Jamcrest deps      | pending |
| 2     | Write `TestDatabaseConfig` (HSQLDB schema init)  | pending |
| 3     | Write `AbstractRestTest`                         | pending |
| 4     | Write `FavouritesRestSmokeTest`                  | pending |
| 5     | Post-SP sign-off: coverage → refactor → routing  | pending |

## Resume instructions

1. Read `.planning/plans/SP4_TEST_INFRA_PLAN.md`
2. Find the first phase marked **pending**
3. Read only the **Files to read** listed for that phase
4. Implement, run `./mvn.sh -pl czh-favorites-app test-compile`, confirm clean
5. Run `~/bin/geany-progress done N ...` as specified at the end of the phase
