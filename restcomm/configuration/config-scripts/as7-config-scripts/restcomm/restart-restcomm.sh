#!/usr/bin/env bash
##
## Descript+ion: Script that collects all necessary system logs and data.
## Author     : Lefteris Banos
#

##Global Parameters
DATE=$(date +%F_%H_%M)
DIR_NAME=restcomm_$DATE
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
JMAP_DIR=$BASEDIR/$DIR_NAME

JMAP="false"
DTAR="false"

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

restcomm_jmap(){
if [[ -z "$RESTCOMM_PID" ]]; then
    echo "Please make sure that RestComm is running..."
 else
    jmap -dump:format=b,file=restcomm_jmap.bin $RESTCOMM_PID
     mv restcomm_jmap.bin $JMAP_DIR
 fi

}

rms_jmap(){
if [[ -z "$RMS_PID" ]]; then
      echo "Please make sure that Mediaserver is running..."
  else
      jmap -dump:format=b,file=rms_jmap.bin $RMS_PID
       mv rms_jmap.bin $JMAP_DIR
 fi
}

make_tar() {
 if [ -d "$JMAP_DIR" ]; then
     echo TAR_FILE : $JMAP_DIR.tar.gz
     tar -zcf $JMAP_DIR.tar.gz -C $JMAP_DIR . 3>&1 1>&2 2>&3
     rm -rf $JMAP_DIR
     return 0
 fi
   exit 1
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
        echo "-z make tar"
        echo "-h show brief help"
        exit 0
        ;;
       m)
             JMAP="true"
             ;;
       z)
             DTAR="true"
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
 echo "...JMAP files will be collected to : $JMAP_DIR"
 mkdir -p $JMAP_DIR
 getPID
 rms_jmap
 restcomm_jmap
fi

if [ "$DTAR" == "true" ]; then
make_tar
fi

stopRestComm
startRestComm

