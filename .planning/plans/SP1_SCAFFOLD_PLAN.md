# SP1: Project Scaffold — Detailed Implementation Plan

**Sub-plan:** SP1 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.favourites`  
**Progress state:** `.planning/state/SP1_SCAFFOLD_PROGRESS.md`

## Overview

Fix the root POM's `<modules>` block (currently lists `dgsubs-*` placeholders), write the three
child module POMs, create the Spring Boot application entry point, `application.yml` with all
`favourites.*` config properties, `logback-spring.xml`, a minimal `DataSourceConfig`, and
`bin/coverage.sh`. No business logic. The SP is done when `./mvn.sh install` compiles and the
smoke test passes.

## Context budget per phase

Each phase is sized to fit in one Claude Sonnet 4.6 context window (~30 files or fewer). Read
only the files listed under **Files to read** before implementing the phase — do not load extra
files. After each phase write progress state and confirm the build is green before continuing.

---

## Phase 1 — Fix root POM and write module POMs

### Goal
`./mvn.sh install` builds successfully with three empty (skeleton) modules:
`czh-favorites-api`, `czh-favorites-app`, `czh-favorites-db`.

### Files to read before starting
- `/java/czh/czh-favourites/pom.xml` — current root POM (incorrect `<modules>`)
- `/java/czh/czh-money/pom.xml` — root POM shape to follow (`<modules>`, JaCoCo wiring)
- `/java/czh/czh-money/money-api/pom.xml` — api module POM to mirror
- `/java/czh/czh-money/money-app/pom.xml` — app module POM to mirror
- `/java/czh/czh-money/czh-money-db/pom.xml` — db module POM to mirror

### Tasks

#### 1.1 — Fix `<modules>` in root `pom.xml`

Replace the placeholder module list:
```xml
<modules>
    <module>dgsubs-api</module>
    <module>dgsubs</module>
    <module>dgsubs-admin</module>
</modules>
```
with:
```xml
<modules>
    <module>czh-favorites-api</module>
    <module>czh-favorites-app</module>
    <module>czh-favorites-db</module>
</modules>
```

Everything else in the root POM stays unchanged. JaCoCo `prepare-agent` execution is already
present and active — do not touch it.

**Root POM `artifactId`** must be `czh-favourites` (already correct — verify, do not change).

#### 1.2 — Write `czh-favorites-api/pom.xml`

Model on `/java/czh/czh-money/money-api/pom.xml`. Key points:
- `<parent>` → `czh-favourites` group/version
- `<artifactId>` → `czh-favorites-api`
- `<packaging>` → `jar`
- `<description>` → `czh-favourites JSON API — request/response DTOs, plain-Java validation`
- Dependencies (exact — no versions, resolved by root BOM):
  - `jackson-annotations` (compile)
  - `jakarta.annotation-api` (compile)
  - `junit-jupiter` (test)
  - `jackson-databind` (test)
  - `jackson-datatype-jsr310` (test)
- No Spring Boot dependency in this module.

#### 1.3 — Write `czh-favorites-db/pom.xml`

Model on `/java/czh/czh-money/czh-money-db/pom.xml`:
- `<artifactId>` → `czh-favorites-db`
- `<packaging>` → `pom` (holds delta SQL only; no Java source)
- `<description>` → `czh-favourites DB delta scripts — applied via db2delta tool`
- No dependencies.

Ensure the directory `czh-favorites-db/src/main/delta/` exists (create it now even though SP3
writes the SQL files):
```
czh-favorites-db/src/main/delta/.gitkeep
```

#### 1.4 — Write `czh-favorites-app/pom.xml`

Model on `/java/czh/czh-money/money-app/pom.xml`. Differences from czh-money:
- `<artifactId>` → `czh-favorites-app`
- `<description>` → `czh-favourites Spring Boot 4 application — favourites microservice`
- Dependency on `czh-favorites-api` (not `money-api`)
- **No** `xamcrest` dependency (czh-money uses it for XML assertions; favourites has no XML)
- `<finalName>` → `czh-favorites-app`
- `spring-boot-maven-plugin` `<mainClass>` → `cz.bsl.favourites.FavouritesApplication`
- `<classifier>exec</classifier>` on the repackage execution (same pattern as czh-money — the
  exec jar is used by RPM; the plain jar can be used as a test dependency if needed)
- `db2` profile with `activeByDefault=true` adding `db2jcc4` (optional, same as czh-money)

Required dependencies (compile):
- `czh-favorites-api` (version `${project.version}`)
- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `spring-boot-starter-jdbc`
- `jackson-datatype-jsr310`
- `jakarta.annotation-api`
- `commons-lang3`

Required dependencies (test):
- `spring-boot-starter-test`
- `hsqldb`
- `jamcrest`

### Verification

```bash
./mvn.sh install
```

Expected: BUILD SUCCESS. Three modules compile with no source yet (empty jars are fine at this
stage). If any module fails to resolve the parent, check that `pom.xml` `<relativePath>` is
correct (leave it empty `<relativePath/>` for all three child modules).

### Write progress state

After verification passes, update `.planning/state/SP1_SCAFFOLD_PROGRESS.md`:
change the Phase 1 row `Status` from `pending` to `complete`, and append a review note:

```
### Phase 1 — Fix root POM modules + write three child module POMs

