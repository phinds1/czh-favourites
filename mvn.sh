#!/bin/bash

export JAVA_HOME=/opt/jdk-25
export MAVEN_HOME=/opt/maven-3.9.6
export MAVEN_OPTS="-Dmaven.repo.local=/home/teknopaul/.m2-25/repo"

export PATH=$PATH:$MAVEN_HOME/bin:$JAVA_HOME/bin

mvn -Dmaven.repo.local=/home/teknopaul/.m2-25/repo "$@"
