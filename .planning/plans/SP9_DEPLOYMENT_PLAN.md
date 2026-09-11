# SP9: Deployment — Detailed Implementation Plan

**Sub-plan:** SP9 of 10  
**Sanename:** `favourites`  
**Root package:** N/A (no Java — RPM config, shell scripts, YAML CI)  
**Progress state:** `.planning/state/SP9_DEPLOYMENT_PROGRESS.md`

## Overview

Create the two deployment RPM modules (`czh-favorites-app-rpm`, `czh-favorites-db-rpm`),
`systemd` unit, `start/stop/status` shell scripts, GitHub Actions CI YAML, and Azure DevOps
pipeline YAMLs (6 pipelines matching the czh-money pattern).

**Reference:** `/java/czh/czh-money/money-app-rpm/`, `/java/czh/czh-money/czh-money-db-rpm/`,
`/java/czh/czh-money/.azuredevops/non-production/maven/`

---

## Phase 1 — `czh-favorites-app-rpm` module

### Goal
`czh-favorites-app-rpm/pom.xml` compiles cleanly (as `pom` type); the `rpm` profile packages
the fat-jar into an RPM installable to `/opt/czh-favorites/`.

### Files to read before starting
- `/java/czh/czh-money/money-app-rpm/pom.xml` (exact pattern to port)
- `/java/czh/czh-money/money-app-rpm/src/main/resources/service/czh-money.service`
- `/java/czh/czh-money/money-app-rpm/src/main/resources/bin/start-service.sh`
- `/java/czh/czh-money/money-app-rpm/src/main/resources/bin/stop-service.sh`
- `/java/czh/czh-money/money-app-rpm/src/main/resources/bin/status-service.sh`
- `/java/czh/czh-money/money-app-rpm/src/main/resources/bin/version.sh`
- `czh-favorites-app/src/main/resources/application.yml` (confirm port 9290, management 9291)
- `/java/czh/czh-favourites/pom.xml` (root POM groupId, version, rpm-plugin.version)

### Tasks

#### 1.1 — Create `czh-favorites-app-rpm/pom.xml`

Mirror `money-app-rpm/pom.xml` with these substitutions:

| czh-money value                  | czh-favourites value                 |
|----------------------------------|--------------------------------------|
| `czh-money`                      | `czh-favourites` (parent artifactId) |
| `money-app`                      | `czh-favorites-app`                  |
| `money-app-rpm`                  | `czh-favorites-app-rpm`              |
| `/opt/czh-money`                 | `/opt/czh-favorites`                 |
| `czh-money-app.jar`              | `czh-favorites-app.jar`              |
| `/var/log/czh-money/`            | `/var/log/czh-favorites/`            |
| `/var/run/czh-money/`            | `/var/run/czh-favorites/`            |
| `/etc/czh-money`                 | `/etc/czh-favorites`                 |
| `czh-money.service`              | `czh-favorites.service`              |
| start-service.sh symlink `start` | keep same pattern                    |

The `<dependency>` on `czh-favorites-app` must use `<classifier>exec</classifier>` (the Spring Boot
repackaged fat jar) — confirm `czh-favorites-app/pom.xml` uses `spring-boot-maven-plugin` with the
`exec` classifier.

#### 1.2 — Create `czh-favorites.service` (systemd unit)

**Path:** `czh-favorites-app-rpm/src/main/resources/service/czh-favorites.service`

```ini
[Unit]
Description=czh-favorites
After=network.target

[Service]
Type=simple
User=appuser01
Group=appgroup
WorkingDirectory=/opt/czh-favorites

Environment="JAVA_OPTS=-Xms384m -Xmx1024m -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true -Dlogging.config=/opt/czh-favorites/conf/logback-spring.xml -Dserver.address=0.0.0.0"

ExecStart=/opt/jdk-25/bin/java $JAVA_OPTS \
  -jar /opt/czh-favorites/lib/czh-favorites-app.jar \
  --spring.config.location=/opt/czh-favorites/conf/application.yml

Restart=on-failure
LimitNOFILE=102642

[Install]
WantedBy=multi-user.target
```

#### 1.3 — Create shell scripts

