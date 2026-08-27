#!/bin/bash
#
# Build, merge JaCoCo coverage from all modules, and emit a CSV report.
# Derived from /java/czh/czh-money/bin/coverage.sh.
#
cd $(dirname $0)/..
set -e

./mvn.sh clean install

# Fetch JaCoCo CLI fat-jar via Maven (version is taken from root pom property)
JACOCO_VERSION=$(./mvn.sh -q -Dexec.executable=echo -Dexec.args='${jacoco-maven-plugin.version}' --non-recursive exec:exec 2>/dev/null)
JACOCO_CLI="target/dependency/org.jacoco.cli-${JACOCO_VERSION}-nodeps.jar"

if [ ! -f "$JACOCO_CLI" ]; then
    echo "Fetching JaCoCo CLI ${JACOCO_VERSION}..."
    ./mvn.sh --non-recursive dependency:copy \
        -Dartifact="org.jacoco:org.jacoco.cli:${JACOCO_VERSION}:jar:nodeps" \
        -q
fi

echo "Merging coverage data..."
find . -name "jacoco.exec" -type f | xargs java -jar "$JACOCO_CLI" merge --destfile target/jacoco-aggregate.exec

echo "Generating CSV report..."
# Only include classfile dirs that exist (api module is empty at SP1)
CLASSFILES=""
SOURCEFILES=""
for module in czh-favorites-api czh-favorites-app; do
    if [ -d "${module}/target/classes" ]; then
        CLASSFILES="$CLASSFILES --classfiles ${module}/target/classes"
        SOURCEFILES="$SOURCEFILES --sourcefiles ${module}/src/main/java"
    fi
done
java -jar "$JACOCO_CLI" report target/jacoco-aggregate.exec \
  $CLASSFILES \
  $SOURCEFILES \
  --csv target/jacoco-report.csv

echo "Report generated at: target/jacoco-report.csv"
