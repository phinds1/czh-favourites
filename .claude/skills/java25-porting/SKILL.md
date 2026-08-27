---
name: java25-porting
description: Generic skill for porting legacy Java projects to Java 25 + Spring Boot 4. Covers POM setup, dependency migration, Spring XML to annotations, REST framework migration, CI builds, JUnit 5, and Jamcrest test porting. Uses the opus-plan → opus-exec staged approach.
allowed-tools: bash, java
---

# Java 25 Porting Skill

This skill guides the migration of a legacy Java project (Java 6–17, Spring 2.x–5.x, XML config, JUnit 4, JAX-RS or other REST frameworks) to **Java 25 + Spring Boot 4** in a phased, context-window-safe approach.

It references and composes the following project skills:
- `opus-plan` — write a phased plan before touching code
- `opus-exec` — execute the plan phase by phase
- `jamcrest` — JSON test matching library patterns
- `jamcrest-testing` — porting REST tests to req.js / resp.js style
- `bug-fixing` — write tests for bugs found during migration
- `security` — post-migration security review

---

## 0. Before You Start — Project Setup

### 0a. Copy `.claude/settings.json`

Every porting project needs Claude tool permissions set before work begins.
Copy this baseline (which matches what was needed for the dgsubs migration) into the new project's `.claude/settings.json`:

```json
{
  "allowTools": [
    "Bash(cd)",
    "Bash(ls)",
    "Bash(cat)",
    "Bash(grep)",
    "Bash(head)",
    "Bash(tail)",
    "Bash(find)",
    "Bash(xargs)",
    "Bash(sed)",
    "Bash(awk)",
    "Bash(diff)",
    "Bash(cp)",
    "Bash(mv)",
    "Bash(mkdir)",
    "Bash(rm)",
    "Bash(git)",
    "Bash(mvn)",
    "Bash(mvn.sh)",
    "Bash(/opt/jdk-25/bin/java)",
    "Bash(/opt/maven/bin/mvn)",
    "Read",
    "Edit",
    "Write"
  ]
}
```

### 0c. Read references — never modify them

The `./references/` directory contains the original legacy codebase (or other read-only reference material).
**No file under `./references/` may ever be modified, renamed, or deleted.**
Agents must only read from references to understand the legacy code.

---

## 1. Discovery Phase (before planning)

Ask Claude Opus (via `opus-plan`) to do a discovery pass over the source before writing the plan.
Provide the output as input to the planner.

Discovery checklist:

### 1a. Package & module structure
```bash
find src/ -name "*.java" | head -50
grep -r "^package " src/main/java --include="*.java" | sed 's|:.*||' | sort -u
```
Note:
- Root package to rename → new package (e.g. `com.gtech.gis.site` → `cz.bsl.favorites`)
- Module layout (single module vs multi-module Maven)

### 1b. Legacy dependency audit
```bash
grep -E "(joda-time|codehaus.jackson|commons-dbcp|log4j:log4j|jboss-javaee|javax\.ws\.rs|jersey|resteasy|resttemplate)" \
  $(find . -name "pom.xml" ! -path "*/references/*") -i -l
```
Produce a table: Legacy dep → Replacement dep (see §5 below).

### 1c. Spring XML context files
```bash
find src/ -name "*context.xml" -o -name "*beans.xml" | grep -v references
```
List each file and its key beans (datasource, transaction, component-scan, REST clients, prototype commands).

### 1d. REST framework used
Check imports across Java source:
```bash
grep -r "javax.ws.rs\|jakarta.ws.rs\|org.springframework.web.client.RestTemplate\|feign\|retrofit\|okhttp" \
  src/ --include="*.java" -l
```
Identify: JAX-RS, RestTemplate, Feign, OkHttp, etc.

### 1e. REST endpoints called (outbound)
```bash
grep -r "exchange\|getForObject\|postForObject\|target\|path\|ClientBuilder" \
  src/ --include="*.java" -B2 -A2
```
Enumerate verbs, paths, request/response types.

