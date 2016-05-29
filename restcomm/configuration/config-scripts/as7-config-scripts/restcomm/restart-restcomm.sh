#!/usr/bin/env bash
##
## Descript+ion: Script that collects all necessary system logs and data.
## Author     : Lefteris Banos
#

JMAP="false"

##
## FUNCTIONS
##
getPID(){
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

}

stopRestComm(){
    echo "...stoping RestComm"
    ./stop-restcomm.sh

   while [[ ! -z "$RESTCOMM_PID" || ! -z "$RMS_PID" ]]; do
       getPID
        echo "...waiting RestComm and MS to stop"
      sleep 2
   done
}

startRestComm(){
    echo "...starting RestComm"
    ./start-restcomm.sh
}

#MAIN
# parse the flag options (and their arguments)
while getopts "hmz" OPT; do
     case "$OPT" in
       h)
        echo "Description: Collects system data. The output is a compressed file."
        echo " "
        echo "restart-restcomm.sh [options]"
        echo " "
        echo "options:"
        echo "-m collect jmap"
        echo "now will jusr restart Restcomm right now"
        echo "-h show brief help"
        exit 0
        ;;
       m)
             JMAP="true"
             ;;
      now)
             JMAP="false"
             ;;
       ?)
             echo "Invalid option: $OPTARG"
             echo "Type \"restart-restcomm.sh -help\" for instructions"
             exit 1 ;;
    esac
done

# get rid of the just-finished flag arguments
shift $(($OPTIND-1))


if [ "$JMAP" == "true" ]; then
  echo "...JMAP files will be collected"
 ./collect_jmap
fi

stopRestComm
startRestComm
