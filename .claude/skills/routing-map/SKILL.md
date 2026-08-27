---
name: routing-map
description: maintain .requirements/design/routing.md — the per-feature sanename index and brief code map for the Java 25 microservice
allowed-tools: bash
---

Maintain `.requirements/design/routing.md` so a future agent (or human) can find the code to change
for a given feature without grepping the whole repository. This is the routing-map step of the
post-execution phase in `opus-masterplan`: run it after each SP that adds or renames a sanenamed
component, and at least once before an SP is signed off.

This skill **reads the current code and rewrites `routing.md`** so it is accurate as of the run.
Do not trust memory, the last-known shape, or a previous version of the file — the code is the
source of truth. Verify every path you list actually exists before listing it.

## How to run

1. Read `.requirements/design/hld.md` and `.planning/java25-spring-boot-migration.md` for the
   project sanename and the feature list. Read `.planning/migration-plan-index.md` for the
   feature→SP mapping.
2. Read the current tree: list the Maven modules, the Java packages in each, and the sanenamed
   classes (controllers/resources, services, DAOs, clients, domain models, config). Use `find` /
   `grep` against the real files — do not infer from the migration input alone, because the input
   describes intent, not what was actually built.
3. Rewrite `.requirements/design/routing.md` with the three sections below.
4. Do not change any code. The only file this skill writes is `routing.md`.

## Output shape of `routing.md`

### Section 1 — Feature → sanename table

One row per sanenamed component, **grouped by feature**. Columns:

| Feature | Sanename | Module | Package | File |
|---------|----------|--------|---------|------|

"Feature" is the user-facing capability (e.g. *wallet payments*, *player auth*, *subscription*),
taken from the migration input / plan index. "Sanename" is the stable grep handle. Every row must
be a file that exists — verify the path.

### Section 2 — Brief code map

The module/package layout, one line per package describing its role. Enough that an agent who has
never seen the repo can answer "which package holds the X?". For example, for a typical CZH
microservice:

```
<app>-api/        cz.bsl.<app>.api.<feature>   JSON request/response POJOs, plain-Java validation
<app>-app/        cz.bsl.<app>.controller      @RestController endpoints (HTTP entry points)
                  cz.bsl.<app>.service         business logic, transaction boundaries
                  cz.bsl.<app>.dao             JdbcTemplate DAOs (persistence boundary)
                  cz.bsl.<app>.domain          domain models (JSON-blob carriers, enums)
                  cz.bsl.<app>.client          external WS / HTTP clients (XML string builders)
                  cz.bsl.<app>.config          Spring @Configuration, DataSourceConfig, MetricsConfig
<app>-db/         src/main/delta/              db2delta SQL migration scripts (no Java)
<app>-rpm/        pom.xml                      application RPM (spring-boot jar + systemd + scripts)
<app>-db-rpm/     pom.xml                      DB RPM (delta SQL + run-delta.sh)
```

Adapt to the actual modules/packages present. If a module or package does not exist, do not list it.

### Section 3 — Sanename → file quick-lookup

A flat `sanename → path` list so a `grep` for a sanename lands on the right file. This is the
fast-path an agent uses when a plan says "change the wallet debit path".

```
wallet           → <app>-app/.../controller/WalletResource.java
wallet-debit     → <app>-app/.../service/<WalletService>.java   (debit method)
...
```

## Rules

- **List only code that exists.** Verify each path. A routing map that points at files that were
  renamed or never created is worse than none.
- **Sanenames are the handles.** Every component in the map must carry the sanename a plan or test
  would grep for. If a class has no stable sanename, flag it — do not invent one here (naming is the
  `/sanename` skill's job); list it under its current name and note the gap.
- **Brief, not exhaustive.** The code map is a one-line-per-package overview, not a file listing.
  The feature table lists sanenamed components, not every helper.
- **One file written.** This skill writes `.requirements/design/routing.md` and nothing else.
