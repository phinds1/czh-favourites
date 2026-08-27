#!/bin/bash
#
# Apply czh-favorites db2delta migrations to a DB2 instance.
#
# Usage: run-delta.sh [host[:port]/db] [user] [password]
# Defaults match the czh-favorites application.yml datasource (PDDB / gtkinst1).
#
# db2delta CLI (verified via `db2delta help` on a deploy host):
#   db2delta apply -d host[:port]/db -u user -p password <file|dir>...
# A missing -p prompts interactively; set DB2_PASSWORD in a ./database.env to avoid the prompt.
#
set -euo pipefail

cd "$(dirname "$0")"/..

DB_DSN="${1:-vip.pddb/PDDB}"
DB_USER="${2:-gtkinst1}"
DB_PASS="${3:-gtkinst1}"

# -x = script mode (minimal output, strict exit codes); the deploy job parses exit codes.
# Apply every .sql in the structure dir; db2delta skips changesets already recorded in its
# changelog table, so re-running is idempotent.
exec /usr/bin/db2delta -x apply -d "${DB_DSN}" -u "${DB_USER}" -p "${DB_PASS}" structure
