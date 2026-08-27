#!/bin/bash
#
# Build and install dev RPMs locally
#
set -e
cd "$(dirname "$0")/.."

sudo rpm --erase czh-favourites-rpm       2>/dev/null || true
sudo rpm --nodeps -ivh czh-favourites-rpm/czh-favourites-rpm/target/czh-favourites-rpm-0.0.1-*.noarch.rpm || true