Files written:
- pom.xml (<modules> fixed)
- czh-favorites-api/pom.xml
- czh-favorites-db/pom.xml (pom packaging, delta dir created)
- czh-favorites-app/pom.xml
- ./mvn.sh install: BUILD SUCCESS
```

---

## Phase 2 — Application entry point, config, and logging

### Goal
`FavouritesApplication.java`, `FavouritesProperties.java`, `application.yml`, and
`logback-spring.xml` are written. `./mvn.sh install` still builds successfully (no tests yet).

### Files to read before starting
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/MoneyApplication.java` — entry point style
- `/java/czh/czh-money/money-app/src/main/resources/application.yml` — config style
- `/java/czh/czh-money/money-app/src/main/resources/logback-spring.xml` — logging style
- `.planning/java25-spring-boot-migration.md` — `favourites.*` config property block (8 fields)

### Tasks

#### 2.1 — Write `FavouritesApplication.java`

Path: `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/FavouritesApplication.java`

```java
package cz.bsl.favourites;

import cz.bsl.favourites.config.FavouritesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FavouritesProperties.class)
public class FavouritesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FavouritesApplication.class, args);
    }
}
```

#### 2.2 — Write `FavouritesProperties.java`

Path: `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/config/FavouritesProperties.java`

This replaces the legacy JMX-managed setters in `DefaultFavoriteWagerService`. Use
`@ConfigurationProperties(prefix = "favourites")` with the 8 fields from the migration doc:

```java
package cz.bsl.favourites.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "favourites")
public class FavouritesProperties {

    private int minGroupIndex = 1;
    private int maxGroupIndex = 10;
    private String defaultGroupName = "";
    private int maxFavorites = 50;
    private int maxFavoriteBoards = 200;
    private int kenoReservedGroupIndex = 1;
    private boolean returnEmptyGroups = false;

    // standard getters and setters for all 7 fields
    // (generate; no Lombok — zero deps outside SB4)
}
```

All 7 fields need getters and setters. Write them out explicitly (no Lombok).

#### 2.3 — Write `application.yml`

Path: `czh-favorites-app/src/main/resources/application.yml`

Mirror the czh-money style exactly. Favourites-specific values:
- `server.port`: `9290`
- `spring.application.name`: `czh-favourites`
- `spring.datasource.url`: `jdbc:db2://localhost:50000/PDDB`
- `spring.datasource.driver-class-name`: `com.ibm.db2.jcc.DB2Driver`
- `spring.datasource.username`: `gtkinst1`
- `spring.datasource.password`: `gtkinst1`
- `spring.datasource.hikari.pool-name`: `FavouritesHikariPool`
- HikariCP pool settings: same as czh-money (max 10, min-idle 2, timeouts)
- `management.server.port`: `9291`
- `management.endpoints.web.exposure.include`: `health,info,prometheus`
- `management.endpoint.health.show-details`: `always`
- `management.metrics.export.prometheus.enabled`: `true`
- `logging.level.root`: `INFO`
- `logging.level.cz.bsl.favourites`: `DEBUG`
- `favourites` block with all 7 properties (matching `FavouritesProperties` field names in
  kebab-case):

```yaml
favourites:
  min-group-index: 1
  max-group-index: 10
  default-group-name: ""
  max-favorites: 50
  max-favorite-boards: 200
  keno-reserved-group-index: 1
  return-empty-groups: false
```

Also add Jackson config:
```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    datatype:
      datetime:
        write-dates-as-timestamps: false
```

#### 2.4 — Write `logback-spring.xml`

