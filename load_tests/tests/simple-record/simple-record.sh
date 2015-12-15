#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

sipp -sf ./record-sipp.xml -s +1235 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS:5090 -l 50 -m 30000 -r 10 -trace_screen -recv_timeout 10000 -t un -nr

