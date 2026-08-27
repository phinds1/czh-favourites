#!/bin/bash

# Print the running app version from the actuator info endpoint.
curl -s -H accept:text/plain http://localhost:9291/actuator/info 2>/dev/null || \
  echo "Error: Failed to retrieve version information from the server." >&2
