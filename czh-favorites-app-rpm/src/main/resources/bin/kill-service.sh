#!/bin/bash

app="czh-favorites"

pid_file=/var/run/czh-favorites/$app.pid

if [[ -f $pid_file ]]
then
  kill -9 $(cat $pid_file) && rm $pid_file
else
    echo "no pid file found"
fi
