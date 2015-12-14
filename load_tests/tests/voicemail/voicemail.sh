#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

ulimit -n 500000
sipp -sf ./voicemail.xml -s +8888 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090  -l 50 -m 5000 -r 10 -trace_screen -recv_timeout 10000 -t un -nr
