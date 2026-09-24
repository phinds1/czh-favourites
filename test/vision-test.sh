#!/bin/bash
# vision-test.sh — harness that proves the Vision GUI works behind the nginx reverse proxy.
#
# Builds the app module test classpath if missing, starts the Java backend on the fixed ports
# (app 9290 / mgmt 9291), starts nginx with the demo fixture config (listener 8080), runs URL
# assertions through the proxy, and tears everything down on exit. --hold keeps the servers up
# for manual browser testing.
#
# Usage: test/vision-test.sh [--hold] [--port PORT] [--nginx-bin PATH]
#
# Sources test/test-functions.sh for shared helpers (test_sleep, pass/fail, assert_*, test_summary).

set -euo pipefail

# --- args -----------------------------------------------------------------------
hold=0
port=8080
nginx_bin_override=""
while [[ $# -gt 0 ]]
do
    case "$1" in
        --hold) hold=1; shift ;;
        --port) port=$2; shift 2 ;;
        --nginx-bin) nginx_bin_override=$2; shift 2 ;;
        -h|--help)
            echo "usage: $0 [--hold] [--port PORT] [--nginx-bin PATH]"
            exit 0
            ;;
        *) echo "unknown arg: $1" >&2; exit 2 ;;
    esac
done

# --- locate the script's project root (so it works from any cwd) -----------------
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd "$script_dir/.." && pwd)
cd "$project_root"

# test-functions.sh uses $test_name in its mktemp; set it before sourcing.
test_name=vision-test

# shellcheck source=test/test-functions.sh
source "$script_dir/test-functions.sh"

fixture_dir="$project_root/test/fixtures/vision-nginx"
default_nginx_bin=/java/czh/czh-nginx-gateway/src/nginx/install/sbin/nginx
nginx_bin=${nginx_bin_override:-${NGINX_BIN:-$default_nginx_bin}}
app_module="$project_root/czh-favorites-app"
test_classes="$app_module/target/test-classes"
classpath_file="$app_module/target/vision-classpath.txt"
harness_main=cz.bsl.czh.favourites.test.VisionHarnessMain
mvn_profiles=${VISION_MVN_PROFILES:-github-ci}
# ./mvn.sh points at a non-existent maven dir; use the real one directly.
export JAVA_HOME=/opt/jdk-25
export PATH=$PATH:/opt/apache-maven-3.9.6/bin:$JAVA_HOME/bin

# Favourites' fixed prod ports (application.yml): app 9290, management 9291.
APP_PORT=9290
MGMT_PORT=9291

# --- state for the teardown trap ------------------------------------------------
java_pid=""
nginx_pid=""
resolved_conf=""

cleanup() {
    echo "-- tearing down"
    if [[ -n "$nginx_pid" ]]
    then
        kill "$nginx_pid" 2>/dev/null || true
        wait "$nginx_pid" 2>/dev/null || true
    fi
    if [[ -n "$java_pid" ]]
    then
        kill "$java_pid" 2>/dev/null || true
        wait "$java_pid" 2>/dev/null || true
    fi
    # graceful backup for nginx if the conf is resolvable
    if [[ -n "$resolved_conf" && -x "$nginx_bin" ]]
    then
        "$nginx_bin" -s stop -c "$resolved_conf" 2>/dev/null || true
    fi
    if [[ -n "${test_tmp:-}" && -d "$test_tmp" ]]
    then
        rm -rf "$test_tmp"
    fi
}
trap cleanup EXIT INT TERM

# --- preconditions --------------------------------------------------------------
if [[ ! -x "$nginx_bin" ]]
then
    echo "nginx binary not found at: $nginx_bin" >&2
    echo "pass --nginx-bin PATH or set NGINX_BIN (default: $default_nginx_bin)" >&2
    exit 1
fi
if [[ ! -d "$fixture_dir" ]]
then
    echo "nginx fixture config not found: $fixture_dir" >&2
    exit 1
