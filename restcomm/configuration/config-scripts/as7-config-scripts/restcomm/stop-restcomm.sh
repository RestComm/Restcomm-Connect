#! /bin/bash
##
## Description: Stops RestComm and Media Server processes running on GNU Screen sessions
## Authors    : Henrique Rosa   henrique.rosa@telestax.com
##

echo 'shutting down telscale restcomm...'


getPIDKill(){
   RESTCOMM_PID=" "
   RMS_PID=""

   RESTCOMM_PID=$(jps | grep jboss-modules.jar | cut -d " " -f 1)

   while read -r line
   do
    if  ps -ef | grep $line | grep -q  mediaserver
    then
          RMS_PID=$line
   fi
   done < <(jps | grep Main | cut -d " " -f 1)

   if [ -n "$RESTCOMM_PID" ]; then
        echo "RESTCOMM_PID: $RESTCOMM_PID"
        kill -9 $RESTCOMM_PID
   fi

    if [ -n "$RMS_PID" ]; then
        echo "RMS_PID: $RMS_PID"
        kill -9 $RMS_PID
   fi
}

#Main
#Kill RMS and RC
getPIDKill

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