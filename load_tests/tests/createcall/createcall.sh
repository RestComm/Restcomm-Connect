#!/bin/bash

AUTHTOKEN=$1
RESTCOMM_ADDRESS=$2
SIPP_CLIENT=$3
MAX_CALLS=$4
CPS=$5

SLEEP_TIME=$(echo "scale=10;1/$CPS" | bc)

echo "Running Create Call load test, will create REST API calls to get $MAX_CALLS calls to Client $SIPP_CLIENT with call rate $CPS cps"
echo "Authtoken: $AUTHTOKEN, RESTCOMM_ADDRESS: $RESTCOMM_ADDRESS, SIPP_CLIENT: $SIPP_CLIENT, MAX_CALLS: $MAX_CALLS, CPS: $CPS, SLEEP_TIME: $SLEEP_TIME"

CURL_COMMAND="curl --connect-timeout 1 -X POST  http://administrator%40company.com:$AUTHTOKEN@$RESTCOMM_ADDRESS:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Calls.json --data-urlencode \"From=+15126001502\" --data-urlencode \"To=$SIPP_CLIENT\" --data-urlencode \"Url=http://$RESTCOMM_ADDRESS:8080/restcomm/demos/hello-play.xml\""

#First REST API Request will take longer than the rest because Jersey loads all endpoints
exec $CURL_COMMAND

x=2
while [ $x -le $MAX_CALLS ]
do
  echo "Will create $x call to $SIPP_CLIENT"
  exec $CURL_COMMAND
  x=$(( $x + 1 ))
  echo "sleeping for $SLEEP_TIME seconds"
  sleep $SLEEP_TIME
done