### 1f. Test framework
```bash
grep -r "@RunWith\|@ExtendWith\|SpringJUnit4ClassRunner\|SpringRunner\|@SpringBootTest" \
  src/test --include="*.java" -l
```

### 1g. Count classes
```bash
find src/main/java -name "*.java" | wc -l
find src/test/java -name "*.java" | wc -l
```

---

## 2. Planning Phase — opus-plan

Invoke the `opus-plan` skill. The prompt should include:
1. Discovery output from §1
2. The migration goals (see §3)
3. The phase-sizing constraint: **each phase must fit in a Claude Sonnet 4.6 context window**
4. The cross-cutting rules (see §4)

The plan must cover:
- Phase 0: CI build setup (GitHub Actions + Azure DevOps)
- Phase 1: POM scaffolding (dependency cleanup, Java 25 compiler, Boot BOM)
- Phase 2: API module (replace external JAX-RS/dto jar with owned Spring Web module)
- Phase 3: Package rename (mechanical, no logic change, separate commit)
- Phase 4a–e: Legacy dependency replacements (JodaTime, Jackson, RestTemplate, DBCP, log4j) — one sub-step per dep
- Phase 5: Spring Boot bootstrap — replace XML contexts with `@Configuration` Java config
- Phase 6: Java 25 language modernisation (lambdas, records, text blocks, switch expressions, var)
- Phase 7: Test migration (JUnit 4 → JUnit 5, Jamcrest REST tests)
- Phase 8: RPM / distribution packaging
- Phase 9: Verification, CVE scan, security review

Plan is written to `./ai-output/<project-name>-java25-springboot4-migration-plan.md`.

---

## 3. Migration Goals

For every project these are the target outcomes:

| Concern             | Target                                                              |
|---------------------|---------------------------------------------------------------------|
| Java version        | Java 25 (`maven.compiler.release=25`)                               |
| Framework           | Spring Boot 4.x (Boot manages all dependency versions via BOM)      |
| REST client         | Spring `RestClient` or `@HttpExchange` proxy (not RestTemplate)     |
| REST server         | Spring Web `@RestController` (if server; not JAX-RS)                |
| JSON                | FasterXML Jackson (via Boot BOM)                                    |
| DB pool             | HikariCP (via `spring-boot-starter-jdbc`)                           |
| Date/time           | `java.time` (not JodaTime, not `java.util.Date` + Calendar)         |
| Logging             | Logback + SLF4J (via Boot; not log4j 1.x)                           |
| Spring config       | `@Configuration` + `@Bean` Java config (not Spring XML)             |
| Tests               | JUnit 5 Jupiter + `@SpringBootTest` (not JUnit 4)                   |
| REST tests          | Jamcrest + req.js / resp.js (see `jamcrest-testing` skill)          |
| Jakarta namespace   | `jakarta.*` (Boot 4 requires this; not `javax.*`)                   |
| CI                  | GitHub Actions (`.github/workflows/build.yml`) + Azure DevOps pipeline |

---

## 4. Cross-Cutting Rules (enforce in every phase)

1. **One behavioural change at a time.** Migrations are mechanical and behaviour-preserving. Keep SQL, endpoint paths, header names, exit codes, and error-mapping logic identical.
2. **Tests are the safety net.** Do not delete tests; port them. A phase is done only when the module compiles and its tests pass.
3. **No hardcoded secrets.** Carry existing credential values as config properties only. Flag them for the security review in the final phase.
4. **References are read-only.** `./references/` may never be modified. Any path under references must be opened with Read-only tools.
5. **Run targeted tests after each phase**: `./mvn.sh -q -pl <module> -am test` (or the equivalent with `-DskipTests=false`). Capture failures before proceeding.
6. **Context window management.** When context is 80% full, write state to `./ai-output/PROGRESS.md` and wait for human to clear context before continuing.

---

## 5. Standard Dependency Migration Table

Use this as the default mapping. Adjust based on the actual project dependencies.

