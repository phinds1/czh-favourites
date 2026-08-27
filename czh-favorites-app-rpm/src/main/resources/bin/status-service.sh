#!/bin/bash

app="czh-favorites"

pid_file=/var/run/czh-favorites/$app.pid

if [[ -f $pid_file ]]
then
  pid=$(cat $pid_file)
  if kill -0 "$pid" 2>/dev/null; then
      echo "running"
      if [[ "$1" == "-v" || "$1" == "--verbose" ]]
      then
        tail -100 /var/log/$app/spring.log
      elif [[ "$1" == "-f" || "$1" == "--follow" ]]
      then
        tail -f /var/log/$app/spring.log
      fi
  else
      echo "process is not running ($pid)"
  fi
else
    echo "no pid file found"
fi
