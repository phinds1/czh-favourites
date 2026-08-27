# SP8: Prometheus Metrics — Detailed Implementation Plan

**Sub-plan:** SP8 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.czh.favourites.metrics`  
**Progress state:** `.planning/state/SP8_PROMETHEUS_METRICS_PROGRESS.md`

## Overview

Wire Prometheus counters into `DefaultFavouritesService` via a `FavouritesMetrics` bean. The
pattern mirrors `BusinessMetrics` from `czh-money` exactly: a single Micrometer
`MeterRegistry`-backed `@Component` with `countOk(op)` / `countFailure(op)` methods.

`DefaultFavouritesService` wraps each operation with try/finally: increment `requests` on entry,
call `countOk(op)` on return, call `countFailure(op)` in the catch block before re-throwing.

The `/actuator/prometheus` endpoint is already configured in `application.yml`
(`management.endpoints.web.exposure.include: health,info,prometheus`). SP8 adds no YAML changes.

**Counter shape** (all as Micrometer: tag-based, Prometheus-rendered as underscore-separated):

| Micrometer name             | Tags          | Rendered in Prometheus                      |
|-----------------------------|---------------|---------------------------------------------|
| `favourites.wager.operation`| `op`, `outcome` | `favourites_wager_operation_total{op=...,outcome=ok\|failure}` |
| `favourites.group.operation`| `op`, `outcome` | `favourites_group_operation_total{op=...,outcome=ok\|failure}` |

**Op values for wager:** `create`, `get`, `update`, `delete`, `list`  
**Op values for group:** `create`, `get`, `update`, `delete`, `list`

---

## Phase 1 — Write `FavouritesMetrics` bean + unit tests

### Goal
`FavouritesMetrics.java` compiles; unit tests verify counters increment.

### Files to read before starting
- `/java/czh/czh-money/money-app/src/main/java/cz/bsl/money/metrics/BusinessMetrics.java` (pattern to mirror)
- `czh-favorites-app/src/main/resources/application.yml` (confirm prometheus endpoint already included)

### Tasks

#### 1.1 — Write `FavouritesMetrics.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/metrics/FavouritesMetrics.java`

```java
package cz.bsl.czh.favourites.metrics;

// Grep anchor: favourites

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Prometheus counters for favourite wager and group operations.
 *
 * <p>Two counter families:
 * <ul>
 *   <li>{@code favourites.wager.operation{op,outcome}} — wager CRUD ops</li>
 *   <li>{@code favourites.group.operation{op,outcome}} — group CRUD ops</li>
 * </ul>
 *
 * <p>{@code outcome} is {@code ok} on success, {@code failure} on exception.
 * Prometheus renders these as {@code favourites_wager_operation_total} and
 * {@code favourites_group_operation_total}.
 *
 * Grep anchor: favourites
 */
@Component
public class FavouritesMetrics {

    public static final String WAGER_COUNTER  = "favourites.wager.operation";
    public static final String GROUP_COUNTER  = "favourites.group.operation";
    public static final String TAG_OP      = "op";
    public static final String TAG_OUTCOME = "outcome";
    public static final String OK          = "ok";
    public static final String FAILURE     = "failure";

    // Op name constants — used in DefaultFavouritesService and the JS operations tab
    public static final String OP_CREATE = "create";
    public static final String OP_GET    = "get";
    public static final String OP_UPDATE = "update";
    public static final String OP_DELETE = "delete";
    public static final String OP_LIST   = "list";

    private final MeterRegistry registry;

    public FavouritesMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void wagerOk(String op) {
        registry.counter(WAGER_COUNTER, TAG_OP, op, TAG_OUTCOME, OK).increment();
    }

    public void wagerFailure(String op) {
        registry.counter(WAGER_COUNTER, TAG_OP, op, TAG_OUTCOME, FAILURE).increment();
    }

    public void groupOk(String op) {
        registry.counter(GROUP_COUNTER, TAG_OP, op, TAG_OUTCOME, OK).increment();
    }

    public void groupFailure(String op) {
        registry.counter(GROUP_COUNTER, TAG_OP, op, TAG_OUTCOME, FAILURE).increment();
    }
}
```

#### 1.2 — Write `FavouritesMetricsTest.java`

**Path:** `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/metrics/FavouritesMetricsTest.java`

Use `SimpleMeterRegistry` (no Spring context needed):

```java
@Test void wagerOk_incrementsWagerCounter()
@Test void wagerFailure_incrementsWagerFailureCounter()
@Test void groupOk_incrementsGroupCounter()
@Test void groupFailure_incrementsGroupFailureCounter()
@Test void multipleOps_counterPerOpLabel()  // call wagerOk("create") twice, wagerOk("get") once — verify totals per label
```

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/metrics/FavouritesMetrics.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/metrics/FavouritesMetricsTest.java
```

---

## Phase 2 — Wire `FavouritesMetrics` into `DefaultFavouritesService`

### Goal
All 10 service operations instrumented; unit tests verify counter increments on success and failure.

### Files to read before starting
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java`
- `czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/DefaultFavouritesServiceTest.java`

### Tasks

#### 2.1 — Add `FavouritesMetrics` to `DefaultFavouritesService`

Constructor-inject `FavouritesMetrics metrics`. Wrap each of the 10 public methods with try/catch:

```java
public FavouriteWagerDto createWager(FavouriteWagerDto dto, String playerId) {
    try {
        // ... existing body ...
        metrics.wagerOk(OP_CREATE);
        return result;
    } catch (RuntimeException e) {
        metrics.wagerFailure(OP_CREATE);
        throw e;
    }
}
```

Wager ops use `metrics.wager*(op)`. Group ops use `metrics.group*(op)`.

Op constants: use `FavouritesMetrics.OP_CREATE` etc. (not inline strings).

#### 2.2 — Update `DefaultFavouritesServiceTest`

Inject a real `SimpleMeterRegistry` into the service under test. After each existing test, assert
the relevant counter incremented. Add two new tests:
- `createWager_onFailure_incrementsWagerFailureCounter` — mock throws, assert `wager.failure(create)=1`
- `createGroup_onFailure_incrementsGroupFailureCounter` — mock throws, assert `group.failure(create)=1`

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/java/cz/bsl/czh/favourites/service/DefaultFavouritesService.java \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/service/DefaultFavouritesServiceTest.java \
  -w "Constructor of DefaultFavouritesService gains a new FavouritesMetrics arg — integration tests auto-wire it from Spring context (no change needed there)."
```

---

## Phase 3 — Integration smoke test + sign-off

### Goal
`/actuator/prometheus` returns the counter names after a wager or group operation; sign-off confirmed.

### Tasks

#### 3.1 — Add Prometheus smoke test to `FavouritesRestSmokeTest`

Extend the existing `FavouritesRestSmokeTest` (or add a new `FavouritesPrometheusTest`) to:
1. POST one wager (via `/favourites/wagers`) to fire `favourites.wager.operation{op=create,outcome=ok}`
2. GET `/actuator/prometheus` (on management port, but management is on same port in test via `-1`)
3. Assert the response body contains `favourites_wager_operation_total`

#### 3.2 — Security note

`FavouritesMetrics` exposes no user data; the metric labels are constants (not user input). No
security action needed.

#### 3.3 — Routing map

Add `FavouritesMetrics` to `.requirements/design/routing.md` (Section 1 + Section 3).

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java \
  -r .requirements/design/routing.md \
  -w "norun active — run ./mvn.sh -pl czh-favorites-app test to validate"
```
