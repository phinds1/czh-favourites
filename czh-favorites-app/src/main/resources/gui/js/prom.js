/* prom.js — shared prometheus text-exposition parser (stub; full port in Phase 3) */
window.GisProm = (function () {
    function parse(text) {
        var rows = [], meta = {};
        (text || '').split('\n').forEach(function (line) {
            var t = line.trim();
            if (!t) return;
            var hm = t.match(/^# HELP ([^\s]+)\s+(.*)/);
            if (hm) { if (!meta[hm[1]]) meta[hm[1]] = {}; meta[hm[1]].help = hm[2]; return; }
            var tm = t.match(/^# TYPE ([^\s]+)\s+(.*)/);
            if (tm) { if (!meta[tm[1]]) meta[tm[1]] = {}; meta[tm[1]].type = tm[2]; return; }
            var m = t.match(/^([a-zA-Z_:][a-zA-Z0-9_:]*)(\{[^}]*\})?\s+([\d.eE+\-]+)/);
            if (!m) return;
            var name = m[1], labelStr = m[2] ? m[2].slice(1, -1) : '', labels = {};
            if (labelStr) {
                (labelStr.match(/[a-zA-Z_][a-zA-Z0-9_]*="[^"]*"/g) || []).forEach(function (pair) {
                    var eq = pair.indexOf('=');
                    labels[pair.slice(0, eq)] = pair.slice(eq + 2, -1);
                });
            }
            var info = meta[name] || {};
            rows.push({ name: name, labels: labels, labelStr: labelStr, value: parseFloat(m[3]), help: info.help || '', type: info.type || '' });
        });
        return rows;
    }
    return { parse: parse };
})();
