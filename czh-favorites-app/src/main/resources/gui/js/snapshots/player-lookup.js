/* global GisApi, GisGui */
/* player-lookup.js — admin player lookup tab.
 * Calls /admin/favourites/players/{id}/wagers and /groups on submit.
 * Renders wager count, group count, and a table of wager name + game name.
 *
 * The tab does NOT auto-refresh — it is user-initiated only. The render function
 * builds the form once (built flag) and returns on subsequent auto-refresh ticks.
 *
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
                '<p class="text-secondary small mb-2">Enter a player ID to view their favourite wagers and groups via the admin API.</p>' +
                '<div class="d-flex gap-2 mb-3">' +
                  '<input id="player-id-input" type="text" ' +
                    'class="form-control form-control-sm bg-black border-secondary font-monospace text-success" ' +
                    'style="max-width:320px" placeholder="playerId" autocomplete="off" spellcheck="false">' +
                  '<button id="player-lookup-btn" class="btn btn-sm btn-outline-success font-monospace">Lookup</button>' +
                '</div>' +
                '<div id="player-lookup-result"></div>';

            document.getElementById('player-lookup-btn').addEventListener('click', function () {
                var playerId = (document.getElementById('player-id-input').value || '').trim();
                if (playerId) {
                    doLookup(playerId);
                } else {
                    document.getElementById('player-lookup-result').innerHTML =
                        '<span class="text-warning small">Enter a player ID first.</span>';
                }
            });

            // Also trigger on Enter key in the input
            document.getElementById('player-id-input').addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    var playerId = (this.value || '').trim();
                    if (playerId) doLookup(playerId);
                }
            });

            built = true;
        }
        // No auto-refresh behaviour — the tab is user-driven only
    }

    async function doLookup(playerId) {
        var result = document.getElementById('player-lookup-result');
        result.innerHTML = '<span class="text-secondary small font-monospace">Loading…</span>';

        var encoded = encodeURIComponent(playerId);
        var res = await Promise.allSettled([
            GisApi.get('/admin/favourites/players/' + encoded + '/wagers'),
            GisApi.get('/admin/favourites/players/' + encoded + '/groups')
        ]);

        var wagersOk = res[0].status === 'fulfilled';
        var groupsOk = res[1].status === 'fulfilled';

        if (!wagersOk && !groupsOk) {
            result.innerHTML =
                '<span class="text-danger small font-monospace">Player not found or error: ' +
                escHtml(res[0].reason ? res[0].reason.message : 'unknown error') + '</span>';
            return;
        }

        var wagers = { items: [], totalCount: 0 };
        var groups = { items: [], totalCount: 0 };
        try { if (wagersOk) wagers = JSON.parse(res[0].value); } catch (e) { /* ignore */ }
        try { if (groupsOk) groups = JSON.parse(res[1].value); } catch (e) { /* ignore */ }

        var rows = (wagers.items || []).map(function (w) {
            return '<tr>' +
                '<td class="font-monospace small">' + escHtml(w.wagerName || '—') + '</td>' +
                '<td class="font-monospace small text-info">' + escHtml(w.gameName || '—') + '</td>' +
                '<td class="font-monospace small text-secondary">' + escHtml(String(w.groupNumber || '—')) + '</td>' +
                '</tr>';
        }).join('');

        result.innerHTML =
            '<div class="arte-grid mb-3">' +
              '<div class="arte-key">playerId</div>' +
              '<div class="arte-val font-monospace text-success">' + escHtml(playerId) + '</div>' +
              '<div class="arte-key">wagers</div>' +
              '<div class="arte-val">' + wagers.totalCount + '</div>' +
              '<div class="arte-key">groups</div>' +
              '<div class="arte-val">' + groups.totalCount + '</div>' +
            '</div>' +
            (rows
                ? '<table class="arte-table">' +
                    '<thead><tr><th>Wager Name</th><th>Game</th><th>Group</th></tr></thead>' +
                    '<tbody>' + rows + '</tbody>' +
                  '</table>'
                : '<p class="text-secondary small">No favourite wagers for this player.</p>');
    }

    function escHtml(s) {
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    GisGui.registerSnapshot('player', 'Player Lookup', { render: render });
})();