| Legacy (remove)                             | Replacement (add)                                          | Notes                                             |
|---------------------------------------------|------------------------------------------------------------|---------------------------------------------------|
| `joda-time`                                 | `java.time` (JDK built-in)                                 | Pick a single `ZoneId` constant for the project   |
| `org.codehaus.jackson` (Jackson 1.x)        | `com.fasterxml.jackson.databind` (Boot BOM)                | API is near-identical; add `@JsonIgnoreProperties`|
| `commons-dbcp` `BasicDataSource`            | HikariCP via `spring-boot-starter-jdbc`                    |                                                   |
| `spring-jdbc` + `RestTemplate` (standalone) | `spring-boot-starter-jdbc` + Spring `RestClient`           |                                                   |
| `javax.ws.rs` / Jersey / RESTEasy           | Spring Web `@GetMapping` / `@PostMapping` / `@HttpExchange`|                                                   |
| `log4j:log4j` 1.x + `slf4j-log4j12`         | Logback (Boot default) + SLF4J                             | Replace `log4j.xml` → `logback.xml`               |
| `jboss-javaee` / `javax.annotation`         | `jakarta.annotation-api`                                   | Boot 4 is full Jakarta                            |
| `javax.*` imports everywhere                | `jakarta.*`                                                | Global search-replace                             |
| `mockito-all`                               | `mockito-core` (Boot BOM managed)                          | `mockito-all` is unmaintained                     |
| JUnit 4 `org.junit.*`                       | JUnit 5 `org.junit.jupiter.*`                              | See §8 below                                      |
| `SpringJUnit4ClassRunner`                   | `@ExtendWith(SpringExtension.class)` / `@SpringBootTest`   |                                                   |
| Spring 3.x XML XSD (`spring-beans-3.x.xsd`) | Delete (replaced by Java config)                           |                                                   |
| External gtech/vendor DTO jars              | New owned API module with Spring Web annotations           | See §6 below                                      |
| Liquibase if unused                         | Remove                                                     | Verify before removing                            |
| `mockito-all`                               | Remove or `mockito-core`                                   |                                                   |

---

## 6. Creating an Owned API Module (replaces vendor JAX-RS jars)

When the project consumes a vendor jar containing JAX-RS-annotated interfaces + DTOs:

1. Create a new Maven module `<project>-api` (packaging `jar`, no Spring Boot plugin).
2. Enumerate every type imported from the vendor jar: DTOs, enums, constants, helper classes.
3. Re-create each type in the new package using:
   - **Records** for immutable DTOs (preferred)
   - **POJOs** with `@JsonIgnoreProperties(ignoreUnknown=true)` if Jackson round-tripping mutability is needed
   - `@GetMapping` / `@PostMapping` / `@RequestParam` / `@RequestBody` / `@RequestHeader` on the typed client interface
   - Consider `@HttpExchange` annotations so the same interface backs a `HttpServiceProxyFactory` client in the batch module
4. Write unit tests for JSON (de)serialization round-trips.
5. Acceptance: `dgsubs-api` compiles; every symbol previously from the vendor jar has a `<project>.api` counterpart.

---

## 7. CI Build Setup

### GitHub Actions (`.github/workflows/build.yml`)
```yaml
name: Build

on:
  push:
    branches: [ "**" ]
  pull_request:
    branches: [ "**" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        # github-ci profile: excludes drivers/artifacts unavailable on Maven Central.
        # Tests run against H2/HSQLDB in-memory; on-prem drivers validated locally only.
        run: mvn -B -s .github/maven-settings.xml -P 'github-ci,!db2' verify --no-transfer-progress
```

### `.github/maven-settings.xml` (blocks private Nexus on GitHub CI)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>central-only</id>
      <name>Maven Central (CI mirror)</name>
      <url>https://repo1.maven.org/maven2</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

Add a `github-ci` Maven profile in the root `pom.xml` that excludes any artifact not on Maven Central (IBM DB2 driver, internal gtech jars, etc.) and swaps in an in-memory DB for tests.

