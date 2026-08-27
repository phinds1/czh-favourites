# SP10: Vision GUI — Detailed Implementation Plan

**Sub-plan:** SP10 of 10  
**Sanename:** `favourites`  
**Root package:** `cz.bsl.favourites.config` (GuiWebConfig only)  
**Progress state:** `.planning/state/SP10_VISION_GUI_PROGRESS.md`

## Overview

Implement the operator Vision GUI — a single-page dark-theme dashboard served at `/gui/` from
`czh-favorites-app`. Three tabs:

| Tab             | Data source                                                | Key content |
|-----------------|------------------------------------------------------------|-------------|
| **Server**      | `/actuator/health`, `/actuator/prometheus`                  | UP/DOWN badge, db status, uptime, JVM heap bar |
| **Operations**  | `/actuator/prometheus`                                      | Per-op TPM bar graphs from `favourites_wager_operation_total` + `favourites_group_operation_total` |
| **Player Lookup** | `/admin/favourites/players/{id}/wagers` + `/groups`      | playerId input, wager/group count, wager name table |

Most JS is **copied verbatim** from the czh-money reference in
`references/czh-money/money-app/src/main/resources/gui/`.
Only `operations.js` is adapted (metric names) and `player-lookup.js` is new.

**Dependencies:** SP7 (admin API paths), SP8 (Prometheus counter names).

---

## Phase 1 — Spring MVC resource handler + HTML shell

### Goal
`GET /gui/index.html` returns 200 with the tab shell and correct JS script tags.

### Files to read before starting
- `references/czh-money/money-app/src/main/resources/gui/index.html`
- `references/czh-money/money-app/src/main/resources/gui/js/app.js`

### Tasks

#### 1.1 — Create `GuiWebConfig.java`

**Path:** `czh-favorites-app/src/main/java/cz/bsl/favourites/config/GuiWebConfig.java`

```java
package cz.bsl.favourites.config;

// Grep anchor: favourites

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the operator Vision GUI static files from {@code classpath:/gui/} at {@code /gui/**}.
 */
@Configuration
public class GuiWebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/gui/**")
                .addResourceLocations("classpath:/gui/");
    }
}
```

#### 1.2 — Copy shared JS + CSS verbatim

Copy **exactly** from `references/czh-money/money-app/src/main/resources/gui/`:

| Source | Destination |
|--------|-------------|
| `css/gui.css` | `czh-favorites-app/src/main/resources/gui/css/gui.css` |
| `js/api.js` | `czh-favorites-app/src/main/resources/gui/js/api.js` |
| `js/app.js` | `czh-favorites-app/src/main/resources/gui/js/app.js` |
| `js/clock.js` | `czh-favorites-app/src/main/resources/gui/js/clock.js` |
| `js/prom.js` | `czh-favorites-app/src/main/resources/gui/js/prom.js` |
| `js/chart.js` | `czh-favorites-app/src/main/resources/gui/js/chart.js` |
| `favicon.png` | `czh-favorites-app/src/main/resources/gui/favicon.png` |

**No changes** to these files — the shared infrastructure is identical.

#### 1.3 — Write `index.html`

**Path:** `czh-favorites-app/src/main/resources/gui/index.html`

Port `references/czh-money/.../index.html` with these substitutions:

- `<title>czh-money 👀</title>` → `<title>czh-favourites Vision 👀</title>`
- Navbar brand text: `czh-money Vision 👀` → `czh-favourites Vision 👀`
- Script tags: replace `snapshots/operations.js` + `snapshots/metrics.js` with:
  ```html
  <script src="/gui/js/snapshots/server.js"></script>
  <script src="/gui/js/snapshots/operations.js"></script>
  <script src="/gui/js/snapshots/player-lookup.js"></script>
  ```
- Add `<link rel="icon" href="/gui/favicon.png">`

#### 1.4 — Copy `server.js` verbatim

Copy `references/czh-money/.../snapshots/server.js` to
`czh-favorites-app/src/main/resources/gui/js/snapshots/server.js` with **one** change:  
`GisGui.registerSnapshot('server', 'Server', ...)` — the registration call is already correct
(generic tab name); no change needed. Copy verbatim.

#### 1.5 — Add smoke test to `FavouritesRestSmokeTest`