Path: `czh-favorites-app/src/main/resources/logback-spring.xml`

Copy the czh-money `logback-spring.xml` exactly, replacing:
- Log file path: `/var/log/gtech/money/money.log` → `/var/log/gtech/favourites/favourites.log`
- File pattern: `money.%d{...}.log` → `favourites.%d{...}.log`
- Logger package: `cz.bsl.money` → `cz.bsl.favourites`

### Verification

```bash
./mvn.sh install
```

Expected: BUILD SUCCESS. The app module now contains Java source and compiles.

### Write progress state

Update `.planning/state/SP1_SCAFFOLD_PROGRESS.md`: change the Phase 2 row `Status`
from `pending` to `complete`, and append:

```
### Phase 2 — Application entry point, FavouritesProperties, application.yml, logback

Files written:
- czh-favorites-app/src/main/java/cz/bsl/czh/favourites/FavouritesApplication.java
- czh-favorites-app/src/main/java/cz/bsl/czh/favourites/config/FavouritesProperties.java
- czh-favorites-app/src/main/resources/application.yml (port 9290 / mgmt 9291)
- czh-favorites-app/src/main/resources/logback-spring.xml
- ./mvn.sh install: BUILD SUCCESS
```

---

## Phase 3 — DataSourceConfig, test profile, and smoke test

### Goal
A minimal `DataSourceConfig.java`, an `application-test.yml` that activates H2, and a
`FavouritesApplicationSmokeTest` that starts the Spring context against H2. `./mvn.sh install`
passes including the smoke test.

### Files to read before starting
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/config/DataSourceConfig.java`
- `/java/czh/czh-money/money-app/pom.xml` (H2 test dep, already mirrored in Phase 1)
- `czh-favorites-app/src/main/resources/application.yml` (just written — datasource block)

### Tasks

#### 3.1 — Write `DataSourceConfig.java`

Path: `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/config/DataSourceConfig.java`

Mirror czh-money: Spring Boot 4 auto-configures HikariCP from `spring.datasource.*`. The class
is intentionally minimal — its sole purpose is a named home for future explicit DataSource
overrides and to satisfy convention:

```java
package cz.bsl.favourites.config;

import org.springframework.context.annotation.Configuration;

// Spring Boot auto-configures HikariCP from spring.datasource.* properties.
// Add @Bean overrides here only if explicit DataSource wiring is required.
@Configuration
public class DataSourceConfig {
}
```

No bean methods needed at this stage. SB4 handles it.

#### 3.2 — Write `application-test.yml`

Path: `czh-favorites-app/src/test/resources/application-test.yml`

The test profile replaces the DB2 datasource with H2 so `./mvn.sh install` can run without a
running DB2 instance. The `spring.profiles.active=test` is activated in tests via
`@ActiveProfiles("test")` on the smoke test (see 3.3).

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:favourites_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      pool-name: FavouritesTestPool
  sql:
    init:
      mode: never

logging:
  level:
    root: WARN
    cz.bsl.favourites: DEBUG
```

`spring.sql.init.mode=never` prevents Spring Boot from trying to run `schema.sql` / `data.sql`
automatically — the real schema init is handled in SP4's `TestDatabaseConfig`.

#### 3.3 — Write `FavouritesApplicationSmokeTest.java`

Path: `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/FavouritesApplicationSmokeTest.java`

```java
package cz.bsl.favourites;

import cz.bsl.favourites.config.FavouritesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Verifies the Spring context loads cleanly against H2 and FavouritesProperties binds correctly.
@SpringBootTest
@ActiveProfiles("test")
class FavouritesApplicationSmokeTest {

    @Autowired
    private FavouritesProperties properties;

    @Test
    void contextLoads() {
        assertNotNull(properties);
    }

    @Test
    void defaultPropertiesAreBound() {
        assertEquals(1,  properties.getMinGroupIndex());
        assertEquals(10, properties.getMaxGroupIndex());
        assertEquals(50, properties.getMaxFavorites());
        assertEquals(200, properties.getMaxFavoriteBoards());
    }
}
```

**Note:** At this stage the context will fail to start if Spring tries to auto-create JDBC tables
or if `DataSource` init fails. The `application-test.yml` above sets `sql.init.mode=never`; H2
is an in-memory datasource that starts cleanly without a schema. This will remain correct through
SP4 which adds the proper `TestDatabaseConfig`.

