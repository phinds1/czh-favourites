---
name: jira
description: Implement jira requirements
allowed-tools: java, bash
---

Usage: /jira <issue-id>

Behavior:
- This skill requires exactly one argument: a Jira issue ID.

<objective>
Automate fixing bugs or features raised in Jira issues.
</objective>

<process>

Create a feature branch to work on `e.,g.` `git checkout -b feature/jira-$1`
run `bin/jiracontext-md.sh <issue id>` this creates a file `ai-contexts-tmp/$1-issue.md` with the issue details.

Read the context file to understand the issue.

For bug fixes apply /bug-fixing, to ensure test coverage of the fix.

For new features ensure test coverage in JUnit tests close to the code and if required apply /jamcrest

Check in to the feature branch. (do NOT update any other branches or push)

Human user with verify and create a github pull request from the feature branch for review.

<process>
