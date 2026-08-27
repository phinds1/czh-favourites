#!/bin/bash

export JAVA_HOME=/opt/jdk-25
export MAVEN_HOME=/opt/maven-3.9.6

export PATH=$PATH:$MAVEN_HOME/bin:$JAVA_HOME/bin

mvn "$@"
