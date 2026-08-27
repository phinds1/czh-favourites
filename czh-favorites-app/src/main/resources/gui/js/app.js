/* app.js — tab + refresh + cmd shell (stub; full port in Phase 3) */
window.GisGui = (function () {
    var snapshots = [];
    var activeId = null;
    var refreshSecs = 10;
    var refreshTimer = null;
    function el(id) { return document.getElementById(id); }
    function showError(msg) { var e = el('error-msg'); if (e) e.textContent = msg; }
    function clearError() { var e = el('error-msg'); if (e) e.textContent = ''; }
    function registerSnapshot(id, label, mod) {
        snapshots.push({ id: id, label: label, mod: mod });
        var tabs = el('snap-tabs');
        var content = el('snap-content');
        var li = document.createElement('li');
        li.className = 'nav-item';
        li.innerHTML = '<a id="tab-' + id + '" class="nav-link" href="#" role="tab">' + label + '</a>';
        tabs.appendChild(li);
        var pane = document.createElement('div');
        pane.id = 'pane-' + id;
        pane.className = 'tab-pane';
        pane.setAttribute('role', 'tabpanel');
        content.appendChild(pane);
        li.querySelector('a').addEventListener('click', function (e) { e.preventDefault(); switchTo(id); });
        if (snapshots.length === 1) switchTo(id);
    }
    function switchTo(id) {
        snapshots.forEach(function (s) {
            var tab = el('tab-' + s.id); var pane = el('pane-' + s.id);
            if (tab) tab.classList.remove('active');
            if (pane) pane.classList.remove('active');
        });
        var tab = el('tab-' + id); var pane = el('pane-' + id);
        if (tab) tab.classList.add('active');
        if (pane) pane.classList.add('active');
        activeId = id;
        var snap = snapshots.find(function (s) { return s.id === id; });
        if (snap) el('snapshot-title').textContent = '**** ' + snap.label.toUpperCase() + ' SNAPSHOT ****';
        clearError();
        var cmdEl = el('cmd-input'); if (cmdEl) cmdEl.focus();
        restartRefresh();
    }
    function restartRefresh() {
        if (refreshTimer) clearInterval(refreshTimer);
        if (window.GisClock) window.GisClock.setRefreshSecs(refreshSecs);
        refresh();
        refreshTimer = setInterval(refresh, refreshSecs * 1000);
    }
    function refresh() {
        if (!activeId) return;
        var snap = snapshots.find(function (s) { return s.id === activeId; });
        if (!snap || !snap.mod || !snap.mod.render) return;
        snap.mod.render('pane-' + activeId).catch(function (err) { showError(err.message); });
    }
    function init() {
        var sel = el('refresh-select');
        if (sel) {
            sel.addEventListener('change', function () {
                refreshSecs = parseInt(sel.value, 10);
                var rdisp = el('refresh-rate-display'); if (rdisp) rdisp.textContent = refreshSecs + 's';
                restartRefresh();
            });
        }
        var cmd = el('cmd-input');
        if (cmd) {
            cmd.addEventListener('keydown', function (e) {
                if (e.key !== 'Enter') return;
                var val = cmd.value.trim(); cmd.value = '';
                var refM = val.match(/^ref\s+(\d+)$/i);
                if (refM) { refreshSecs = parseInt(refM[1], 10); restartRefresh(); return; }
                var tabMatch = snapshots.find(function (s) {
                    var lv = val.toLowerCase();
                    return s.label.toLowerCase().startsWith(lv) || s.id.toLowerCase().startsWith(lv);
                });
                if (tabMatch) switchTo(tabMatch.id);
            });
        }
    }
    document.addEventListener('DOMContentLoaded', init);
    return { registerSnapshot: registerSnapshot, showError: showError, clearError: clearError };
})();
