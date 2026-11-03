#!/bin/bash
#
# Build and install dev RPMs locally
#
set -e
cd "$(dirname "$0")/.."

./mvn.sh clean install -Drpm=true

sudo rpm --erase czh-favorites-app-rpm       2>/dev/null || true
sudo rpm --nodeps -ivh czh-favorites-app-rpm/target/czh-favorites-app-rpm-0.0.1-*.noarch.rpm
sudo rpm -q czh-favorites-app-rpm
