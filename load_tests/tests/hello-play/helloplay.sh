#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

sipp -sf ./helloplay-sipp.xml -s +1234 $RESTCOMM_ADDRESS:5080 -p 5090 -mi $LOCAL_ADDRESS:5090 -l 5 -m 100 -r 1 -trace_screen -recv_timeout 10000 -t un -nr