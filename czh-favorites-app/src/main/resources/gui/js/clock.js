/* clock.js — browser LOCAL clock (stub; full port in Phase 3) */
window.GisClock = (function () {
    var clockTimer = null;
    function pad2(n) { return n < 10 ? '0' + n : '' + n; }
    function tick() {
        var el = document.getElementById('gtms-clock');
        if (!el) return;
        var d = new Date();
        el.textContent = pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds()) + ' LOCAL';
    }
    function setRefreshSecs(secs) {
        if (clockTimer) clearInterval(clockTimer);
        tick();
        clockTimer = setInterval(tick, 1000);
    }
    tick();
    clockTimer = setInterval(tick, 1000);
    return { setRefreshSecs: setRefreshSecs };
})();