fi

# --- build the app module test classpath ----------------------------------------
# The app cannot run as a bare java -jar: production uses DB2, while the harness needs the test
# classpath (HSQLDB + the test schema). VisionHarnessMain boots FavouritesApplication on the fixed
# ports with the test profile; the schema is loaded via spring.sql.init (set in the harness main).
if [[ ! -d "$test_classes" || ! -f "$classpath_file" || ! -d "$app_module/target/classes" ]]
then
    header "Building the app test classpath (first run is slow ~30s)"
    # -pl takes the reactor module id (czh-favorites-app). -am builds its dependency (api) too.
    # test-compile produces both target/classes (main) and target/test-classes (harness + tests).
    mvn -pl czh-favorites-app -am test-compile -P "$mvn_profiles" -DskipTests
    # build-classpath resolves -Dmdep.outputFile relative to the reactor root, so pass an absolute
    # path (the relative form silently writes nowhere when run from the project root with -pl).
    mvn -pl czh-favorites-app dependency:build-classpath -P "$mvn_profiles" \
        -Dmdep.outputFile="$project_root/czh-favorites-app/target/vision-classpath.txt"
fi
# The harness needs: test-classes (VisionHarnessMain) + the app's main classes + all dependencies.
# dependency:build-classpath only emits external deps, so prepend the app's target/classes.
main_classes="$app_module/target/classes"
cp_string=$test_classes:$main_classes:$(cat "$classpath_file")

# --- start the Java backend on 9290/9291 -----------------------------------------
header "Starting Java backend (app $APP_PORT / mgmt $MGMT_PORT, HSQLDB)"
which java
java -version
java -cp "$cp_string" $harness_main \
    --server.port=$APP_PORT --management.server.port=$MGMT_PORT --spring.profiles.active=test \
    >"$test_tmp/java.log" 2>&1 &
java_pid=$!

info "Waiting for the app to come up on $MGMT_PORT (timeout 60s)"
deadline=$(( $(date +%s) + 60 ))
up=0
while [[ $(date +%s) -lt $deadline ]]
do
    if curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:$MGMT_PORT/actuator/health 2>/dev/null | grep -q 200
    then
        up=1; break
    fi
    if ! kill -0 "$java_pid" 2>/dev/null
    then
        echo "Java backend exited early — log:" >&2
        cat "$test_tmp/java.log" >&2
        exit 1
    fi
    test_sleep 1
done
if [[ $up -ne 1 ]]
then
    echo "Java backend did not come up on $MGMT_PORT within 60s — log tail:" >&2
    tail -30 "$test_tmp/java.log" >&2
    exit 1
fi
pass "Java backend up (app $APP_PORT, mgmt $MGMT_PORT)"

# --- seed a favourite wager so the GUI admin lookup + Operations tab have data ---
# POST a group then a wager for a known player via the APP port (direct, X-Player-Id header).
seed_player=vision-seed-player
info "Seeding a favourite wager for $seed_player"
seed_group_body='{"groupNumber":"5","groupName":"Vision Seed Group"}'
curl -s -o /dev/null -w '%{http_code}' -X POST "http://127.0.0.1:$APP_PORT/favourites/groups" \
    -H 'Content-Type: application/json' -H "X-Player-Id: $seed_player" \
    -d "$seed_group_body" >/dev/null
seed_wager_body='{"groupNumber":"5","wagerName":"Vision Seed Wager","wager":{"gameName":"LOTTO","stake":1,"price":1,"duration":1,"serialNumber":"VIS1","boards":[[1,2,3]]}}'
curl -s -o /dev/null -w '%{http_code}' -X POST "http://127.0.0.1:$APP_PORT/favourites/wagers" \
    -H 'Content-Type: application/json' -H "X-Player-Id: $seed_player" \
    -d "$seed_wager_body" >/dev/null
pass "Seeded favourite wager for $seed_player"