### Azure DevOps (`azure-pipelines.yml`)
```yaml
trigger:
  branches:
    include:
      - '*'

pool:
  vmImage: 'ubuntu-latest'

variables:
  JAVA_HOME: '/opt/jdk-25'
  MAVEN_OPTS: '-Xmx1024m'

steps:
  - task: JavaToolInstaller@0
    inputs:
      versionSpec: '25'
      jdkArchitectureOption: 'x64'
      jdkSourceOption: 'PreInstalled'
    displayName: 'Use Java 25'

  - task: Cache@2
    inputs:
      key: 'maven | "$(Agent.OS)" | **/pom.xml'
      restoreKeys: |
        maven | "$(Agent.OS)"
        maven
      path: $(HOME)/.m2/repository
    displayName: 'Cache Maven packages'

  - script: mvn -B verify --no-transfer-progress -P '!db2'
    displayName: 'Build and test'
    env:
      MAVEN_OPTS: $(MAVEN_OPTS)
```

> **Note**: Azure DevOps can reach the internal Nexus, so no mirror redirect is needed (unlike GitHub Actions).
> Adjust the pool `vmImage` or `name` to match the on-prem agent pool if self-hosted agents are used.

---

## 8. POM Setup Checklist

Root `pom.xml` must have:

```xml
<properties>
  <java.version>25</java.version>
  <maven.compiler.release>25</maven.compiler.release>
  <!-- explicit versions only for deps NOT managed by Boot BOM -->
  <commons-cli.version>1.9.0</commons-cli.version>
  <commons-lang3.version>3.17.0</commons-lang3.version>
</properties>

<dependencyManagement>
  <dependencies>
    <!-- Boot BOM manages Jackson, Logback, HikariCP, JUnit 5, Mockito, Spring -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Key rules:
- **Never specify versions for Boot-managed deps** — let the BOM own them to avoid drift.
- **Remove** any `<version>` tags on `spring-*`, `jackson-*`, `logback-*`, `junit-*`, `mockito-*`, `hibernate-*`, `hikari*` when they are already in the BOM.
- **DB2 driver** (`com.ibm.db2:db2jcc4`) is not on Maven Central — confirm the internal Nexus repo entry is present and add a `<repository>` block to the POM. Exclude it from the `github-ci` profile.
- **Compiler plugin**: ensure `maven-compiler-plugin` uses `<release>25</release>` (not `<source>/<target>`).

---

## 9. JUnit 4 → JUnit 5 Migration Cheatsheet

| JUnit 4                               | JUnit 5 (Jupiter)                                         |
|---------------------------------------|-----------------------------------------------------------|
| `@RunWith(SpringJUnit4ClassRunner)`   | `@ExtendWith(SpringExtension.class)` or `@SpringBootTest` |
| `@ContextConfiguration(locations=…)`  | `@SpringBootTest` / `@ContextConfiguration(classes=…)`    |
| `@Before`                             | `@BeforeEach`                                             |
| `@After`                              | `@AfterEach`                               |
| `@BeforeClass`                        | `@BeforeAll`                               |
| `@AfterClass`                         | `@AfterAll`                                |
| `@Ignore`                             | `@Disabled`                                |
| `org.junit.Assert.*`                  | `org.junit.jupiter.api.Assertions.*`       |
| `@Test(expected = Foo.class)`         | `assertThrows(Foo.class, () -> …)`         |
| `@Test(timeout = 1000)`               | `@Timeout(1)` (seconds) or `assertTimeout`  |

Embedded DB: replace `@ContextConfiguration(locations="classpath:embedded-datasource-test-context.xml")` with `@DynamicPropertySource` or Boot `spring.datasource.*=` test properties pointing at HSQLDB/H2.

---

## 10. Spring XML → Java Config Cheatsheet

| XML element                                 | Java config equivalent                                        |
|---------------------------------------------|---------------------------------------------------------------|
| `<context:component-scan base-package="…">` | `@SpringBootApplication` on main class (auto-detects)         |
| `<bean id="ds" class="BasicDataSource">`    | `@Bean HikariDataSource dataSource(@Value…)`                  |
| `<bean id="jdbcTemplate"…>`                 | `@Bean JdbcTemplate jdbcTemplate(DataSource ds)`              |
| `<tx:annotation-driven/>`                   | `@EnableTransactionManagement` (Boot does this automatically) |
| `<bean id="txManager"…>`                    | `@Bean DataSourceTransactionManager txManager(DataSource)`    |
| `<bean scope="prototype"…>`                 | `@Scope("prototype") @Bean` or `ObjectProvider<T>`            |
| `PropertyPlaceholderConfigurer`             | `application.properties` / `@Value("${key}")` / `@ConfigurationProperties` |
| `<import resource="other-context.xml"/>`    | Delete — use `@Import(OtherConfig.class)` or scan             |
| Custom `PropertyEditor` (e.g. MonetaryAmount) | Jackson `@JsonDeserialize` + `@JsonSerialize` or Spring `Converter<String, T>` |

Boot entry point:
```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        // For CLI batch (no embedded server):
        ConfigurableApplicationContext ctx =
            new SpringApplicationBuilder(MyApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        // dispatch to CLI command beans here, then ctx.close()
    }
}
```

---

## 11. REST Client Migration

Replace `RestTemplate` (or JAX-RS `Client`) with Spring `RestClient` (Boot 4 preferred):

```java
// Old RestTemplate style
RestTemplate rt = new RestTemplate();
ResponseEntity<MyDto> resp = rt.exchange(url, HttpMethod.GET, entity, MyDto.class);

