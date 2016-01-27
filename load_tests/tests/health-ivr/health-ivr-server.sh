LOCAL_ADDRESS=192.168.1.2

ulimit -n 500000

sipp -sf ./health-ivr-sipp-server.xml -s +1999 -bind_local -p 5060 -trace_screen -trace_err -recv_timeout 6000 -t un -nr