**`start-service.sh`:** substitute all `czh-money` → `czh-favorites`, `/var/run/czh-money/` → `/var/run/czh-favorites/`

**`stop-service.sh`, `status-service.sh`, `version.sh`:** same substitution pattern.

**`kill-service.sh`:** port from czh-money (sends SIGTERM to the Spring Boot PID).

All scripts go to `czh-favorites-app-rpm/src/main/resources/bin/`.

#### 1.4 — Copy `logback-spring.xml`

Copy `/java/czh/czh-money/money-app-rpm/src/main/resources/config/logback-spring.xml` to
`czh-favorites-app-rpm/src/main/resources/config/logback-spring.xml` and substitute `czh-money` → `czh-favorites` in the log path and appender names.

#### 1.5 — Add module to root POM

Add `<module>czh-favorites-app-rpm</module>` to `/java/czh/czh-favourites/pom.xml` after `czh-favorites-db`.

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app-rpm/pom.xml \
  -r czh-favorites-app-rpm/src/main/resources/service/czh-favorites.service \
  -r czh-favorites-app-rpm/src/main/resources/bin/ \
  -w "RPM only builds with -Drpm=true. Normal ./mvn.sh install skips it (maven.packaging.type=pom). Test with: ./mvn.sh install -Drpm=true -pl czh-favorites-app-rpm"
```

---

## Phase 2 — `czh-favorites-db-rpm` module

### Goal
`czh-favorites-db-rpm/pom.xml` compiles cleanly; the `rpm` profile packages the delta SQL scripts
into an RPM installable to `/opt/czh-favorites-db/`.

### Files to read before starting
- `/java/czh/czh-money/czh-money-db-rpm/pom.xml`

### Tasks

#### 2.1 — Create `czh-favorites-db-rpm/pom.xml`

Mirror `czh-money-db-rpm/pom.xml` with substitutions:

| czh-money value                  | czh-favourites value                 |
|----------------------------------|--------------------------------------|
| `czh-money`                      | `czh-favourites`                     |
| `czh-money-db`                   | `czh-favorites-db`                   |
| `czh-money-db-rpm`               | `czh-favorites-db-rpm`               |
| `/opt/czh-money-db`              | `/opt/czh-favorites-db`              |
| `../czh-money-db/src/main/delta` | `../czh-favorites-db/src/main/delta` |

The `<dependency>` on `czh-favorites-db` forces reactor ordering.

#### 2.2 — Create `run-delta.sh`

**Path:** `czh-favorites-db-rpm/src/main/bin/run-delta.sh`

Port from czh-money: substitute app name. This script is called by operators to apply delta SQL
files in sequence using db2delta.

#### 2.3 — Add module to root POM

Add `<module>czh-favorites-db-rpm</module>` to `/java/czh/czh-favourites/pom.xml`.

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-db-rpm/pom.xml \
  -r czh-favorites-db-rpm/src/main/bin/run-delta.sh \
  -w "RPM only builds with -Drpm=true. Test with: ./mvn.sh install -Drpm=true -pl czh-favorites-db-rpm"
```

---

## Phase 3 — GitHub Actions CI

### Goal
`.github/workflows/build.yml` triggers on push/PR to `develop`; builds with Java 25 + Maven;
runs tests; builds RPMs.

### Files to read before starting
- `/java/czh/czh-money/.github/workflows/maven.yml` (exact pattern)

### Tasks

#### 3.1 — Write `.github/workflows/build.yml`

Port `czh-money` `maven.yml` verbatim, changing only:
- `name:` → `czh-favorites CI`
- Branch targets: `develop`
- Maven command: `mvn -B install -Duser.timezone=Europe/Prague -Drpm=true -P github-ci`

The `github-ci` profile must be defined in the root POM (or inherited from the parent BOM). Check
if `github-ci` exists in `/java/czh/czh-favourites/pom.xml`; if not, add it with the same
datasource override (H2 for DB2) as czh-money uses.

