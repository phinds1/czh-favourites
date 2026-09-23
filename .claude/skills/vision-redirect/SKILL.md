---
name: vision-redirect
description: Proxy a microservice's Vision GUI behind nginx under a context root to avoid CORS; the backend reads an X-Vision-Base request header and rewrites its own HTML so the GUI JS rebases URLs.
allowed-tools: bash, read, write, edit
---

# vision-redirect

Recipe for putting a Vision microservice GUI behind a single nginx reverse proxy under a context
root, so the browser makes only same-origin fetches (no CORS). This is the pattern proven on
czh-autopay; apply it to the other Vision microservices.

## The problem

Each Vision microservice serves its GUI HTML on a **management port** and its business `/api/*` on
an **app port** — two different origins. The browser's cross-port fetch trips CORS, and per-service
CORS filters are a dead end (Firefox blocked them even with correct headers on autopay — the approach
was tried and removed).

## The solution

Put **one nginx reverse proxy** in front of all backends, listening on one port. nginx routes by the
**first URL segment** (the context root) and strips the prefix before proxying, so the backends keep
serving `/gui/`, `/api/`, `/actuator/` at the servlet root unchanged. Because every browser request
targets the same nginx origin, there is **no cross-origin fetch** and therefore **no CORS**.

The GUI must become **context-root aware**: nginx tells the backend which context the request arrived
under by sending an `X-Vision-Base` **request** header; the backend rewrites its own GUI HTML to
rebase its absolute URLs under that root. The base defaults to empty (root) when the header is
absent, so a GUI loaded direct on its own port at `/gui/` keeps working unchanged.

## The nginx pattern

Two/three `location` blocks per microservice. nginx routes by the first segment; the trailing slash
on `proxy_pass` strips the prefix. `proxy_set_header X-Vision-Base` carries the context root to the
backend (a REQUEST header — the browser does not see it; the backend reads it).

```nginx
# <msname>-mgmt context: the GUI's own context. The GUI's base is /<msname>-mgmt for ALL its calls.
# /<msname>-mgmt/api/ -> APP port (business API, reached through the GUI's context).
#   NOTE the /api/ on proxy_pass: it preserves the /api segment. A bare http://host:APP/ would strip
#   /<msname>-mgmt/api/ and leave /jobs/status -> 404. This is the easy mistake to make.
location /<msname>-mgmt/api/ {
    proxy_pass         http://127.0.0.1:<APP_PORT>/api/;
    proxy_set_header   Host $host;
    proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;
    proxy_set_header   X-Vision-Base "/<msname>-mgmt";
}

# /<msname>-mgmt/gui/ + /<msname>-mgmt/actuator/ -> MGMT port (GUI assets + actuator).
location /<msname>-mgmt/ {
    proxy_pass         http://127.0.0.1:<MGMT_PORT>/;
    proxy_set_header   Host $host;
    proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;
    proxy_set_header   X-Vision-Base "/<msname>-mgmt";
}

# Bare /<msname>/ -> APP port for direct/cron API access (the GUI does not use this).
location /<msname>/ {
    proxy_pass         http://127.0.0.1:<APP_PORT>/;
    proxy_set_header   Host $host;
    proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;
    proxy_set_header   X-Vision-Base "/<msname>";
}
```

Placeholders to replace: `<msname>`, `<APP_PORT>`, `<MGMT_PORT>`. The GUI is served from the
**management** context (`/<msname>-mgmt/gui/`); the business API is reached **through the GUI's own
context** (`/<msname>-mgmt/api/`) so the GUI's base is a single value for all its calls.

## Single-base routing model (why the API lives under the GUI's context)

The GUI uses **one** base (`/<msname>-mgmt`) for every call — `/gui` assets, `/api` fetches,
`/actuator` — so the JS stays trivial: `resolveUrl(path) = VISION_BASE + path`. nginx routes the
`/api` sub-path **within** the GUI's context to the app port, and `/gui` + `/actuator` to the mgmt
port. This avoids needing two bases in the JS (which the plan's original diagram implied and which
doubles the JS complexity). The bare `/<msname>/` context is kept for direct/cron API access; the
GUI does not use it.

## The GUI change (three edits)

### 1. `index.html` — the `vision-base` meta tag

Add an empty-content meta in `<head>` before any `/gui/` asset reference. The backend fills the
`content` with the context root; empty (no header) means root.

```html
<meta name="vision-base" content="">
```

Keep the asset paths absolute (`/gui/css/...`) — the backend prefixes them with the base when it
rewrites the HTML. No `<base>` tag, no relative paths needed.

### 2. `api.js` — `resolveUrl` reads the meta at runtime

Replace any cross-port/port-rewriting logic with a base-context model. The JS reads the meta once at
load and prefixes every absolute site path:

