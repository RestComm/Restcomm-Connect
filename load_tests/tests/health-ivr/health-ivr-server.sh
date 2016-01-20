LOCAL_ADDRESS=10.187.67.98

ulimit -n 500000

sipp -sf ./health-ivr-sipp-server.xml -s +1999 -bind_local -p 5060 -trace_screen -trace_err -recv_timeout 3000 -t un -nr