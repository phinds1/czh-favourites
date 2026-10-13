#!/bin/bash
#
# Polite stop with SIGTERM
#

pid_file=/var/run/czh/favorites/.pid

if [[ -f $pid_file ]]
then
  kill -15 $(cat $pid_file)
else
    echo "no pid file found"
fi
