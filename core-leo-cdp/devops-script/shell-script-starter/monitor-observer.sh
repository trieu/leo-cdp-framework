#!/bin/bash

# Specify the process name and the URL endpoint to check
# You must replace with the actual URL endpoint
endpoint_url="https://datahub4uspa.leocdp.net/ping"  

# Send a GET request to the endpoint URL and check the response
response_code=$(curl -s -o /dev/null -w "%{http_code}" "$endpoint_url")

# Check the response code
if [ "$response_code" -eq 200 ]; then
  echo "The process is running."
else
  echo "The process is not running. Try to restart ..."
  sudo su -c "cd /build/leo-cdp-uspa; /build/leo-cdp-uspa/start-observer.sh" leocdp
fi