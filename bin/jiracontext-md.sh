#!/bin/bash
#
# get a jira issue in AI friendly format
#
set -e
cd $(dirname $0)/..


if [ -z $JIRA_USERNAME ] || [ -z $JIRA_PASSWORD ]
then
  echo "Please set JIRA_USERNAME and JIRA_PASSWORD environment variables"
  exit 2
fi

if [ -z $1 ]
then
  echo "Usage: $0 <JIRA-ISSUE>"
  exit 2
fi

tool_jar=tools/jiracontext-exporter/target/jira-context-exporter-1.0.0.jar
if [ ! -f $tool_jar ]
then
  (
    set -e
    cd tools/jiracontext-exporter
    mvn package
  )
fi

mkdir -p ai-output/
java -jar $tool_jar -i $1 - > ai-output/$1-issue.md
