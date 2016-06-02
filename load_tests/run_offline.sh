#!/bin/bash
##
## Descript+ion: Restcomm performance test script
## Author     : George Vagenas
#

export CURRENT_FOLDER=`pwd`
export RESTCOMM_HOME=/home/gvagenas/Downloads/Restcomm/Restcomm-JBoss-AS7-7.6.0.875
export WARMUP=true

export RESULTS_FOLDER=$CURRENT_FOLDER/results
if [ ! -d "$RESULTS_FOLDER" ]; then
  mkdir $RESULTS_FOLDER
fi

export RESTCOMM_ADDRESS=$1
export LOCAL_ADDRESS=$2

export SIMULTANEOUS_CALLS=$3
export MAXIMUM_CALLS=$4
export CALL_RATE=$5

export COLLECT_JMAP=$6

export TEST_NAME="${7,,}"

export VOICERSS=c58e134224704b0182f2e2eaef59f8d8
export RESTCOMM_NEW_PASSWORD='NewPassword'

$CURRENT_FOLDER/run.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE $COLLECT_JMAP $TEST_NAME
