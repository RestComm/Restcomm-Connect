#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

ulimit -n 500000

sipp -sf ./health-ivr-sipp-client.xml -s +5555 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 300 -m 30000 -r 10 -trace_screen -trace_err -recv_timeout 5000 -t un -nr
