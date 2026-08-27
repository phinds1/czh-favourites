/* global GisApi, GisGui, GisProm */
/* server.js — Server snapshot: is the app up + uptime + memory (the task's first 3 requirements).
 * Adapts the reference server.js (which expected plain-text /actuator/health) to Spring Boot's JSON
 * health + the prometheus uptime/memory gauges. */
(function () {
    function badge(ok, yes, no) {
        return '<span class="badge ' + (ok ? 'badge-up' : 'badge-down') + '">' + (ok ? yes : no) + '</span>';
    }
    function fmtBytes(b) {
        if (b == null || !isFinite(b)) return '—';
        var mb = b / (1024 * 1024);
        if (mb < 1024) return mb.toFixed(1) + ' MiB';
        return (mb / 1024).toFixed(2) + ' GiB';
    }
    function fmtUptime(seconds) {
        if (seconds == null || !isFinite(seconds)) return '—';
        var s = Math.floor(seconds);
        var d = Math.floor(s / 86400); s -= d * 86400;
        var h = Math.floor(s / 3600); s -= h * 3600;
        var m = Math.floor(s / 60); s -= m * 60;
        function pad2(n) { return n < 10 ? '0' + n : '' + n; }
        return (d > 0 ? d + 'd ' : '') + pad2(h) + ':' + pad2(m) + ':' + pad2(s);
    }
    /* Sum a gauge's value across rows matching a label predicate (e.g. jvm_memory_used_bytes area=heap).
     * valuePred (optional) drops rows whose value fails it — used to skip jvm_memory_max_bytes rows
     * that report -1 (undefined max for some G1 regions). */
    function sumGauge(rows, name, labelPred, valuePred) {
        var total = 0, found = false;
        rows.forEach(function (r) {
            if (r.name === name && labelPred(r.labels) && (!valuePred || valuePred(r.value))) {
                total += r.value; found = true;
            }
        });
        return found ? total : null;
    }
    function gaugeValue(rows, name) {
        // unlabelled gauge (e.g. process_uptime_seconds has no labels)
        var row = rows.find(function (r) { return r.name === name && !r.labelStr; });
        return row ? row.value : null;
    }
    async function render(containerId) {
        var pane = document.getElementById(containerId);
        if (!pane) return;
        var res = await Promise.allSettled([
            GisApi.get('/actuator/health'), GisApi.get('/actuator/prometheus')
        ]);
        var healthText = res[0].status === 'fulfilled' ? res[0].value : '';
        var promText = res[1].status === 'fulfilled' ? res[1].value : '';
        var up = false, dbStatus = null, components = {};
        try {
            var h = JSON.parse(healthText);
            up = h.status === 'UP';
            components = h.components || {};
            dbStatus = components.db ? components.db.status : null;
        } catch (e) { up = false; }

        var rows = GisProm.parse(promText);
        var uptime = gaugeValue(rows, 'process_uptime_seconds');
        var used = sumGauge(rows, 'jvm_memory_used_bytes', function (l) { return l.area === 'heap'; });
        var committed = sumGauge(rows, 'jvm_memory_committed_bytes', function (l) { return l.area === 'heap'; });
        // max: skip -1 (undefined max for some G1 regions like Eden/Survivor)
        var max = sumGauge(rows, 'jvm_memory_max_bytes', function (l) { return l.area === 'heap'; }, function (v) { return v > 0; });
        var usedPct = (used != null && max != null && max > 0) ? (used / max) * 100 : null;
        var committedPct = (committed != null && max != null && max > 0) ? (committed / max) * 100 : 0;

        var compRows = Object.keys(components).map(function (k) {
            var st = components[k].status || 'UNKNOWN';
            return '<div class="arte-key">' + k + '</div><div class="arte-val">' +
                badge(st === 'UP', st, st) + '</div>';
        }).join('');

        pane.innerHTML =
            '<div class="row">' +
              '<div class="col-md-4"><p class="snapshot-section">Health</p><div class="arte-grid">' +
                '<div class="arte-key">status</div><div class="arte-val">' + badge(up, 'UP', 'DOWN') + '</div>' +
                '<div class="arte-key">db</div><div class="arte-val">' + (dbStatus ? badge(dbStatus === 'UP', dbStatus, dbStatus) : '—') + '</div>' +
                '<div class="arte-key">uptime</div><div class="arte-val font-monospace">' + fmtUptime(uptime) + '</div>' +
              '</div></div>' +
              '<div class="col-md-8"><p class="snapshot-section">Health Components</p><div class="arte-grid">' +
                (compRows || '<div class="arte-key">—</div><div class="arte-val text-secondary">no components</div>') +
              '</div></div>' +
            '</div>' +
            '<p class="snapshot-section">JVM Heap Memory</p>' +
            '<div class="row">' +
              '<div class="col-md-8">' +
                '<div class="mem-bar"><div class="mem-committed" style="width:' + committedPct.toFixed(1) + '%"></div><div class="mem-used" style="width:' + (usedPct != null ? usedPct.toFixed(1) : 0) + '%"></div></div>' +
                '<div class="small text-secondary mt-1">used ' + fmtBytes(used) + ' / committed ' + fmtBytes(committed) + ' / max ' + fmtBytes(max) +
                (usedPct != null ? ' (' + usedPct.toFixed(1) + '% of max)' : '') + '</div>' +
              '</div>' +
            '</div>';
    }
    GisGui.registerSnapshot('server', 'Server', { render: render });
})();
