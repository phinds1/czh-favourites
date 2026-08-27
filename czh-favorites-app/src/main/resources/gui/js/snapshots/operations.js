/* global GisApi, GisGui, GisProm, GisChart */
/* operations.js — per-op ok/error TPM graphs for czh-favourites.
 *
 * Discovers ops DYNAMICALLY from favourites_wager_operation_total and
 * favourites_group_operation_total labels (do NOT hardcode the op list):
 * for each op seen in the scrape, render one graph with two series — outcome="ok" (green) and
 * outcome="failure" (red) — graphed as TPM (transactions-per-minute) computed from the counter
 * delta vs the previous scrape.
 *
 * Op keys are prefixed with the counter family so wager and group ops are distinguished:
 * "wager/create", "wager/get", "group/create", etc.
 *
 * Both counter families are LAZY (absent until the first op fires); the tab shows "no operations
 * yet" until traffic, then fills in as ops run.
 *
 * Grep anchor: favourites
 */
(function () {
    var OK_COLOR = '#00cc66';
    var FAIL_COLOR = '#dc3545';
    var GRAPH_WINDOW_SECS = 60;
    // state persisted across renders: per (opKey,outcome) prev sample for TPM, + the graph cards
    // (keyed by opKey) so we keep the canvas + chart instance across refreshes instead of rebuilding.
    var prevSample = {};      // "opKey|outcome" -> { value, time }
    var graphs = {};          // opKey -> { div, chart }
    var built = false;

    function rowKey(opKey, outcome) { return opKey + '|' + outcome; }

    function fmtTpm(v) {
        if (v === null || v === undefined) return '—';
        if (v < 0.01) return '0';
        if (v < 10) return v.toFixed(2);
        return Math.round(v).toLocaleString();
    }

    /* Collect { opKey -> { ok: {value, tpm}, failure: {value, tpm} } } from the prometheus rows,
     * stamping TPM from prevSample and updating prevSample for the next render.
     * opKey is "wager/<op>" or "group/<op>" to distinguish the two counter families. */
    function collectOps(rows, now) {
        var ops = {};
        rows.forEach(function (r) {
            var family;
            if (r.name === 'favourites_wager_operation_total') {
                family = 'wager';
            } else if (r.name === 'favourites_group_operation_total') {
                family = 'group';
            } else {
                return;
            }
            var op = r.labels.op, outcome = r.labels.outcome;
            if (!op || !outcome) return;
            var opKey = family + '/' + op;
            if (!ops[opKey]) ops[opKey] = { ok: { value: 0, tpm: null }, failure: { value: 0, tpm: null } };
            ops[opKey][outcome] = { value: r.value, tpm: null };
            // TPM from prevSample delta
            var k = rowKey(opKey, outcome);
            var prev = prevSample[k];
            if (prev) {
                var elapsed = (now - prev.time) / 1000;
                var delta = r.value - prev.value;
                ops[opKey][outcome].tpm = (elapsed > 0 && delta >= 0) ? (delta / elapsed) * 60 : null;
            }
            prevSample[k] = { value: r.value, time: now };
        });
        return ops;
    }

    function ensureGraph(pane, opKey) {
        if (graphs[opKey]) return graphs[opKey];
        var card = document.createElement('div');
        card.className = 'op-graph-card col-md-6';
        card.innerHTML =
            '<div class="d-flex justify-content-between align-items-center mb-1">' +
              '<span class="small font-monospace text-info">' + opKey + ' (ok / fail TPM)</span>' +
              '<span class="chart-legend small">' +
                '<span><span class="swatch" style="background:' + OK_COLOR + '"></span>ok</span>' +
                '<span><span class="swatch" style="background:' + FAIL_COLOR + '"></span>fail</span>' +
              '</span>' +
            '</div>' +
            '<canvas class="chart-canvas" style="height:90px"></canvas>';
        var graphsGrid = pane.querySelector('#op-graphs');
        graphsGrid.appendChild(card);
        var chart = GisChart.create(card.querySelector('canvas'), { windowSecs: GRAPH_WINDOW_SECS, label: opKey });
        graphs[opKey] = { div: card, chart: chart };
        return graphs[opKey];
    }

    function renderTable(ops) {
        var tbody = document.getElementById('op-tbody');
        if (!tbody) return;
        var opKeys = Object.keys(ops).sort();
        if (!opKeys.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-secondary">No operations yet — counters appear after the first business op fires.</td></tr>';
            return;
        }
        tbody.innerHTML = opKeys.map(function (opKey) {
            var o = ops[opKey];
            return '<tr><td class="font-monospace">' + opKey + '</td>' +
                '<td class="text-end"><span class="text-success">' + fmtTpm(o.ok.tpm) + '</span>' +
                    '<br><span class="text-secondary" style="font-size:0.65rem">' + Math.round(o.ok.value).toLocaleString() + ' total</span></td>' +
                '<td class="text-end"><span class="text-danger">' + fmtTpm(o.failure.tpm) + '</span>' +
                    '<br><span class="text-secondary" style="font-size:0.65rem">' + Math.round(o.failure.value).toLocaleString() + ' total</span></td>' +
                '<td class="text-end">' + (o.ok.value + o.failure.value).toLocaleString() + '</td></tr>';
        }).join('');
    }

    async function render(containerId) {
        var pane = document.getElementById(containerId);
        if (!pane) return;
        var res = await Promise.allSettled([GisApi.get('/actuator/prometheus')]);
        if (res[0].status !== 'fulfilled') { GisGui.showError('/actuator/prometheus: ' + res[0].reason.message); return; }
        var now = Date.now();
        var rows = GisProm.parse(res[0].value);
        var ops = collectOps(rows, now);
        var opKeys = Object.keys(ops).sort();

        if (!built) {
            pane.innerHTML =
                '<div class="row">' +
                  '<div class="col-md-12"><p class="snapshot-section">Per-Operation Ok / Error (TPM)</p>' +
                    '<div class="row" id="op-graphs"></div>' +
                    '<p class="text-secondary mt-1" style="font-size:0.7rem">One graph per op (discovered from favourites_wager_operation_total / favourites_group_operation_total labels). ok=green, failure=red. ' + GRAPH_WINDOW_SECS + 's window. New ops appear automatically.</p>' +
                  '</div>' +
                '</div>' +
                '<p class="snapshot-section">Operation Counts</p>' +
                '<table class="arte-table"><thead><tr><th>Op</th><th class="text-end">Ok (TPM)</th><th class="text-end">Failure (TPM)</th><th class="text-end">Total</th></tr></thead>' +
                '<tbody id="op-tbody"></tbody></table>';
            built = true;
        }

        var graphsGrid = pane.querySelector('#op-graphs');
        opKeys.forEach(function (opKey) {
            var g = ensureGraph(pane, opKey);
            g.chart.pushSeries('ok', ops[opKey].ok.tpm || 0, OK_COLOR);
            g.chart.pushSeries('failure', ops[opKey].failure.tpm || 0, FAIL_COLOR);
        });
        if (!opKeys.length && graphsGrid && !graphsGrid.hasChildNodes()) {
            graphsGrid.innerHTML = '<p class="text-secondary small">No operations yet.</p>';
        }
        renderTable(ops);
    }
    GisGui.registerSnapshot('operations', 'Operations', { render: render });
})();