Add one test:
```java
@Test void guiIndex_returns200() {
    ResponseEntity<String> resp = restTemplate.getForEntity("/gui/index.html", String.class);
    assertEquals(200, resp.getStatusCode().value());
    assertTrue(resp.getBody().contains("czh-favourites Vision"));
}
```

```sh
~/bin/geany-progress done 1 \
  -r czh-favorites-app/src/main/java/cz/bsl/favourites/config/GuiWebConfig.java \
  -r czh-favorites-app/src/main/resources/gui/index.html \
  -r czh-favorites-app/src/test/java/cz/bsl/czh/favourites/test/FavouritesRestSmokeTest.java \
  -w "norun active. Test smoke with: ./mvn.sh -pl czh-favorites-app test after clear."
```

---

## Phase 2 — Operations tab (`operations.js`)

### Goal
`operations.js` discovers ops dynamically from `favourites_wager_operation_total` and
`favourites_group_operation_total` counters and renders one TPM bar graph per op.

### Files to read before starting
- `references/czh-money/money-app/src/main/resources/gui/js/snapshots/operations.js`
  (entire file — adapt, do not rewrite from scratch)
- `czh-favorites-app/src/main/java/cz/bsl/czh/favourites/metrics/FavouritesMetrics.java`
  (counter name constants: `WAGER_COUNTER = "favourites.wager.operation"` →
  Prometheus renders as `favourites_wager_operation_total`)

### Tasks

#### 2.1 — Write `operations.js`

**Path:** `czh-favorites-app/src/main/resources/gui/js/snapshots/operations.js`

Port from czh-money `operations.js` with these changes:

1. **Metric names** — the counter check:
   ```js
   // czh-money had:
   if (r.name !== 'money_operation_total') return;
   // replace with two checks (wager + group are separate counter families):
   if (r.name !== 'favourites_wager_operation_total' && r.name !== 'favourites_group_operation_total') return;
   ```

2. **Op key prefix** — include the metric family in the graph title so wager and group ops are
   distinguished:
   ```js
   // When building the op label shown in the graph card, prefix with the family:
   // "favourites_wager_operation_total" + op="create" → graph title: "wager / create"
   var family = r.name.includes('wager') ? 'wager' : 'group';
   var opKey = family + '/' + r.labels.op;
   ```
   Use `opKey` as the key in `prevSample` and `graphs` maps (replacing bare `op`).

3. **Comment update** — replace all references to `money_operation_total` with
   `favourites_wager_operation_total` / `favourites_group_operation_total`.

4. **Footer text** — replace `"discovered from money_operation_total labels"` with
   `"discovered from favourites_wager_operation_total / favourites_group_operation_total labels"`.

5. **Tab registration** — keep `GisGui.registerSnapshot('operations', 'Operations', ...)`.

All graph layout, animation, TPM delta logic, and lazy-load behaviour is **identical to czh-money
— do not change it**.

```sh
~/bin/geany-progress done 2 \
  -r czh-favorites-app/src/main/resources/gui/js/snapshots/operations.js \
  -w "Operations tab is lazy: graphs only appear after traffic hits the service. In test: POST a wager then refresh the GUI."
```

---

## Phase 3 — Player Lookup tab (`player-lookup.js`)

### Goal
The Player Lookup tab renders: a `playerId` text input + Submit button; on submit, calls the admin
API (SP7) and displays wager count, group count, and a scrollable table of wager name + game name.

### Files to read before starting
- `references/czh-money/money-app/src/main/resources/gui/js/api.js` (GisApi.get — use for fetch)
- `czh-favorites-app/src/main/java/cz/bsl/favourites/web/AdminFavouritesController.java`
  (endpoint paths: `/admin/favourites/players/{id}/wagers` and `/groups`)

### Tasks

#### 3.1 — Write `player-lookup.js`

**Path:** `czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js`

This is a new file with no czh-money equivalent.