If the context still fails to start due to a missing datasource at test time, add
`spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`
to `application-test.yml` temporarily — but only if the H2 url approach above fails; prefer not
to exclude auto-config.

### Verification

```bash
./mvn.sh install
```

Expected: BUILD SUCCESS with `FavouritesApplicationSmokeTest` passing.

### Write progress state

Update `.planning/state/SP1_SCAFFOLD_PROGRESS.md`: change the Phase 3 row `Status`
from `pending` to `complete`, and append:

```
### Phase 3 — DataSourceConfig, application-test.yml (H2), smoke test

Files written:
- czh-favorites-app/src/main/java/cz/bsl/czh/favourites/config/DataSourceConfig.java
- czh-favorites-app/src/test/resources/application-test.yml (H2, sql.init.mode=never)
- czh-favorites-app/src/test/java/cz/bsl/czh/favourites/FavouritesApplicationSmokeTest.java
- ./mvn.sh install: BUILD SUCCESS — FavouritesApplicationSmokeTest ×2 green
```

---

## Phase 4 — Coverage script

### Goal
`bin/coverage.sh` is written and executable. Running it produces `target/jacoco-report.csv`
(covering only `czh-favorites-api` and `czh-favorites-app` source — `czh-favorites-db` has no
Java).

### Files to read before starting
- `/java/czh/czh-money/bin/coverage.sh` — script to adapt

### Tasks

#### 4.1 — Write `bin/coverage.sh`

Path: `bin/coverage.sh`

Adapt `/java/czh/czh-money/bin/coverage.sh` with these differences:
- Module classfile paths: `czh-favorites-api/target/classes` and `czh-favorites-app/target/classes`
- Module source paths: `czh-favorites-api/src/main/java` and `czh-favorites-app/src/main/java`
- No `money-*` paths.

The script structure stays identical:
1. `cd $(dirname $0)/..` — run from project root
2. `./mvn.sh clean install` — build + test (generates `jacoco.exec` in each module)
3. Resolve `JACOCO_VERSION` from root POM via `mvn exec:exec`
4. Fetch `org.jacoco.cli:nodeps` jar if not already in `target/dependency/`
5. `merge` all `jacoco.exec` files → `target/jacoco-aggregate.exec`
6. `report` → `target/jacoco-report.csv` with `--classfiles` and `--sourcefiles` for both modules

```bash
#!/bin/bash
#
# Build, merge JaCoCo coverage from all modules, and emit a CSV report.
# Derived from /java/czh/czh-money/bin/coverage.sh.
#
cd $(dirname $0)/..
set -e

./mvn.sh clean install

JACOCO_VERSION=$(./mvn.sh -q -Dexec.executable=echo -Dexec.args='${jacoco-maven-plugin.version}' --non-recursive exec:exec 2>/dev/null)
JACOCO_CLI="target/dependency/org.jacoco.cli-${JACOCO_VERSION}-nodeps.jar"

if [ ! -f "$JACOCO_CLI" ]; then
    echo "Fetching JaCoCo CLI ${JACOCO_VERSION}..."
    ./mvn.sh --non-recursive dependency:copy \
        -Dartifact="org.jacoco:org.jacoco.cli:${JACOCO_VERSION}:jar:nodeps" \
        -q
fi

echo "Merging coverage data..."
find . -name "jacoco.exec" -type f | xargs java -jar "$JACOCO_CLI" merge --destfile target/jacoco-aggregate.exec

echo "Generating CSV report..."
java -jar "$JACOCO_CLI" report target/jacoco-aggregate.exec \
  --classfiles czh-favorites-api/target/classes \
  --classfiles czh-favorites-app/target/classes \
  --sourcefiles czh-favorites-api/src/main/java \
  --sourcefiles czh-favorites-app/src/main/java \
  --csv target/jacoco-report.csv

echo "Report generated at: target/jacoco-report.csv"
```

Make it executable:
```bash
chmod +x bin/coverage.sh
```

#### 4.2 — Run coverage.sh

```bash
bin/coverage.sh
```

Expected: script runs, produces `target/jacoco-report.csv`. At this stage coverage will be low
(only the smoke test fires) — that is expected. Confirm the CSV exists and is non-empty.

Check that `target/jacoco-report.csv` contains at least a header row and entries for
`cz.bsl.favourites` classes. If `FavouritesProperties` shows 0% instruction coverage,
note it — the smoke test in Phase 3 exercises property binding but not every getter/setter. This
is an accepted gap at SP1; the service tests in SP5 will cover FavouritesProperties usage fully.