```yaml
# .github/workflows/build.yml
name: czh-favorites CI
on:
  push:
    branches: [ "develop" ]
  pull_request:
    branches: [ "develop" ]
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Set timezone
        run: sudo timedatectl set-timezone Europe/Prague
      - uses: actions/checkout@v4
      - name: Install rpm
        run: sudo apt install -y rpm
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven
      - name: Build with Maven
        run: mvn -B install -Duser.timezone=Europe/Prague -Drpm=true -P github-ci
```

#### 3.2 — Verify `github-ci` profile exists in root POM

Open `/java/czh/czh-favourites/pom.xml`. If no `github-ci` profile exists, add one that:
- Sets `spring.datasource.url=jdbc:h2:mem:testdb` (or the HSQLDB equivalent already used in tests)
- Sets `spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver`
- Excludes the DB2 driver from the build classpath

(If it already exists from SP1, skip this step and note it in progress.)

```sh
~/bin/geany-progress done 3 \
  -r .github/workflows/build.yml \
  -r pom.xml \
  -w "GitHub Actions uses public Maven Central + Temurin JDK 25. No private Nexus needed in CI (github-ci profile)."
```

---

## Phase 4 — Azure DevOps CI

### Goal
Six Azure DevOps pipeline YAML files under `.azuredevops/non-production/maven/` mirroring
the czh-money pattern with all `czh-money` references replaced by `czh-favorites`.

### Reference files
- `/java/czh/czh-money/.azuredevops/non-production/maven/maven-build-develop-branch.yaml`
- `/java/czh/czh-money/.azuredevops/non-production/maven/maven-build-pull-request.yaml`
- `/java/czh/czh-money/.azuredevops/non-production/maven/maven-build-release-action-branch.yaml`
- `/java/czh/czh-money/.azuredevops/non-production/maven/maven-dependency-submission.yaml`
- `/java/czh/czh-money/.azuredevops/non-production/maven/update-dependencies-to-release-pr.yaml`
- `/java/czh/czh-money/.azuredevops/non-production/maven/update-target-branch-version-pr.yaml`

### Substitution table

| czh-money value            | czh-favorites value            |
|----------------------------|--------------------------------|
| `czh-money-settings.xml`   | `czh-favorites-settings.xml`   |
| `czh_money.version`        | `czh_favorites.version`        |
| `TEAMS_CZH_MONEY_RELEASE`  | `TEAMS_CZH_FAVORITES_RELEASE`  |
| `TEAMS_CZH_MONEY_SNAPSHOT` | `TEAMS_CZH_FAVORITES_SNAPSHOT` |

All other content (pool names, template references, Nexus variables, azure-github-templates
version `0.0.11`, Sonar URL) copies verbatim — these are organisation-wide settings.

### Tasks

#### 4.1 — Create directory structure

```
.azuredevops/non-production/maven/
```

#### 4.2 — Create all 6 pipeline files

Port each file applying the substitution table above.

```sh
~/bin/geany-progress done 4 \
  -r .azuredevops/non-production/maven/maven-build-develop-branch.yaml \
  -r .azuredevops/non-production/maven/maven-build-pull-request.yaml \
  -r .azuredevops/non-production/maven/maven-build-release-action-branch.yaml \
  -r .azuredevops/non-production/maven/maven-dependency-submission.yaml \
  -r .azuredevops/non-production/maven/update-dependencies-to-release-pr.yaml \
  -r .azuredevops/non-production/maven/update-target-branch-version-pr.yaml
```

---

## Phase 5 — Sign-off

### Tasks

#### 5.1 — Verify RPM build locally

```bash
./mvn.sh install -Drpm=true -pl czh-favorites-app-rpm
./mvn.sh install -Drpm=true -pl czh-favorites-db-rpm
```

Check that `czh-favorites-app-rpm/target/*.rpm` and `czh-favorites-db-rpm/target/*.rpm` exist.

#### 5.2 — Verify `./mvn.sh install` (without `-Drpm=true`) still passes all tests

The default `maven.packaging.type=pom` must skip RPM packaging cleanly.

#### 5.3 — Routing map

No new sanenamed Java components — skip routing map update for SP9.

```sh
~/bin/geany-progress done 5 \
  -w "norun active — run ./mvn.sh install -Drpm=true to validate RPM build"
```
