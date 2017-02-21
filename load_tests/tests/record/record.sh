#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=192.168.1.3

ulimit -n 50000

$SIPP_HOME -sf ./record-sipp.xml -s +1233 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS -l 10 -m 10 -r 10 -trace_screen -recv_timeout 10000 -t un -nr

