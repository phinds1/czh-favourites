#!/bin/bash
#
#  run the command that runs in github workflows,  (builds wihtout IBM deps)
#

export JAVA_HOME=/opt/jdk-25
export MAVEN_HOME=/opt/maven-3.9.6

export PATH=$PATH:$MAVEN_HOME/bin:$JAVA_HOME/bin

mvn -B -s .github/maven-settings.xml -P 'github-ci,!db2' verify --no-transfer-progress
