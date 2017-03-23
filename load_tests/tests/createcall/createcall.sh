#!/bin/bash

AUTHTOKEN=$1
RESTCOMM_ADDRESS=$2
LOCAL_ADDRESS=$3
MAX_CALLS=$4
CPS=$5
SIPP_CLIENT=sip:1999@$LOCAL_ADDRESS

SLEEP_TIME=$(echo "scale=10;1/$CPS" | bc)

DATE=$(date +%F_%H_%M)

echo "Running Create Call load test, will create REST API calls to get $MAX_CALLS calls to Client $SIPP_CLIENT with call rate $CPS cps" > $RESULTS_DIR/createcall-$DATE.log
echo "Authtoken: $AUTHTOKEN, RESTCOMM_ADDRESS: $RESTCOMM_ADDRESS, SIPP_CLIENT: sip:1999@$LOCAL_ADDRESS, MAX_CALLS: $MAX_CALLS, CPS: $CPS, SLEEP_TIME: $SLEEP_TIME" >> $RESULTS_DIR/createcall-$DATE.log

CURL_COMMAND="curl --connect-timeout 1 -X POST  http://administrator%40company.com:$AUTHTOKEN@$RESTCOMM_ADDRESS:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Calls.json -d From=15126001502 -d To=sip%3A1999%40$LOCAL_ADDRESS -d Url=http://$RESTCOMM_ADDRESS:8080/restcomm/demos/hello-play.xml"
echo "Will use CURL Command: $CURL_COMMAND" >> $RESULTS_DIR/createcall-$DATE.log
#First REST API Request will take longer than the rest because Jersey loads all endpoints
# exec $CURL_COMMAND

echo "Sleeping for 10 sec before starting the test" >> $RESULTS_DIR/createcall-$DATE.log
sleep 10
echo "About to start test" >> $RESULTS_DIR/createcall-$DATE.log

x=1
while [ $x -le $MAX_CALLS ]
do
  echo "Will create $x call out of $MAX_CALLS to $SIPP_CLIENT" >> $RESULTS_DIR/createcall-$DATE.log
  eval $CURL_COMMAND
  x=$(( $x + 1 ))
  echo "sleeping for $SLEEP_TIME seconds" >> $RESULTS_DIR/createcall-$DATE.log
  sleep $SLEEP_TIME
done

SIPP_PID=$(pidof sipp)
echo
echo "Will wait for SIPP PID $SIPP_PID to finish"

while [ -e /proc/$SIPP_PID ]
do
    echo "Process: $SIPP_PID is still running...."
    sleep 20
done

echo "SIPP Finished. will terminate script"

echo "Everything set, will exit createcall.sh script" >> $RESULTS_DIR/createcall-$DATE.log

exit 0
