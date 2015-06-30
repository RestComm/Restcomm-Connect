#! /bin/bash
##
## Description: Stops RestComm and Media Server processes running on GNU Screen sessions
## Authors    : Henrique Rosa   henrique.rosa@telestax.com
##

echo 'shutting down telscale restcomm...'
# stop load balancer if necessary
if screen -ls | grep -q 'balancer'; then
	screen -S 'balancer' -p 0 -X 'quit'
	echo '...stopped SIP Load Balancer running on screen session "balancer"...'
else 
	echo '...load balancer is not running, skipping...'
fi
# stop Media Server if necessary
if screen -ls | grep -q 'mms'; then
	screen -S 'mms' -p 0 -X 'quit'
	echo '...stopped Mobicents Media Server instance running on screen session "mms"...'
else
	echo '...media server is not running, skipping...'
fi
# stop restcomm if necessary
if screen -list | grep -q 'restcomm'; then
	screen -S 'restcomm' -p 0 -X 'quit'
	echo '...stopped RestComm instance running on screen session "restcomm"!'
else
	echo '...restComm already stopped!'
fi