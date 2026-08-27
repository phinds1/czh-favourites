# source this
# test-functions.sh
#
# Common bash helpers sourced by every test script.
#

# Guard against double-sourcing
[[ -n "${_TEST_FUNCTIONS_LOADED:-}" ]] && return 0
_TEST_FUNCTIONS_LOADED=1

# -----------------------------------------------------------------------------
# test_sleep <seconds>
#
# Drop-in replacement for `sleep` in integration tests.
# Sleeps are multiplied by TEST_SLEEP_FACTOR to compensate for slow cloud VMs.
#
# Detection (first match wins):
#   1. TEST_SLEEP_FACTOR env var set explicitly  (any value)
#   2. TF_BUILD env var set                      (Azure DevOps pipeline)
#   3. DMI chassis asset tag matches Azure GUID  (Azure VM running locally)
#   4. Default: factor = 1  (developer laptop — no change)
#
# Override manually:
#   TEST_SLEEP_FACTOR=3 ./test/integration/run-all.sh
# -----------------------------------------------------------------------------
TEST_SLEEP_FACTOR=1

detect_sleep_factor() {
    # Azure DevOps sets TF_BUILD=True in all pipeline jobs
    if [[ -n "${TF_BUILD:-}" ]]; then
        TEST_SLEEP_FACTOR=3
        return
    fi
    # Azure VMs have a recognisable DMI chassis asset tag (fast file read)
    local dmi_tag
    dmi_tag=$(cat /sys/class/dmi/id/chassis_asset_tag 2>/dev/null || true)
    if [[ $dmi_tag == "7783-7084-3265-9085-8269-3286-77" ]]
    then
        TEST_SLEEP_FACTOR=3
        return
    fi
    TEST_SLEEP_FACTOR=1
}

detect_sleep_factor

test_sleep() {
    local secs=$1
    # use awk for floating-point multiplication
    sleep $(awk "BEGIN { printf \"%.3f\", $secs * $TEST_SLEEP_FACTOR }")
}

test_tmp=$(mktemp -d /tmp/$test_name-test-XXXXXX)

# -----------------------------------------------------------------------------
# Standard variables
# -----------------------------------------------------------------------------
#nginx_bin=$project_root/src/nginx/install/sbin/nginx
#nginx_conf_share=$project_root/src/nginx/install/conf

# -----------------------------------------------------------------------------
# Test counters - initialized here for all tests
# -----------------------------------------------------------------------------
PASSED=0
FAILED=0
FAILURES=0

#
# Colour helpers  (silent no-ops when stdout is not a terminal)
#
BOLD=$(tput bold   2>/dev/null || true)
GREEN=$(tput setaf 2 2>/dev/null || true)
RED=$(tput setaf 1 2>/dev/null || true)
YELLOW=$(tput setaf 3 2>/dev/null || true)
CYAN=$(tput setaf 6 2>/dev/null || true)
NC=$(tput sgr0    2>/dev/null || true)

#
# Output primitives
#
pass()   { echo "  ${GREEN}✓${NC}  $*"; PASSED=$((PASSED+1));   }
fail()   { echo "  ${RED}✗${NC}  $*";   FAILURES=$((FAILURES+1)); FAILED=$((FAILED+1)); }
header() { echo ""; echo "${BOLD}$*${NC}"; }
info()   { echo "${CYAN}▶${NC}  $*"; }
warn()   { echo "${YELLOW}!${NC}  $*"; }

assert_equals() {
    local label="$3" got="$1" want="$2"
    if [[ $got == $want ]]
    then
        pass "$label: '$want'"
    else
        fail "$label: expected '$want', got '$got'"
    fi
}

assert_not_equals() {
    local label="$3" got="$1" want="$2"
    if [[ $got != $want ]]
    then
        pass "$label: '$want'"
    else
        fail "$label: expected '$want' != '$got'"
    fi
}

assert_contains() {
    local label="$3" haystack="$1" needle="$2"
    if [[ $haystack == *$needle* ]]
    then
        pass "$label: contains '$needle'"
    else
        fail "$label: expected to contain '$needle', got '$haystack'"
    fi
}