// New RestClient style
RestClient client = RestClient.builder()
    .baseUrl("${service.url}")
    .defaultHeader("x-api-key", "${service.api-key}")
    .build();

MyDto dto = client.get()
    .uri("/resource/{id}", id)
    .retrieve()
    .body(MyDto.class);
```

For typed, annotation-driven clients use `@HttpExchange`:
```java
@HttpExchange("/api/v1")
public interface MyServiceApi {
    @GetExchange("/items/{id}")
    MyDto getItem(@PathVariable String id);
}
// Wire in @Configuration:
HttpServiceProxyFactory factory = HttpServiceProxyFactory
    .builderFor(RestClientAdapter.create(restClient)).build();
MyServiceApi api = factory.createClient(MyServiceApi.class);
```

Error handling: `RestClient.retrieve()` throws `HttpClientErrorException` by default; catch it or use `.onStatus(…)` to map to domain exceptions — preserve existing error body parsing (e.g. `ErrorDTO`).

---

## 12. REST Test Porting (Jamcrest)

See the `jamcrest` and `jamcrest-testing` skills for full details.

Key steps:
1. Port `JamcrestUtils.java` to the new project (copy + adjust package).
2. Create `AbstractRestTest` base class that starts Spring Boot test server on a random port and provides helper methods.
3. For each legacy REST test (e.g. `TestRestEndpoint.java`):
   - Create `*.req.js` in `src/test/resources/rest/` with the JSON request body.
   - Write a `@Test` method using `AbstractRestTest` to call the endpoint.
   - Run once without `*.resp.js` to capture actual response in `target/ai.log`.
   - Verify expected output; create `*.resp.js` from `ai.log`; re-run to confirm green.
4. Replace hard-coded JSON strings in old tests with `jamcrest.loadJson()` / `jamcrest.validateJson()`.

---

## 13. Execution Order

Execute phases in this order; do not start a phase until the previous phase's acceptance criteria pass.

```
0 (setup) → 1 (poms) → 2 (api module) → 3 (package rename)
→ 4a (JodaTime) → 4b (Jackson) → 4c (RestClient) → 4d (HikariCP note)
→ 4e (logging) → 5 (Boot bootstrap / XML removal)
→ 6 (Java 25 language) → 7 (JUnit 5 + Jamcrest tests)
→ 8 (packaging / RPM) → 9 (verification + security)
```

Each of phases 4 and 6 should be executed as sub-steps, one dependency at a time.
Phases 5 and 7 are the most complex — give them their own context window.

Use the `opus-exec` skill to execute each phase. When context is 80% full:
1. Write state to `./ai-output/PROGRESS.md`
2. Report to human which phase was completed and which is next
3. Wait for context to be cleared before continuing

---

## 14. JaCoCo Coverage Setup

Add JaCoCo to the root `pom.xml` so coverage is collected on every `mvn verify` run
and enforced as a build gate. The version must be declared explicitly (Boot BOM does
not manage jacoco-maven-plugin).

### Root `pom.xml` — property
```xml
<properties>
  <jacoco-maven-plugin.version>0.8.14</jacoco-maven-plugin.version>
