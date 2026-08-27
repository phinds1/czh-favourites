---
name: opus-masterplan
description: create a set of numbered plans for the migration of a complete microservice
allowed-tools: java, bash
---

Rather than implementing any code, output should be a markdown file `.planning/migration-plan-index.md`
that contains named phased implementation plans (SP1, SP2, ...) where each phase contains a delieverable state of the whole appliction.

Primary input should be a `.planning/java25-spring-boot-migration.md` document with the high level goals of the migration.

This skill should be used for porting applications and requires `./reference` projects to define how the application should be built.
The references should include the complete code we are migrating,  typically this will be a monolithic app that we are implementing only a specific sub set of features.
input should include the main project sanename, and java package name for code if appropriate
Each componene to be build should have a `sanename` so agents and humans can route throught the code with reliable greps.

Plas should include

- SP1 SCAFFOLD_PLAN project setup
- Running mocks required for integration testing
- Any project specific tooling, e.,g. test tools
- a Dao layer plan, if the microservice keeps state.
- an API plan to create JSON APIs that are exposed, usually porting from exising Java classes that use the wrong JSON serializer
- Specific instrrutionns for how to use GTMS time (hosts definition of the day and time-of-day
- Unit test requirements and Jacoco coverage tooling
- Jamcrest testing framework or similar for local testing of the application
- Remote test suit for testing agains a running, remote, test system.
- Deployment plan, for creating RPMS isntalling DB schema items,Azure and githup yaml files
- Application Documentation requirements
- prometheus metrics to expose, required at least health, success and failure metrics
- A vision GUI, if applicable
- Integration test suite in bash against a real running remote server

Input for each and every phase should have documentation, exmaples and real working code references.
Nothing should be hallucinated so the agent should stop and ask if reference information is missing.

No changes should be made, other than writing one new plan-index document.

## Planning Agent Guidance

This section tells the planning agent *how* to produce the plan index. It is derived from the
worked `czh-money` migration — read `/java/czh/czh-money/.planning/migration-plan-index.md` and its
`plans/SP1`–`SP11` as the reference shape. Reuse that structure; do not invent a new one.

### Output structure of `migration-plan-index.md`

In order:

1. **Sub-Plan Summary Table** — `| ID | Title | Key output | Depends on |`, one row per SP. This is
   the map a human scans to see the whole migration at a glance.
2. **One section per sub-plan** (`## SPn: Title`), each containing:
   - **Scope** — what gets built, one paragraph.
   - **Why this is a unit** — the single reason this is one sub-plan and not two (or merged). This
     is what stops plan-splitting drift.
   - **Key reference files** — a `| File | Purpose |` table pointing at *real* files in
     `references/`, the HLD, or the migration input. Nothing hallucinated.
   - **Key classes / files to create** — an ASCII tree of the new files (package paths, file names,
     one-line role). This is the contract the executor builds against.
   - **Dependencies on other sub-plans** — explicit `SPx` references.
   - **Suggested detailed-plan prompt** — a ready-to-paste block quote for `/opus-plan` to expand
     into the detailed `SPX_*_PLAN.md`.
3. **Progress Tracking** — file-location conventions (`.planning/plans/SPX_<SLUG>_PLAN.md`,
   `.planning/state/SPX_<SLUG>_PROGRESS.md`), the `geany-progress` init/done commands, and the
   resume-in-new-session procedure (read progress state -> read index -> continue with
   `/opus-exec`).

### Sub-plan ordering

Order by dependency, and make every sub-plan end in a state where `./mvn.sh install` compiles
cleanly. The czh-money ordering that worked:

- **SP1 Scaffold** — root/child POMs, `@SpringBootApplication` entry, config, metrics, the `-db`
  delta module. No business logic. Foundation; everything depends on it. **Wire JaCoCo here:** the
  root POM already declares the `jacoco-maven-plugin` (property + `pluginManagement` + a
  `prepare-agent` execution) so every module drops a `target/jacoco.exec` during `./mvn.sh install`;
  confirm that execution is present and active, and ship `bin/coverage.sh` (see the reusable
  references table) so coverage can be generated from the start. Coverage tooling is a scaffold
  deliverable, not a late addition — it must exist before SP2 so every SP's tests are measured.
- **Mocks next** (e.g. SP2 PAM mock) — build the runnable network mock *before* the client code so
  client code can be tested locally against it. The mock must be idempotent like the real system.
- **Client / integration layers** (XML/WS clients, DAO, JSON API models) — the layers the features
  compose. DAO and the pure-model API jar can proceed in parallel once SP1 is done.
- **Core feature** (service + REST resource + Jamcrest tests) — the central feature combines
  service + DAO + client; design it as one unit because those three interact.
- **Smaller feature controllers** (auth, subscription/notify) — can run in parallel once the layers
  + test infra exist.
- **Test infrastructure** (Jamcrest `AbstractRestTest`, mock DB, mock auth) — define the shared
  base before the feature controllers that consume it.
- **Admin / operational APIs** — Quartz jobs become REST endpoints callable by external cron; these
  depend on the full service layer.
- **Deployment** (last, or orthogonal) — the project-specific instance of the deployment guide: app
  RPM + DB RPM (db2delta, not Liquibase) + GitHub Actions + Azure DevOps + systemd. Can proceed in
  parallel with late feature work once the scaffold + the spring-boot jar exist.

### Reusable references (point every detailed plan at these)

These are the same for any Java 25 migration off this template. The plan index should cite the
ones relevant to each sub-plan; detailed plans must read them before specifying anything.

| Reference                       | Where                                                   | Use it for                                                                                                                                                                                       |
|---------------------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Migration goals (primary input) | `.planning/java25-spring-boot-migration.md`             | High-level goals, sanename, package name, DAO/API/auth/test requirements. This is the spec the index decomposes.                                                                                 |
| High-level design               | `.requirements/design/hld.md`                           | Code choices (Java 25 / SB4, zero deps outside SB4), state design (JSON document store, 32-bit flag bitmask, JdbcTemplate), monitoring (Prometheus), deployment (RPM to `/opt`).                 |
| Deployment recipe               | `.requirements/JAVA25_MICROSERVICE_DEPLOYMENT_GUIDE.md` | Generic RPM + CI (GitHub Actions + Azure DevOps) + db2delta + systemd guide. The deployment SP is the project-specific instance of this — read it first, fill the `<app>` placeholders.          |
| Legacy source                   | `references/gis/` (symlink)                             | The legacy code being ported. Every "port X" sub-plan points at the exact legacy file(s) here.                                                                                                   |
| Recently-ported style reference | a `references/<port>` project, if present               | A Java 15/25 SB project already ported in this org — copy its POM structure, `application.yml` style, `JdbcTemplate` DAO idiom, `AbstractRestTest` pattern. The migration input names which one. |
| Jamcrest library                | `references/jamcrest` (symlink) + `/jamcrest` skill     | JSON matching used by `.req.js`/`.resp.js` tests.                                                                                                                                                |
| Jamcrest test framework         | `/jamcrest-testing` skill                               | `AbstractRestTest` contract: boot SB on random port, mock DB, mock auth headers, print `suggestJsonDefinition()` to `target/ai.log` when a `.resp.js` is missing.                                |
| Build wrapper                   | `./mvn.sh`                                              | Sets `JAVA_HOME=/opt/jdk-25`, `MAVEN_HOME`, a separate `.m2-25`. Use it for every build, local and (where `/opt/jdk-25` exists) CI.                                                              |
| JDK                             | `/opt/jdk-25/`                                          | Compile/run target. systemd `ExecStart` and start scripts must use `/opt/jdk-25/bin/java`, not `java` from PATH.                                                                                 |
| SB4 dependency versions         | `~/bin/sb4ver`                                          | Find precise Spring Boot 4 transitive versions. Zero deps outside SB4 (per HLD).                                                                                                                 |
| DB delta tool                   | `/usr/bin/db2delta` + `man db2delta`                    | Installed delta tool. Delta SQL files live in `<app>-db/src/main/delta/NNN-name.sql` in db2delta format. Verify exact CLI flags against the man page — do not guess.                             |
| Progress tracking               | `~/bin/geany-progress`                                  | `init` registers a plan in the Geany sidebar; `done N -r ... -w ...` records phase completion to `.planning/state/`. Skip silently if the socket is absent.                                      |
| Coverage                        | `bin/coverage.sh`                                       | Builds, merges every module's `jacoco.exec`, emits `target/jacoco-report.csv` for the Geany coverage GUI. The root POM's `jacoco-maven-plugin` `prepare-agent` execution must be active so each module produces `jacoco.exec`. Derived from `/java/czh/czh-money/bin/coverage.sh`; module-agnostic (auto-discovers `target/classes` + `src/main/java`). |
| Detailed-plan / executor skills | `/opus-plan`, `/opus-exec`                              | `/opus-plan` expands a sub-plan section into `plans/SPX_<SLUG>_PLAN.md`; `/opus-exec` runs it phase by phase, writing progress state.                                                            |
| Generic porting skill           | `/java25-porting`                                       | Composes the above into the overall porting workflow.                                                                                                                                            |
| Naming skill                    | `/sanename`                                             | The reliable-grep naming convention every component must follow.                                                                                                                                 |
| Worked example                  | `/java/czh/czh-money/.planning/`                        | Complete `migration-plan-index.md` + `plans/SP1`–`SP11` this guidance is derived from. Read it when in doubt about structure or depth.                                                           |

### Guardrails

- **No hallucination.** Every reference file cited in a sub-plan must be a real path the planner
  has read. If a reference is missing (no style-reference project, unclear legacy file, unknown
  `db2delta` flags), stop and ask the human — do not fill gaps with guesses.
- **No git.** (Project rule.) The plan only writes `.planning/` documents; it never runs git.
- **Test-first, always.** Each phase of each detailed plan must include its unit test where
  applicable; bug fixes start with a failing test. Specify the fastest local test (JUnit on the
  module); integration tests validate afterwards.
- **One plan document only.** This skill writes `.planning/migration-plan-index.md` and nothing
  else. Detailed `SPX_*_PLAN.md` files are produced later by `/opus-plan`, not here.
- **Each sub-plan fits a context window.** Detailed plans are expanded for a Sonnet context window,
  so sub-plans must be sized so their detailed expansion + implementation fits. When in doubt,
  split.
- **Every sub-plan ends compilable.** After the executor finishes an SP, `./mvn.sh install` must
  still pass — each SP is a deliverable state of the whole application, not a partial slice.

## Post-execution phase (run after every SP completes)

Once `/opus-exec` finishes a sub-plan, the migration is not done. Each SP is followed by a
**test → coverage → security → refactor → routing-map** sequence before the SP is signed off. The
master plan index should state that these run after each SP, and the detailed `SPX_*_PLAN.md`
(written by `/opus-plan`) should carry them as its final phases. Order matters; do not reorder.

### 1. Test — ensure everything written passes

- Run the fastest closest test first: the local JUnit suite on the module(s) touched by this SP
  (`./mvn.sh -pl <module> test`). Then run the full build (`./mvn.sh install`).
- Every test written during the SP must pass. Any failing or skipped test blocks sign-off — fix or
  un-skip it; do not leave red or `@Disabled` tests behind.
- Run the Jamcrest integration tests that hit the running service (the `/jamcrest-testing` flow).
  Confirm that no `.resp.js` is missing — if `target/ai.log` contains a `suggestJsonDefinition()`,
  the fixture still needs to be written; do that now and re-run until green.
- Validate with the integration tests, not just unit tests: unit tests verify code correctness,
  integration tests verify the feature actually works end to end.
- If a test was written against a mock (PAM mock etc.), confirm the mock is up and the test is
  guarded correctly (e.g. `@EnabledIfSystemProperty` / `@RequiresPamMock`) so it skips cleanly when
  the mock is absent rather than failing.

### 2. Coverage — run `bin/coverage.sh` and analyse gaps (target 80%)

- Run `bin/coverage.sh`. It builds (`./mvn.sh clean install`), merges every module's `jacoco.exec`
  into `target/jacoco-aggregate.exec`, and emits `target/jacoco-report.csv` — the CSV the Geany
  coverage GUI consumes. The root POM's `jacoco-maven-plugin` `prepare-agent` execution must be
  active (SP1 wired it) or the `.exec` files will be absent and the report empty; if they are
  absent, fix the POM before continuing rather than reporting empty coverage.
- Read `target/jacoco-report.csv`. Each row is a class with `INSTRUCTION_MISSED` /
  `INSTRUCTION_COVERED` (and the same for branch/line). Compute coverage per class and overall:
  `covered / (covered + missed)` for instructions. The **target is 80% instruction coverage.**
- Identify uncovered code. For any class or method below 80%, and especially any class with **zero**
  coverage, list the gap and the reason:
  - Code that *should* be tested (business logic, DAOs, services, controllers, validators, mappers)
    → a missing test. Write the test now (test-first), then re-run `bin/coverage.sh` to confirm the
    gap closed. Treat a zero-coverage public class as a blocker.
  - Code that is genuinely unreachable in unit/integration tests (e.g. Spring Boot wiring only
    exercised at runtime, generated code, `main()` entry points) → record the exception in the
    progress notes with the reason; do not write a pointless test just to lift the number. JaCoCo
    can exclude such packages via `<excludes>` on the `prepare-agent`/`report` config if the team
    agrees — but prefer real coverage over exclusions, and call out any exclusion in the sign-off
    notes so it is visible.
  - Methods present but never called by any feature (dead code from the port) → flag for the
    refactor step (step 4) to remove, rather than testing.
- Re-run `bin/coverage.sh` after closing gaps. The coverage figure at sign-off must meet the 80%
  target, or the progress notes must record the accepted exceptions and the residual percentage.
- This step runs *before* security and refactor so that coverage measures the code as written;
  security fixes and refactors then re-run `bin/coverage.sh` to confirm they did not regress
  coverage.

### 3. Security — run `security-java` (recommendations only)

- Run the `/security-java` skill on the Java added/changed by this SP. It reports NPE risks,
  unvalidated-input issues, and DDoS/resource-exhaustion risks.
- **Its output is recommendations, not fixes.** Do not let the security pass edit code.
- Triage the report. For each recommendation the human accepts, apply it **test-first**: write a
  test that reproduces the issue (a failing test for an NPE; a test sending the bad input for a
  validation gap; a test that demonstrates the unbounded behaviour for a DoS risk), apply the fix,
  and confirm the test now passes.
- Re-run step 1 after every accepted fix so the test suite stays green.

### 4. Refactor — pull up common code, improve style, verify comments

Run only when step 1 (tests) is green and step 3's accepted security fixes are applied and green.

- **Pull up common code.** Look for duplicated logic across the classes added in this SP and across
  earlier SPs — shared controller helpers, repeated XML request/response building, repeated
  `RowMapper` patterns, repeated auth-header handling. Move the common part into a shared
  superclass or utility (e.g. an `AbstractResource` base, a `AbstractRestTest` mixin) where it
  reduces duplication without over-abstracting. Do not introduce abstractions for code that is not
  yet duplicated; three similar lines is better than a premature abstraction.
- **Style and improvement checks.** Review for dead code, unused imports, unreachable branches,
  methods that have grown too large to reason about, and naming that breaks the `/sanename`
  convention. Apply the template's code choices from `.requirements/design/hld.md` (zero deps
  outside SB4, readable validatable SQL, plain-Java validation rather than Hibernate Validator).
