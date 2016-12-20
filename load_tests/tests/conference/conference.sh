#!/bin/bash
##
## Descript+ion: Restcomm performance test script for Hello-Play application.
## Author     : George Vagenas
#

SIMULTANEOUS_CALLS_WARMUP=40
MAXIMUM_CALLS_WARMUP=100
CALL_RATE_WARMUP=10

usage() {
  echo 'Script usage: ./conference.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE'
  echo "Example: ./conference.sh 192.168.1.11 192.168.1.12 100 10000 30"
}

if [[ -z $RESTCOMM_ADDRESS ]]; then
    RESTCOMM_ADDRESS=127.0.0.1
fi

if [[ -z $LOCAL_ADDRESS ]]; then
  LOCAL_ADDRESS=127.0.0.1
fi

if [[ -z $SIMULTANEOUS_CALLS ]]; then
  echo "Error you need to provide SIMULTANEOUS CALLS"
  usage
  exit 1
fi

if [[ -z $MAXIMUM_CALLS ]]; then
  echo "Error you need to provide MAXIMUM CALLS"
  usage
  exit 1
fi

if [[ -z $CALL_RATE ]]; then
  echo "Error you need to provide CALL RATE"
  usage
  exit 1
fi

DATE=$(date +%F_%H_%M)

echo "Restcomm IP Address: $RESTCOMM_ADDRESS - Local IP Address: $LOCAL_ADDRESS"

if [[ "$WARMUP" == "true" ]]; then
  echo "Warm up, SIMULTANEOUS_CALLS_WARMUP: $SIMULTANEOUS_CALLS_WARMUP, MAXIMUM_CALLS_WARMUP: $MAXIMUM_CALLS_WARMUP, CALL_RATE_WARMUP: $CALL_RATE_WARMUP"
  sleep 3
  $SIPP_EXECUTABLE -sf $CURRENT_FOLDER/tests/conference/conference-sipp.xml -s 2222 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS -l $SIMULTANEOUS_CALLS_WARMUP -m $MAXIMUM_CALLS_WARMUP -r $CALL_RATE_WARMUP -recv_timeout 10000 -t un -nr
  echo "Warmup finished"
  sleep 2m
fi

echo "About to launch rocket... SIMULTANEOUS_CALLS: $SIMULTANEOUS_CALLS, MAXIMUM_CALLS: $MAXIMUM_CALLS, CALL_RATE: $CALL_RATE"
sleep 3
$SIPP_EXECUTABLE -sf $CURRENT_FOLDER/tests/conference/conference-sipp.xml -s 2222 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS -l $SIMULTANEOUS_CALLS -m $MAXIMUM_CALLS -r $CALL_RATE -recv_timeout 10000 -t un -nr -fd 1 -trace_stat -stf $RESULTS_DIR/conference-$DATE.csv -trace_screen -screen_file $RESULTS_DIR/conference-$DATE-screens.log
echo $?




#!/bin/bash
# RESTCOMM_ADDRESS=127.0.0.1
# LOCAL_ADDRESS=127.0.0.1
#
# ulimit -n 500000
#
# ./sipp/sipp -sf ./conference.xml -s +7777 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 100 -m 150 -r 30 -trace_screen -trace_err -recv_timeout 10000 -t un -nr