```js
var VISION_BASE = (function () {
    var meta = document.querySelector('meta[name="vision-base"]');
    return meta ? (meta.getAttribute('content') || '') : '';
})();

function resolveUrl(path) {
    return VISION_BASE + path;
}
```

Then every `fetch(resolveApi(path), ...)` becomes `fetch(resolveUrl(path), ...)`. Delete any
`APP_PORT` constant and cross-port `http://<host>:<port>` rewrite (that was the cross-origin fetch
being killed). No meta (direct on port) → base `''` → paths unchanged → today's behaviour.

### 3. Backend filter — read `X-Vision-Base`, rewrite the HTML

A servlet filter (registered on the management context where the GUI is served) reads the
`X-Vision-Base` request header and, for `text/html` responses, (a) fills the `vision-base` meta
content and (b) prefixes the `="/gui/` asset references with the base. No header → no rewrite →
direct-on-port unchanged. JS/CSS/actuator pass through byte-for-byte (the JS reads the meta at
runtime, so it does not need server-side rewriting).

**Root-path gotcha:** `GET /` (bare context root) typically forwards to `/gui/index.html` via a
view controller. A servlet `forward` commits the response before a post-chain filter can rewrite it,
so when a base is present, serve the index page directly from the classpath (rewritten) instead of
forwarding. With no base (direct on port) the forward runs as before. See the reference filter
(`VisionBaseContextFilter`) for the exact buffering + direct-serve logic.

## Frame-loadability (for iframe/portal use)

The GUI must be loadable in an `<iframe>`: send **no** `X-Frame-Options` and **no** CSP
`frame-ancestors`. Spring Boot does not set these by default, so this is usually already fine — but
assert it in the test harness (`curl -D -` shows no `X-Frame-Options`). A portal page that iframes
the per-service GUIs is a separate future task (the real `czh-nginx-gateway` deployable).

## The test harness pattern

A bash harness (`vision-test.sh`) proves the URLs work through the proxy:

1. Build the app's test module + classpath (the app can't run as a bare `java -jar` — it needs the
   test classpath: H2 in DB2 mode + in-process downstream mocks). Launch a harness main that boots
   the mocks then the app on the fixed ports (`--server.port=<APP_PORT> --management.server.port=<MGMT_PORT> --spring.profiles.active=test`).
2. Resolve the nginx config (`{{FIXTURE}}` / `{{PORT}}` placeholders) into a temp copy of the fixture
   dir (the `include` is absolute, so it must point into the temp copy, not the original). `nginx -t`
   then start nginx.
3. Assert through the proxy: GUI index.html (200 + meta injected + no X-Frame-Options), a GUI JS
   asset (200 + `resolveUrl`), the business API through the GUI's context (200 + JSON marker, with
   the `X-Admin-Id` header if the API requires it), actuator health (200 + `"status":"UP"`).
4. nginx crash check — grep the error log for worker exits / segfaults.
5. `--hold` prints the bookmarkable URLs and waits for Enter (manual browser check). An EXIT/INT/TERM
   trap tears down nginx + the Java backend by PID.

## The real-deployable differences (czh-nginx-gateway)

The production nginx (in `czh-nginx-gateway`) differs from this demo in two ways:
- **Auth** — the real proxy adds auth; the microservice backends stay auth-free (the prompt specifies
  this). The demo here adds no auth either.
- **`:80` listener** — the real proxy listens on `:80`; the test harness uses a high port (8080) to
  avoid root + port clashes. Override via the `{{PORT}}` placeholder / `--port`.

The microservice backends do **not** change between demo and prod (same context-root scheme, same
`X-Vision-Base` header, same GUI edits). nginx aggregates all microservices' context roots into one
`server` block.

## Worked example (this project)

- `test/fixtures/vision-nginx/conf.d/vision-demo.conf` — the nginx config.
- `test/fixtures/vision-nginx/nginx.conf` — the main config (placeholders `{{FIXTURE}}` / `{{PORT}}`).
- `test/vision-test.sh` — the harness (build, start, assert, teardown, `--hold`).
- `czh-autopay-app/src/main/java/cz/bsl/autopay/config/VisionBaseContextFilter.java` — the backend
  filter (reads `X-Vision-Base`, rewrites HTML; direct-serve for the root path).
- `czh-autopay-app/src/main/resources/gui/js/api.js` — the `resolveUrl` / `VISION_BASE` model.
- `czh-autopay-app/src/main/resources/gui/index.html` — the `vision-base` meta tag.
- `czh-autopay-test/src/test/java/cz/bsl/autopay/test/VisionHarnessMain.java` — the harness main
  (boots in-process mocks + the app on the fixed ports for the harness).
