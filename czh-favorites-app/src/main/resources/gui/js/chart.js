/* chart.js — canvas sparkline, multi-series + per-series color.
 * Ported from the reference (single green series) and extended so one chart can hold multiple
 * named series (used by operations.js for ok-green + failure-red in one graph). The single-series
 * API (push(value), opts.color) still works for the Metrics tab's click-to-graph. */
window.GisChart = (function () {
    var DEFAULT_COLOR = '#00cc66';
    function create(canvas, opts) {
        opts = opts || {};
        var windowSecs = opts.windowSecs || 60;
        var label = opts.label || '';
        var defaultColor = opts.color || DEFAULT_COLOR;
        // series: key -> { color, points }; the anonymous/default series key is ''.
        var series = {};
        function seriesOf(key) {
            var k = key || '';
            if (!series[k]) series[k] = { color: defaultColor, points: [] };
            return series[k];
        }
        var ctx = canvas.getContext('2d');
        function trim(s) {
            var cutoff = Date.now() - windowSecs * 1000;
            s.points = s.points.filter(function (p) { return p.t >= cutoff; });
        }
        // Single-series push (legacy API for the Metrics tab): onto the anonymous series.
        function push(value) {
            var s = seriesOf('');
            s.points.push({ value: value, t: Date.now() });
            trim(s);
            draw();
        }
        // Named-series push (for operations.js): push value onto series `key`, assigning color on
        // first sighting.
        function pushSeries(key, value, color) {
            var s = seriesOf(key);
            if (color) s.color = color;
            s.points.push({ value: value, t: Date.now() });
            trim(s);
            draw();
        }
        function draw() {
            var w = canvas.offsetWidth || 200;
            var h = canvas.offsetHeight || 100;
            canvas.width = w;
            canvas.height = h;
            ctx.clearRect(0, 0, w, h);
            ctx.fillStyle = '#0a0a0a';
            ctx.fillRect(0, 0, w, h);
            var allPoints = [];
            Object.keys(series).forEach(function (k) {
                series[k].points.forEach(function (p) { allPoints.push(p.value); });
            });
            if (allPoints.length < 2) return;
            var minV = Math.min.apply(null, allPoints);
            var maxV = Math.max.apply(null, allPoints);
            if (maxV === minV) maxV = minV + 1;
            var pad = 4;
            var now = Date.now();
            function xOf(age) { return w - pad - (age / windowSecs) * (w - 2 * pad); }
            function yOf(v) { return h - pad - ((v - minV) / (maxV - minV)) * (h - 2 * pad); }
            ctx.font = '9px monospace';
            ctx.fillStyle = '#444';
            ctx.fillText(maxV.toPrecision(4), pad + 2, pad + 9);
            ctx.fillText(minV.toPrecision(4), pad + 2, h - pad - 2);
            Object.keys(series).forEach(function (k) {
                var s = series[k];
                if (s.points.length < 1) return;
                ctx.strokeStyle = s.color;
                ctx.lineWidth = 1.5;
                ctx.beginPath();
                s.points.forEach(function (p, i) {
                    var age = (now - p.t) / 1000;
                    var x = Math.max(pad, xOf(age));
                    var y = yOf(p.value);
                    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                });
                ctx.stroke();
            });
            if (label) { ctx.fillStyle = defaultColor; ctx.fillText(label, Math.max(pad + 2, w - label.length * 5.5), h - pad - 2); }
        }
        return { push: push, pushSeries: pushSeries, draw: draw };
    }
    return { create: create };
})();
