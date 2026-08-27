#!/bin/bash
#
# Build and install dev RPMs on a remote host
#
set -e
cd "$(dirname "$0")/.."

host=root@czhd9rigis01

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

./mvn.sh install -Dbuild.rpms

scp czh-favourites/czh-favourites-rpm/target/czh-czh-favourites-rpm-0.0.1-*.noarch.rpm \
    $host:/tmp/czh-czh-favourites-rpm.noarch.rpm
scp czh-favourites-admin/czh-favourites-admin-rpm/target/czh-czh-favourites-admin-rpm-0.0.1-*.noarch.rpm \
    $host:/tmp/czh-czh-favourites-admin-rpm.noarch.rpm

ssh $host '
rpm --erase czh-czh-favourites-rpm
rpm --erase czh-czh-favourites-admin-rpm
rpm --nodeps -ivh /tmp/czh-czh-favourites-rpm.noarch.rpm
rpm --nodeps -ivh /tmp/czh-czh-favourites-admin-rpm.noarch.rpm
'