</properties>
```

### Root `pom.xml` — pluginManagement (version pin only)
```xml
<pluginManagement>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>${jacoco-maven-plugin.version}</version>
    </plugin>
  </plugins>
</pluginManagement>
```

### Root `pom.xml` — plugins (executions inherited by all modules)
```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <executions>

        <!-- 1. Bind the agent before tests run so coverage is recorded -->
        <execution>
          <id>prepare-agent</id>
          <goals><goal>prepare-agent</goal></goals>
        </execution>

        <!-- 2. Generate HTML/XML reports after tests (site or verify) -->
        <execution>
          <id>report</id>
          <phase>verify</phase>
          <goals><goal>report</goal></goals>
        </execution>

        <!-- 3. Enforce minimum coverage thresholds — breaks the build if not met -->
        <execution>
          <id>check</id>
          <phase>verify</phase>
          <goals><goal>check</goal></goals>
          <configuration>
            <rules>
              <rule>
                <element>BUNDLE</element>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.60</minimum>  <!-- raise gradually as tests are ported -->
                  </limit>
                  <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.50</minimum>
                  </limit>
                </limits>
              </rule>
            </rules>
          </configuration>
        </execution>

      </executions>
    </plugin>
  </plugins>
</build>
```

### Skipping check during early migration phases
While porting (phases 1–6) the threshold check will likely fail — skip it selectively:
```bash
# Skip only the check goal, still collect coverage data
./mvn.sh verify -Djacoco.skip.check=true

# Or skip JaCoCo entirely (no data, no report, no check)
./mvn.sh verify -Djacoco.skip=true
```

Enable enforced thresholds in Phase 7 once the JUnit 5 test suite is green.

### Modules to exclude from coverage
Pure packaging modules (RPM, assembly zip) produce no classes and should skip JaCoCo.
Add to their `pom.xml`:
```xml
<properties>
  <jacoco.skip>true</jacoco.skip>
</properties>
```

### Reading the report
After `./mvn.sh verify` the HTML report is at:
```
<module>/target/site/jacoco/index.html
```
For a multi-module aggregate report, add a dedicated reporting module or use:
```bash
./mvn.sh jacoco:report-aggregate -pl <reporting-module>
```

### GitHub Actions — upload coverage report as artifact
Add to `.github/workflows/build.yml` after the build step:
```yaml
- name: Upload JaCoCo coverage report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: jacoco-report
    path: '**/target/site/jacoco/'
    retention-days: 7
```

---

## 15. Final Verification Checklist

Run these before declaring the migration complete:

```bash
# No legacy imports remain
grep -rn "org\.joda\|org\.codehaus\.jackson\|commons\.dbcp\|javax\.ws\.rs\|log4j\.Logger\|org\.junit\.Test$" \
  src/ --include="*.java"

# No old package remains
grep -rn "com\.igt\.pd\|com\.gtech\." src/ --include="*.java"

# No Spring XML contexts remain in main resources
find src/main/resources -name "*context.xml" -o -name "*beans.xml"

# No JUnit 4 runner annotations remain
grep -rn "SpringJUnit4ClassRunner\|@RunWith" src/test --include="*.java"

# Full build green
./mvn.sh clean verify

# RPM build (if applicable)
./mvn.sh clean package -Dbuild.rpms
```

Security review (after passing build): invoke the `security` skill over the new source package.
