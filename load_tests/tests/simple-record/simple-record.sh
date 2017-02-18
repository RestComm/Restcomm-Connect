#!/bin/bash
##
## Descript+ion: Restcomm performance test script for Simple Recording application.
## Author      : Henrique Rosa
#
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

SIMULTANEOUS_CALLS_WARMUP=20
MAXIMUM_CALLS_WARMUP=100
CALL_RATE_WARMUP=10

usage() {
    echo 'Script usage: ./simple-record.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE'
    echo "Example: ./simple-record.sh 192.168.1.11 192.168.1.12 100 10000 30"
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
$SIPP_EXECUTABLE -sf $CURRENT_FOLDER/tests/simple-record/simple-record-sipp.xml -s +1233 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS:5090 -l $SIMULTANEOUS_CALLS_WARMUP -m $MAXIMUM_CALLS_WARMUP -r $CALL_RATE_WARMUP -recv_timeout 10000 -t un -nr
echo "Warmup finished"
sleep 2m
fi

echo "About to launch rocket... SIMULTANEOUS_CALLS: $SIMULTANEOUS_CALLS, MAXIMUM_CALLS: $MAXIMUM_CALLS, CALL_RATE: $CALL_RATE"
sleep 3
$SIPP_EXECUTABLE -sf $CURRENT_FOLDER/tests/simple-record/simple-record-sipp.xml -s +1233 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS:5090 -l $SIMULTANEOUS_CALLS -m $MAXIMUM_CALLS -r $CALL_RATE -recv_timeout 10000 -t un -nr -fd 1 -trace_stat -stf $RESULTS_DIR/simple-record-$DATE.csv -trace_screen -screen_file $RESULTS_DIR/simple-record-$DATE-screens.log
echo $?





sipp -sf ./record-sipp.xml -s +1235 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS:5090 -l 50 -m 30000 -r 10 -trace_screen -recv_timeout 10000 -t un -nr