assert_exists() {
    local thing="$1" label="$2"
    if [[ -n "$thing" ]]
    then
        pass "$label: exists '$thing'"
    else
        fail "$label: expected to exist but was empty"
    fi
}
# -----------------------------------------------------------------------------
# assert_json <label> <json_string> <dotted.path> <expected_value>
#
# Navigates a JSON string via a dot-separated key path and compares the
# stringified leaf value against the expected string.
#
# Examples:
#   assert_json "T-01 path"    "$body" "path"                 "/v2/addr"
#   assert_json "T-07 header"  "$body" "headers.x-player-id"  "PL-42"
# -----------------------------------------------------------------------------
assert_json() {
    local label="$1" json="$2" path="$3" expected="$4"
    local actual
    actual=$(python3 -c "
import sys, json
d = json.loads(sys.argv[1])
for p in sys.argv[2].split('.'):
    d = d[p]
print(str(d))
" "$json" $path 2>/dev/null || echo "__ERR__")
    if [[ $actual == $expected ]]
    then
        pass "$label: $path=$expected"
    else
        fail "$label: $path expected='$expected' got='$actual'"
    fi
}

# -----------------------------------------------------------------------------
# assert_json_field <label> <dotted["key"]> <expected> <json_string>
#
# (e.g. "['server']", "['player']['tier']") and the json is passed via stdin.
# -----------------------------------------------------------------------------
assert_json_field() {
    local label="$1" field="$2" expected="$3" body="$4"
    local actual
    actual=$(echo "$body" | python3 -c \
        "import sys,json; d=json.load(sys.stdin); print(d$field)" \
        2>/dev/null || echo "__PARSE_ERROR__")
    if [[ $actual == $expected ]]
    then
        pass "$label: $field = $actual"
    else
        fail "$label: $field expected '$expected', got '$actual'"
    fi
}

# -----------------------------------------------------------------------------
# assert_log_contains <label> <grep_pattern> <logfile>
# assert_log_absent   <label> <grep_pattern> <logfile>
# -----------------------------------------------------------------------------
assert_log_contains() {
    local label=$1 pattern=$2 logfile=$3
    if grep -q "$pattern" $logfile 2>/dev/null
    then
        pass "$label: log contains '$pattern'"
    else
        fail "$label: log does NOT contain '$pattern'"
    fi
}

assert_log_absent() {
    local label=$1 pattern=$2 logfile=$3
    if ! grep -q "$pattern" $logfile 2>/dev/null
    then
        pass "$label: log is clean (no '$pattern')"
    else
        fail "$label: log unexpectedly contains '$pattern'"
    fi
}

# Alias: same semantics, more descriptive name at call-site
assert_log_not_contains() {
    assert_log_absent "$@";
}

cleanup_pid() {
    if [[ $# -eq 0 ]]
    then
        echo "No pid provided."
        return
    elif [[ -z $1 ]]
    then
        echo "No pid provided."
        return
    fi

    pid=$1
    if [[ -n $pid ]]
    then
        kill $pid 2>/dev/null || true
        wait $pid 2>/dev/null || true
    fi
}

core_dumps() {
  if [[ $(sysctl kernel.core_pattern) =~ .*/tmp/coredumps/core\.p ]]
  then
    ulimit -c unlimited
    echo "core dumps at /tmp/coredumps/core.*"
  else
    echo "for core dumps run sudo sysctl -w  kernel.core_pattern=/tmp/coredumps/core.%p"
  fi
}

cleanup_temp_dir() {
    if [[ -d $test_tmp ]]
    then
        rm -rf $test_tmp
    fi
}

skip()   {
  echo "  ${YELLOW}SKIP${NC} $*"
}

# -----------------------------------------------------------------------------
# run_long_tests [case-name]
#
# Returns 0 (true)  when LONG_TESTS=1 — the caller should run the test.
# Returns 1 (false) when LONG_TESTS is unset or 0 — prints a SKIP line.
#
# Whole-script skip (put at the top of a dedicated long-running script):
#   run_long_tests || exit 0
#
# Single-case skip inside a script:
#   if run_long_tests "T-9: 30-second backoff ramp"
#   then
#       ...long test body...
#   fi
# -----------------------------------------------------------------------------
run_long_tests() {
    local name=${1:-$(basename "$0")}
    if [[ "${LONG_TESTS:-0}" = "1" ]]
    then
        return 0
    fi
    skip "$name (set LONG_TESTS=1 to run)"
    return 1
}

# -----------------------------------------------------------------------------
# test_summary
#
# Prints the final pass/fail banner and exits with code 1 if any failures were
# recorded.  Call at the end of every test script.
# -----------------------------------------------------------------------------
test_summary() {
    local failures=${FAILURES:-${FAILED:-0}}
    local passed=${PASSED:-0}
    local total=$(( passed + failures ))
    echo ""
    echo "═══════════════════════════════════════════════════"
    if [[ $failures -eq 0 ]]
    then
        echo "${GREEN}${BOLD} ALL TESTS PASSED${NC} ($passed assertions)"
    else
        echo "${RED}${BOLD} $failures FAILED${NC} / $total total"
        echo "FAIL - $test_name" >> /tmp/results.log
    fi
    echo "═══════════════════════════════════════════════════"
    echo ""
    [[ $failures -eq 0 ]]
}
