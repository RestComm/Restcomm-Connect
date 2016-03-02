#!/bin/bash
RESTCOMM_ADDRESS=127.0.0.1
LOCAL_ADDRESS=127.0.0.1

#ulimit -n 500000

# having issues with +5555, also get an error when -t un is there
#sipp -sf ./webrtc-sipp-client.xml -s +5555 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 300 -m 30000 -r 10 -trace_screen -trace_err -recv_timeout 5000 -nr -t un 
sudo sipp -sf ./webrtc-sipp-client.xml -s +5555 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 50 -m 50 -r 10 -trace_screen -trace_err -recv_timeout 5000 -nr -t u1
#sipp -sf ./webrtc-sipp-client.xml -s +5555 $RESTCOMM_ADDRESS:5080 -mi $LOCAL_ADDRESS:5090 -l 1 -m 1 -r 1 -trace_screen -trace_err -recv_timeout 5000 -nr -t un