### Write progress state

Update `.planning/state/SP1_SCAFFOLD_PROGRESS.md`: change the Phase 4 row `Status`
from `pending` to `complete`, and append:

```
### Phase 4 — bin/coverage.sh written and executable

- bin/coverage.sh written and executable
- bin/coverage.sh ran successfully; target/jacoco-report.csv produced
⚠ FavouritesProperties getter/setter coverage low at SP1 — accepted; closes in SP5 service tests
```

---

## Phase 5 — Post-SP sign-off

### 5.1 — Full test run

```bash
./mvn.sh install
```

All tests must be green. No `@Disabled` tests. Expected passing tests at this stage:
- `FavouritesApplicationSmokeTest#contextLoads`
- `FavouritesApplicationSmokeTest#defaultPropertiesAreBound`

### 5.2 — Coverage check

```bash
bin/coverage.sh
```

Read `target/jacoco-report.csv`. At SP1 the only production classes are:
- `FavouritesApplication` — `main()` only; excluded from coverage target (Spring wiring, not
  a unit-testable business method). Record as accepted exception.
- `FavouritesProperties` — partial coverage (getters/setters; smoke test exercises binding).
  Record as accepted exception; will close in SP5.
- `DataSourceConfig` — empty class; 100% trivially. Fine.

Record in progress notes:
```
Coverage exceptions at SP1 sign-off:
  FavouritesApplication.main() — Spring entry point, not unit testable. Accepted.
  FavouritesProperties getters/setters — no service logic yet. Will close in SP5.
```

### 5.3 — Security review

Run `/security-java` on the Java added in SP1:
- `FavouritesApplication.java`
- `FavouritesProperties.java`
- `DataSourceConfig.java`

Expected findings: none significant (pure config classes). If any finding is accepted, apply
test-first per the sign-off process before signing off.

### 5.4 — Refactor pass

Review each class added in this SP:
- `FavouritesApplication`: only change allowed is if `@EnableConfigurationProperties` needs
  additional classes registered — defer to SP5. No refactoring needed.
- `FavouritesProperties`: confirm all 7 fields match the migration doc's `application.yml` block
  exactly. Confirm getter/setter naming follows Java conventions.
- `DataSourceConfig`: intentionally empty — correct.
- `application.yml`: confirm all `favourites.*` key names match `FavouritesProperties` Java
  field names in kebab-case (Spring relaxed binding). E.g. `max-favorites` binds to `maxFavorites`.
- `logback-spring.xml`: confirm log file path uses `favourites`, not `money`.

### 5.5 — Routing map

After SP1 there are no sanenamed business components yet, but the package structure exists.
Run `/routing-map` (or update `.requirements/design/routing.md` manually) to record the initial
module/package layout:

```
czh-favorites-api   — JSON API models (empty at SP1; filled in SP2)
czh-favorites-app   — Spring Boot application
  cz.bsl.favourites              — application root (@SpringBootApplication)
  cz.bsl.favourites.config       — configuration (@ConfigurationProperties, DataSourceConfig)
czh-favorites-db    — DB delta scripts (empty at SP1; filled in SP3)
```

### Final sign-off

Update `.planning/state/SP1_SCAFFOLD_PROGRESS.md`: change the Phase 5 row `Status`
from `pending` to `complete`, and append:

```
### Phase 5 — Post-SP sign-off

- All tests green: FavouritesApplicationSmokeTest ×2
- bin/coverage.sh ran; target/jacoco-report.csv produced
⚠ Coverage exceptions: FavouritesApplication.main() (Spring entry point — accepted),
  FavouritesProperties getters/setters (no service logic yet — closes SP5)
- /security-java: no findings
- Refactor pass: clean; application.yml relaxed-binding verified
- .requirements/design/routing.md updated with SP1 package layout
- SP1 COMPLETE — proceed to SP2 (JSON API Models) and SP3 (DAO Layer) in parallel
```

---

## Resume procedure

If the context window is cleared mid-SP, resume as follows:

1. Read `.planning/state/SP1_SCAFFOLD_PROGRESS.md` — find the last `complete` phase in the table.
2. Read this file (`.planning/plans/SP1_SCAFFOLD_PLAN.md`) — go to the next `pending` phase.
3. Read only the **Files to read** listed for that phase.
4. Continue with `/opus-exec`.

Do not re-read completed phases. Do not re-run completed tasks.
