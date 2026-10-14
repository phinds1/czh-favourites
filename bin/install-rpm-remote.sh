#!/bin/bash
#
# Build and install dev RPMs on a remote host
#
set -e
cd "$(dirname "$0")/.."

host=root@czhrigisrhel9

case "$1" in
  -h|--help|\?|help)
    echo "
Usage: $0 [-h|-H ...]
Options:
  -h, --help, ?, help   Show this help message
  -H, --host            host to deploy on e.g. root@czhd9rigis01
"
    exit 0
    ;;
  -H|--host)
    shift
    host=$1
    ;;
esac

echo "installing to $host"

./mvn.sh clean install -Drpm=true

scp czh-favorites-app-rpm/target/czh-favorites-app-rpm-0.0.1-*.noarch.rpm $host:/var/tmp/czh-favorites-app-rpm.noarch.rpm
scp czh-favorites-db-rpm/target/czh-favorites-db-rpm-0.0.1-*.noarch.rpm $host:/var/tmp/czh-favorites-db-rpm.noarch.rpm

ssh $host '
rpm --erase czh-favorites-app-rpm
rpm --erase czh-favorites-db-rpm
rpm --nodeps -ivh /var/tmp/czh-favorites-app-rpm.noarch.rpm
rpm --nodeps -ivh /var/tmp/czh-favorites-db-rpm.noarch.rpm
'
