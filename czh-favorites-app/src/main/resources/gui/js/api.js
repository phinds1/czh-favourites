/* api.js — fetch helpers. czh-favourites is not internet-facing; no auth on the GUI (the management
 * port serves read-only ops data + the actuator; the GUI's admin fetches go to the app port through
 * the nginx proxy under the GUI's own context).
 *
 * Context-root model: the GUI may sit behind an nginx reverse proxy under a context root
 * (e.g. /favourites-mgmt). nginx routes every GUI request — /gui assets, /admin calls, /actuator —
 * under that one context, so the browser makes only same-origin fetches (no cross-port, no CORS).
 * The backend reads nginx's X-Vision-Base request header and fills <meta name="vision-base">
 * in the HTML; resolveUrl() reads that meta once at load and prefixes every absolute site path
 * with it. No meta (GUI loaded direct on its own port at /gui/) → base '' → today's behaviour. */
window.GisApi = (function () {
    /* The context root the GUI was loaded under, read once from the <meta name="vision-base"> tag
     * the backend injects. Empty when the GUI is served direct on its own port (no nginx) →
     * resolveUrl() leaves paths unchanged. */
    var VISION_BASE = (function () {
        var meta = document.querySelector('meta[name="vision-base"]');
        return meta ? (meta.getAttribute('content') || '') : '';
    })();
    /* Prefix an absolute site path (/admin/..., /actuator/..., /gui/...) with the context base.
     * Every request then targets the same origin (the nginx listener) — no cross-port rewrite. */
    function resolveUrl(path) {
        return VISION_BASE + path;
    }
    function get(path) {
        return fetch(resolveUrl(path)).then(function (r) {
            if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
            return r.text();
        });
    }
    function getWithStatus(path) {
        return fetch(resolveUrl(path)).then(function (r) {
            return r.text().then(function (body) { return { status: r.status, body: body }; });
        });
    }
    function getJson(path) {
        return fetch(resolveUrl(path)).then(function (r) {
            if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
            return r.json();
        });
    }
    function postJson(path, body) {
        return fetch(resolveUrl(path), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: body == null ? null : JSON.stringify(body)
        }).then(function (r) {
            return r.text().then(function (text) {
                if (!r.ok) throw new Error(r.status + ' ' + r.statusText + (text ? ': ' + text : ''));
                return text ? JSON.parse(text) : null;
            });
        });
    }
    /* Escape API-derived strings before inserting into innerHTML. The reference GUI uses
     * innerHTML for layout; any field from an API response is host-generated but not trusted —
     * escape to prevent XSS. For pure text cells, prefer textContent, but where innerHTML
     * templating is used, run values through esc(). */
    function esc(s) {
        if (s == null) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }
    return { get: get, getWithStatus: getWithStatus, getJson: getJson, postJson: postJson, esc: esc, resolveUrl: resolveUrl };
})();
