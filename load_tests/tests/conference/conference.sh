#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

ulimit -n 500000

./sipp/sipp -sf ./conference.xml -s +7777 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 100 -m 150 -r 30 -trace_screen -trace_err -recv_timeout 10000 -t un -nr