# --- resolve the nginx config into a temp copy of the fixture --------------------
# envsubst is not always installed; sed replaces only the fixture's own placeholders. The $host /
# $scheme / $proxy_* nginx vars use a leading '$' that sed does not touch (they have no ${FIXTURE}
# / ${PORT} shape), so they stay intact for nginx to expand at request time.
header "Resolving nginx config (listener $port)"
resolved_root="$test_tmp/nginx"
cp -r "$fixture_dir" "$resolved_root"
mkdir -p "$resolved_root/logs" "$resolved_root/temp" "$resolved_root/run"
for f in "$resolved_root/nginx.conf" "$resolved_root/conf.d/vision-demo.conf"
do
    sed -e "s|\${FIXTURE}|$resolved_root|g" -e "s|\${PORT}|$port|g" "$f" > "$f.tmp" && mv "$f.tmp" "$f"
done
resolved_conf="$resolved_root/nginx.conf"

"$nginx_bin" -p "$resolved_root/" -t -c "$resolved_conf"
info "Starting nginx on $port"
"$nginx_bin" -p "$resolved_root/" -c "$resolved_conf" &
nginx_pid=$!

# wait for nginx to listen
deadline=$(( $(date +%s) + 15 ))
nginx_up=0
while [[ $(date +%s) -lt $deadline ]]
do
    if curl -s -o /dev/null http://127.0.0.1:$port/ 2>/dev/null
    then
        nginx_up=1; break
    fi
    test_sleep 0.5
done
if [[ $nginx_up -ne 1 ]]
then
    echo "nginx did not come up on $port within 15s — error log:" >&2
    cat "$resolved_root/logs/error.log" >&2
    exit 1
fi
pass "nginx up on $port"

# --- URL assertions (the core proof) --------------------------------------------
base_url=http://127.0.0.1:$port
header "URL assertions through the proxy"

assert_status() {
    local url=$1 want=$2 label=$3
    local got
    got=$(curl -s ${curl_headers:-} -o /dev/null -w '%{http_code}' "$url")
    if [[ $got == $want ]]
    then
        pass "$label: HTTP $want"
    else
        fail "$label: expected HTTP $want, got $got ($url)"
        return 1
    fi
}

assert_body_contains() {
    local url=$1 needle=$2 label=$3
    local body
    body=$(curl -s ${curl_headers:-} "$url")
    if echo "$body" | grep -qF "$needle"
    then
        pass "$label: body contains '$needle'"
    else
        fail "$label: body missing '$needle'"
        echo "  -- body --"; echo "$body" | head -20 | sed 's/^/  /' >&2
        return 1
    fi
}

assert_body_absent() {
    local url=$1 needle=$2 label=$3
    local body
    body=$(curl -s ${curl_headers:-} "$url")
    if echo "$body" | grep -qF "$needle"
    then
        fail "$label: body should NOT contain '$needle'"
        return 1
    else
        pass "$label: body absent '$needle'"
    fi
}

assert_header_absent() {
    local url=$1 header_name=$2 label=$3
    local headers
    headers=$(curl -s -D - -o /dev/null "$url")
    if echo "$headers" | grep -iq "^$header_name:"
    then
        fail "$label: $header_name present (should be absent for frame-loadability)"
        return 1
    else
        pass "$label: $header_name absent"
    fi
}

# 1. GUI index.html under the management context: meta rewritten, frame-loadable.
assert_status $base_url/favourites-mgmt/gui/index.html 200 "GET /favourites-mgmt/gui/index.html"
assert_body_contains $base_url/favourites-mgmt/gui/index.html '<meta name="vision-base" content="/favourites-mgmt">' "meta injection"
assert_body_contains $base_url/favourites-mgmt/gui/index.html '="/favourites-mgmt/gui/js/api.js"' "asset path rebased under context root"
assert_header_absent $base_url/favourites-mgmt/gui/index.html X-Frame-Options "no X-Frame-Options (frame-loadable)"

