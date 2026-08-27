/* api.js — fetch helpers (stub; full port in Phase 3) */
window.GisApi = (function () {
    function get(path) {
        return fetch(path).then(function (r) {
            if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
            return r.text();
        });
    }
    function getWithStatus(path) {
        return fetch(path).then(function (r) {
            return r.text().then(function (body) { return { status: r.status, body: body }; });
        });
    }
    function getJson(path) {
        return fetch(path).then(function (r) {
            if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
            return r.json();
        });
    }
    return { get: get, getWithStatus: getWithStatus, getJson: getJson };
})();
