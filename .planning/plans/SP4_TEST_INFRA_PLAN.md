# SP4: Test Infrastructure — Detailed Implementation Plan

**Sub-plan:** SP4 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.czh.favourites`  
**Progress state:** `.planning/state/SP4_TEST_INFRA_PROGRESS.md`

## Overview

Port `JamcrestUtils` from `czh-money`, write `TestDatabaseConfig` (creates `GIS_FAV_GROUP` and
`GIS_FAV_WAGER` in the HSQLDB test datasource), and write `AbstractRestTest` — the base class for
all SP6/SP7 Jamcrest integration tests. The SP is done when a smoke test boots the full Spring
context against HSQLDB and confirms both tables exist.

**Key decisions baked in:**
- The project already uses HSQLDB (not H2) — confirmed in `application-test.yml`
- `application-test.yml` already exists with `sql.init.mode=never` — keep it unchanged
- Spring Boot 4 removed `TestRestTemplate`; use plain `RestTemplate` with a non-throwing error handler (matches `AbstractIntegrationTest` in czh-money)
- `JamcrestUtils` is ported verbatim from `czh-money/czh-money-test` into package `cz.bsl.czh.favourites.test`

## Context budget per phase

Each phase fits in one Claude Sonnet 4.6 context window. Read **only** the files listed under
**Files to read** before implementing. Mark phase done with `~/bin/geany-progress done N` and
confirm the build is green before the next phase.

---

## Phase 1 — Port `JamcrestUtils`

### Goal
`JamcrestUtils` compiles in package `cz.bsl.czh.favourites.test`.

### Files to read before starting
- `/java/czh/czh-money/czh-money-test/src/test/java/cz/bsl/money/test/JamcrestUtils.java`
- `/java/czh/czh-money/czh-money-test/pom.xml` (Jamcrest + GraalVM dependency versions)
- `czh-favorites-app/pom.xml` (confirm Jamcrest + GraalVM are already declared)

### Tasks

#### 1.1 — Port `JamcrestUtils.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/JamcrestUtils.java`

Copy from `czh-money` verbatim; change only:
- Package declaration: `cz.bsl.czh.favourites.test`
- Class-level Javadoc: update project name from `czh-money` to `czh-favourites`
- Grep anchor comment: `// Grep anchor: favourites`

Do **not** change any method signatures, field names, or logic. This is a direct port.

#### 1.2 — Verify Jamcrest dependency in `czh-favorites-app/pom.xml`

Check that `io.github.teknopaul:jamcrest` and GraalVM JS (`org.graalvm.polyglot:polyglot`,
`org.graalvm.polyglot:js`) are declared. If absent, add them in test scope, using the same
`<version>` as in `/java/czh/czh-money/czh-money-test/pom.xml`.

#### 1.3 — Build verify (compile only)

```bash
./mvn.sh -pl czh-favorites-app test-compile
```

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/JamcrestUtils.java \
  -r czh-favorites-app/pom.xml
```

---

## Phase 2 — Write `TestDatabaseConfig`

### Goal
The HSQLDB test datasource has `GIS_FAV_GROUP` and `GIS_FAV_WAGER` tables when the `test` profile
is active. Uses the `INFORMATION_SCHEMA.TABLES` guard pattern from czh-money so the schema is
only created once even when multiple `@SpringBootTest` contexts share the same in-memory DB.

### Files to read before starting
- `/java/czh/czh-money/money-app/src/test/java/cz/bsl/money/test/TestDatabaseConfig.java`
- `czh-favorites-app/src/test/resources/application-test.yml` (datasource URL, `mode: never`)
- `czh-favorites-app/src/test/resources/dao-test-schema.sql` (HSQLDB DDL already written in SP3 — reuse it)

### Tasks

#### 2.1 — Write `TestDatabaseConfig.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/TestDatabaseConfig.java`

Model on `czh-money`'s `TestDatabaseConfig`. Key points:
- `@TestConfiguration` + `@Profile("test")`
- Single `@Bean` method that takes the `DataSource` (autowired from the test context — the HSQLDB
  instance declared in `application-test.yml`)
- Guard with `INFORMATION_SCHEMA.TABLES` check before running DDL — prevent double-create when
  the Spring context is cached between test classes:

```java
boolean tableExists = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_GROUP'",
    Integer.class) > 0;
if (!tableExists) {
    new ResourceDatabasePopulator(
        new ClassPathResource("dao-test-schema.sql")).execute(dataSource);
}
```

- Return the `JdbcTemplate` so tests can `@Autowired JdbcTemplate` for DB clean-up
- Grep anchor: `favourites`

**Note:** `dao-test-schema.sql` was written in SP3 and already contains HSQLDB-compatible DDL for
both tables. Reuse it — do not write a new SQL file.

#### 2.2 — Build verify

```bash
./mvn.sh -pl czh-favorites-app test-compile
```

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/TestDatabaseConfig.java
```

---

## Phase 3 — Write `AbstractRestTest`