```js
/* global GisApi, GisGui */
/* player-lookup.js — admin player lookup tab.
 * Calls /admin/favourites/players/{id}/wagers and /groups on submit.
 * Grep anchor: favourites
 */
(function () {
    var built = false;

    async function render(containerId) {
        var pane = document.getElementById(containerId);
        if (!pane) return;

        if (!built) {
            pane.innerHTML =
                '<p class="snapshot-section">Player Lookup</p>' +
                '<div class="d-flex gap-2 mb-3">' +
                  '<input id="player-id-input" type="text" class="form-control form-control-sm bg-black border-secondary font-monospace text-success" style="max-width:320px" placeholder="playerId" autocomplete="off">' +
                  '<button id="player-lookup-btn" class="btn btn-sm btn-outline-success font-monospace">Lookup</button>' +
                '</div>' +
                '<div id="player-lookup-result"></div>';

            document.getElementById('player-lookup-btn').addEventListener('click', function () {
                var playerId = document.getElementById('player-id-input').value.trim();
                if (playerId) doLookup(playerId);
            });
            built = true;
        }
    }

    async function doLookup(playerId) {
        var result = document.getElementById('player-lookup-result');
        result.innerHTML = '<span class="text-secondary small font-monospace">Loading…</span>';

        var res = await Promise.allSettled([
            GisApi.get('/admin/favourites/players/' + encodeURIComponent(playerId) + '/wagers'),
            GisApi.get('/admin/favourites/players/' + encodeURIComponent(playerId) + '/groups')
        ]);

        var wagersOk  = res[0].status === 'fulfilled';
        var groupsOk  = res[1].status === 'fulfilled';

        if (!wagersOk && !groupsOk) {
            result.innerHTML = '<span class="text-danger small">Player not found or error: ' + res[0].reason.message + '</span>';
            return;
        }

        var wagers = wagersOk ? JSON.parse(res[0].value) : { items: [], totalCount: 0 };
        var groups = groupsOk ? JSON.parse(res[1].value) : { items: [], totalCount: 0 };

        var rows = (wagers.items || []).map(function (w) {
            return '<tr><td class="font-monospace small">' + escHtml(w.wagerName || '—') + '</td>' +
                   '<td class="font-monospace small text-info">' + escHtml(w.gameName || '—') + '</td>' +
                   '<td class="font-monospace small text-secondary">' + escHtml(w.groupNumber || '—') + '</td></tr>';
        }).join('');

        result.innerHTML =
            '<div class="arte-grid mb-3">' +
              '<div class="arte-key">playerId</div><div class="arte-val font-monospace text-success">' + escHtml(playerId) + '</div>' +
              '<div class="arte-key">wagers</div><div class="arte-val">' + wagers.totalCount + '</div>' +
              '<div class="arte-key">groups</div><div class="arte-val">' + groups.totalCount + '</div>' +
            '</div>' +
            (rows
              ? '<table class="arte-table"><thead><tr><th>Wager Name</th><th>Game</th><th>Group</th></tr></thead><tbody>' + rows + '</tbody></table>'
              : '<p class="text-secondary small">No favourite wagers for this player.</p>');
    }

    function escHtml(s) {
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    GisGui.registerSnapshot('player', 'Player Lookup', { render: render });
})();
```

**Key design notes:**
- The tab does **not** auto-refresh (no data on load — user-initiated only); `render` builds the
  form once and returns. The `built` flag prevents form rebuild on auto-refresh ticks.
- `GisApi.get` already uses `fetch` and returns text; wrap in `JSON.parse` manually.
- On 404 (player not found): `GisApi.get` throws — caught by `allSettled`, displayed as "not found".
- `encodeURIComponent` on `playerId` prevents path injection.

```sh
~/bin/geany-progress done 3 \
  -r czh-favorites-app/src/main/resources/gui/js/snapshots/player-lookup.js \
  -w "Player Lookup calls admin API from the browser. In local dev the admin endpoints are unprotected — this is intentional (SP7 Javadoc). In production the GUI must be served behind the admin gateway."
```

---

## Phase 4 — Sign-off

### Tasks

#### 4.1 — Smoke test passes

```bash
./mvn.sh -pl czh-favorites-app test
```

Confirm `guiIndex_returns200` passes.

#### 4.2 — Manual browser check (operator step)

Start the app locally (`./mvn.sh spring-boot:run -pl czh-favorites-app`) and open
`http://localhost:9290/gui/index.html`. Verify:
- Server tab: UP/DOWN badge renders, JVM heap bar shows non-zero values
- Operations tab: shows "No operations yet" before any traffic
- Player Lookup tab: input box present; entering a known `playerId` and clicking Lookup shows counts

#### 4.3 — Routing map

Add `GuiWebConfig` to `.requirements/design/routing.md`.

```sh
~/bin/geany-progress done 4 \
  -r .requirements/design/routing.md \
  -w "Browser smoke check (step 4.2) is manual — not automated. norun active for unit tests."
```