# 2. GUI JS asset served under the context root (proves the asset path resolves through nginx).
#    JS is NOT rewritten (resolveUrl reads the meta at runtime) — assert the base is absent in it.
assert_status $base_url/favourites-mgmt/gui/js/api.js 200 "GET /favourites-mgmt/gui/js/api.js"
assert_body_contains $base_url/favourites-mgmt/gui/js/api.js resolveUrl "api.js served under context root"
assert_body_absent $base_url/favourites-mgmt/gui/js/api.js "/favourites-mgmt/gui/" "api.js not rewritten (JS reads meta at runtime)"

# 3. Bare context root under the GUI's context: filter serves the index directly (rewritten).
assert_status $base_url/favourites-mgmt/ 200 "GET /favourites-mgmt/ (root, direct-served)"
assert_body_contains $base_url/favourites-mgmt/ '<meta name="vision-base" content="/favourites-mgmt">' "root index meta injected"

# 4. Admin API through the GUI's context (/favourites-mgmt/admin -> app port 9290). The admin
#    wagers endpoint needs no X-Player-Id header (playerId is a path param); it returns JSON with
#    items + totalCount. The GUI fetches /admin through its own context, which routes to the app port.
assert_status "$base_url/favourites-mgmt/admin/favourites/players/$seed_player/wagers" 200 "GET /favourites-mgmt/admin wagers (GUI's admin context)"
assert_body_contains "$base_url/favourites-mgmt/admin/favourites/players/$seed_player/wagers" '"totalCount"' "GUI-context admin wagers JSON"
# The bare /favourites/ context also routes to the app port (direct/cron access).
assert_status "$base_url/favourites/admin/favourites/players/$seed_player/groups" 200 "GET /favourites/admin groups (direct app context)"
assert_body_contains "$base_url/favourites/admin/favourites/players/$seed_player/groups" '"totalCount"' "direct-context admin groups JSON"

# 5. Actuator health through the management context.
assert_status $base_url/favourites-mgmt/actuator/health 200 "GET /favourites-mgmt/actuator/health"
assert_body_contains $base_url/favourites-mgmt/actuator/health '"status":"UP"' "actuator health UP"

# 6. Actuator prometheus through the management context (the Server/Operations tabs' source).
assert_status $base_url/favourites-mgmt/actuator/prometheus 200 "GET /favourites-mgmt/actuator/prometheus"
assert_body_contains $base_url/favourites-mgmt/actuator/prometheus 'process_uptime_seconds' "prometheus has uptime gauge"

# --- nginx crash check (no worker exits in error.log) ---------------------------
header "Crash check — no worker exits in nginx error.log"
crash_patterns=(
    "worker process.*exited on signal"
    "worker process.*exited with fatal"
    "segfault"
    "SIGSEGV"
    "SIGBUS"
    "SIGABRT"
)
crashes=0
for pattern in "${crash_patterns[@]}"
do
    if grep -qiE "$pattern" "$resolved_root/logs/error.log" 2>/dev/null
    then
        fail "crash detected: '$pattern'"
        crashes=$(( crashes + 1 ))
    fi
done
[[ $crashes -eq 0 ]] && pass "no nginx worker crashes"

# --- hold for manual browser testing --------------------------------------------
if [[ $hold -eq 1 ]]
then
    echo ""
    echo "=============================================================="
    echo "Vision GUI is up behind nginx. Open in a browser:"
    echo "  GUI (management context): http://localhost:$port/favourites-mgmt/gui/index.html"
    echo "  Admin wagers API:         http://localhost:$port/favourites/admin/favourites/players/$seed_player/wagers"
    echo "  Actuator health:          http://localhost:$port/favourites-mgmt/actuator/health"
    echo "  Press ENTER here to shut everything down."
    echo "=============================================================="
    read -r _ </dev/tty || true
fi

test_summary
