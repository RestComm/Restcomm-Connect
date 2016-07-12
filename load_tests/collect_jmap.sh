#!/usr/bin/env bash
##
## Descript+ion: Script that collects all necessary system logs and data.
## Author     : Lefteris Banos
## Author     : George Vagenas
#

##Global Parameters
DATE=$(date +%F_%H_%M)
DIR_NAME=restcomm_$DATE
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
JMAP_DIR=$BASEDIR/results/jmap_$DATE
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/

JMAP="true"
DTAR="true"

##
## FUNCTIONS
##
getPID(){
  if [[ -z $RESTCOMM_PID ]] || [[ -z $RMS_PID ]]; then
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
 fi

}

restcomm_jmap(){
if [[ -z "$RESTCOMM_PID" ]]; then
      getPID
fi

if [[ -z "$RESTCOMM_PID" ]]; then
    echo "Please make sure that RestComm is running..."
 else
    echo "****************************************************************" > $JMAP_DIR/restcomm_mem
    echo "Testname: \"$TEST_NAME\", SIMULTANEOUS_CALLS: $SIMULTANEOUS_CALLS, MAXIMUM_CALLS: $MAXIMUM_CALLS, CALL_RATE: $CALL_RATE" >> $JMAP_DIR/restcomm_mem
    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    echo "GC Histogram before GC.run" >> $JMAP_DIR/restcomm_mem
    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    jcmd $RESTCOMM_PID GC.class_histogram  | grep org.mobicents.servlet.restcomm >> $JMAP_DIR/restcomm_mem

    jcmd $RESTCOMM_PID GC.run
    sleep 15

    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    echo "GC Histogram after GC.run" >> $JMAP_DIR/restcomm_mem
    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    jcmd $RESTCOMM_PID GC.class_histogram  | grep org.mobicents.servlet.restcomm >> $JMAP_DIR/restcomm_mem

    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    echo "JVMTop" >> $JMAP_DIR/restcomm_mem
    echo "****************************************************************" >> $JMAP_DIR/restcomm_mem
    $BASEDIR/jvmtop.sh -n 1 >> $JMAP_DIR/restcomm_mem

    jmap -dump:live,format=b,file=restcomm_jmap_$DATE.bin $RESTCOMM_PID
    mv restcomm_jmap_$DATE.bin $JMAP_DIR
 fi

}

rms_jmap(){
if [[ -z "$RMS_PID" ]]; then
        getPID
fi

if [[ -z "$RMS_PID" ]]; then
      echo "Please make sure that Mediaserver is running..."
  else
      jcmd $RMS_PID GC.run
      sleep 2
      jmap -dump:live,format=b,file=rms_jmap_$DATE.bin $RMS_PID
      mv rms_jmap_$DATE.bin $JMAP_DIR
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

mkdir $JMAP_DIR
restcomm_jmap
rms_jmap
make_tar