### Goal
`AbstractRestTest` boots the full Spring Boot application on a random port against HSQLDB,
provides a `RestTemplate` that does not throw on 4xx/5xx, and exposes `asPlayer(playerId)` and
`asAdmin(adminId)` header builders.

### Files to read before starting
- `/java/czh/czh-money/czh-money-test/src/test/java/cz/bsl/money/test/AbstractIntegrationTest.java`
- `czh-favorites-app/src/main/java/cz/bsl/favourites/FavouritesApplication.java`
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/config/TestDatabaseConfig.java` (Phase 2 output)
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/JamcrestUtils.java` (Phase 1 output)

### Tasks

#### 3.1 — Write `AbstractRestTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java`

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {FavouritesApplication.class, TestDatabaseConfig.class}
)
@ActiveProfiles("test")
public abstract class AbstractRestTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected RestTemplate restTemplate;

    @Autowired
    protected JamcrestUtils jamcrest;

    @BeforeEach
    void setUpRestTemplate() {
        restTemplate = new RestTemplate();
        // Do not throw on 4xx/5xx — tests read the error body and assert status directly.
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
    }

    @AfterEach
    void resetJamcrest() {
        jamcrest.reset();
    }

    /**
     * Returns headers with {@code X-Player-Id} set. Use for all player-facing endpoint calls.
     * The gateway pre-authenticates the player before forwarding to this service; no session
     * token is needed here.
     */
    protected HttpHeaders asPlayer(String playerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Player-Id", playerId);
        return headers;
    }
}
```

Add a `@Bean JamcrestUtils jamcrestUtils()` to a `@TestConfiguration` inner class or to
`TestDatabaseConfig` — whichever is cleaner. Follow the czh-money pattern exactly.

Grep anchor: `favourites`

#### 3.2 — Build verify

```bash
./mvn.sh -pl czh-favorites-app test-compile
```

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java
```

---

## Phase 4 — Write smoke test

### Goal
`FavouritesRestSmokeTest` verifies the Spring context boots, HSQLDB tables exist, and the
`/actuator/health` endpoint returns 200.

### Files to read before starting
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/AbstractRestTest.java` (Phase 3 output)
- `czh-favorites-app/src/main/resources/application.yml` (management port)

### Tasks

#### 4.1 — Write `FavouritesRestSmokeTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java`

```java
class FavouritesRestSmokeTest extends AbstractRestTest {

    @Test
    void contextLoads_tablesExist() {
        int groupCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_GROUP'",
            Integer.class);
        assertEquals(1, groupCount, "GIS_FAV_GROUP table must exist");

        int wagerCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'GIS_FAV_WAGER'",
            Integer.class);
        assertEquals(1, wagerCount, "GIS_FAV_WAGER table must exist");
    }

    @Test
    void actuatorHealth_returns200() {
        // Use a separate RestTemplate pointed at the management port (9291 in application.yml)
        // or hit /actuator/health on the main port if management port is shared in test.
        // Check application-test.yml: if management port is not overridden, it defaults to 9291.
        // For simplicity in tests, configure management.server.port=${server.port} in
        // application-test.yml so actuator is reachable on the same random port.
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(200, response.getStatusCode().value());
    }
}
```

**Note on management port:** Add `management.server.port: 0` to `application-test.yml` (or use
`${server.port}`) so the actuator is reachable via the same `restTemplate` base URL. This is a
test-only convenience — production keeps port 9291 separate.

#### 4.2 — Build and test verify (norun — skip when norun active)

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

```sh
~/bin/geany-progress done 4 \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java \
  -r czh-favorites-app/src/test/resources/application-test.yml \
  -w "Tests deferred if norun active. Run ./mvn.sh -pl czh-favorites-app test when clear."
```

---

## Phase 5 — Post-SP sign-off

### Tasks

#### 5.1 — Full test + install (skip if norun active)

```bash
./mvn.sh -pl czh-favorites-app test
./mvn.sh install
```

#### 5.2 — Coverage

```bash
bin/coverage.sh
```

`JamcrestUtils` methods will be partially covered by SP6/SP7 integration tests; accept low
coverage on it here. `TestDatabaseConfig` is infrastructure — accept low coverage. Target
`AbstractRestTest` helper methods fully covered by subclass tests.

#### 5.3 — Security

`AbstractRestTest`, `JamcrestUtils`, and `TestDatabaseConfig` are test-scope only. No production
security surface introduced. No security review required for this SP.

#### 5.4 — Refactor

Check `AbstractRestTest` against the czh-money `AbstractIntegrationTest` for any missed helpers
that SP6 will need (e.g. `asAdmin`, clean-DB helper). Add them now if obvious.

#### 5.5 — Routing map

Run `/routing-map` to record `cz.bsl.czh.favourites.test` in `.requirements/design/routing.md`.

#### 5.6 — Write progress state

```sh
~/bin/geany-progress done 5 \
  -r .requirements/design/routing.md \
  -r .planning/state/SP4_TEST_INFRA_PROGRESS.md
```