- **Verify comments still apply.** Re-read every comment in the touched code and confirm the code
  has not deviated from the comment. Remove or rewrite comments that no longer match the code
  (per the CLAUDE.md rule: comments should explain *why*, not *what*). A comment that describes
  behaviour the code no longer has is a defect — fix it.
- After refactoring, re-run step 1. Refactors must not change behaviour; the test suite is the
  proof. If a refactor changes a test's expected output, the refactor changed behaviour — stop and
  reconsider.

### 5. Routing map — keep `.requirements/design/routing.md` current

- Maintain `.requirements/design/routing.md` (see the routing-map skill section below) so future
  agents can find code by feature via the sanenames. After this SP adds or renames any sanenamed
  component, update the routing map's feature→sanename→file table and the brief code map.
- The routing map is how a future agent answers "where do I change X?" without grepping the whole
  repo. Keep it accurate at every SP sign-off; a stale routing map is worse than none.

### Sign-off

An SP is signed off only when: the full test suite is green (step 1), coverage meets the 80%
target or its exceptions are recorded (step 2), every accepted `security-java` recommendation is
fixed test-first and green (step 3), the refactor pass is done and green (step 4), and `routing.md`
reflects the current code (step 5). Record sign-off in
`.planning/state/SPX_<SLUG>_PROGRESS.md` before moving to the next SP.

## Routing-map skill

`/routing-map` maintains `.requirements/design/routing.md` — the per-feature index of sanenames and
a brief code map. It reads the current code (do not trust memory or the last-known shape) and
rewrites `routing.md` so it is accurate as of the run. See the routing-map skill for the exact
output format; the shape is:

- A **feature → sanename** table: one row per sanenamed component, with the feature it belongs to,
  the sanename, the module, and the file path. Grouped by feature.
- A **brief code map**: the module/package layout (which package holds controllers, services, DAO,
  clients, domain, config; which module holds the JSON API models, the DB deltas, the RPMs) with
  one line per package describing its role.
- A **sanename → file** quick-lookup so a `grep` for a sanename lands on the right file.

Derive the shape from the worked `czh-money` routing map if one exists; otherwise build it from the
current tree. The routing map must list only code that exists at run time — verify each path before
listing it.

