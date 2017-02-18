#!/bin/bash
##
## Descript+ion: Restcomm performance test script for Dial Client application.
## Author     : George Vagenas
#

usage() {
  echo 'Script usage: ./helloplay-one-minute.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE'
  echo "Example: ./helloplay-one-minute.sh 192.168.1.11 192.168.1.12 100 10000 30"
}

if [[ -z $LOCAL_ADDRESS ]]; then
  LOCAL_ADDRESS=127.0.0.1
fi

DATE=$(date +%F_%H_%M)

echo "Restcomm IP Address: $RESTCOMM_ADDRESS - Local IP Address: $LOCAL_ADDRESS"

echo "About to launch rocket..."
sleep 3
$SIPP_EXECUTABLE -sf $CURRENT_FOLDER/tests/createcall/createcall-sipp-server.xml -s 1999 -i $LOCAL_ADDRESS -p 5060 -mi $LOCAL_ADDRESS -recv_timeout 10000 -t un -nr -fd 1 -trace_stat -stf $RESULTS_DIR/createcall-server-$DATE.csv -trace_screen -screen_file $RESULTS_DIR/createcall-server-$DATE-screens.log
echo $?
