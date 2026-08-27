# HLD

## Docs

Englash language artifacts in US English *.md

## Specs

JSON input in jamcrest --template *.req.js
JSON output in jamcrest --template *.resp.js
Process specs in executable bash

## Code choices

production code in Java25 spring-boot4 (intially, expect porting)
test code independent of implementation, (executable process specs) tests should survice production code platform changes
zero library dependencies outside sb4 (use ~/bin/sb4ver to find precices versions)
extensive unit testing, that are not expected to survive platform changes
local inteargation testing
local soak testing
local perf testing

## Proxies

Java25 spring-boot 4 initially
test scripts in human, agent readable, and executable bash code.

## State

nosql style JSON stores
prefer append only style,m but dont enforce it
single table with one colum for any data used in indexes
32bit flag bitmask per table
separate status tables for highly changable state data
JdbcTemplate with readable validatable SQL

## Monitoring

promethus format
limit to core metrics
health via prometheus 

## deployment

RPM deployments to /opt
commons start/stop/status scripts in /opt/xxx/bin
