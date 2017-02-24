#!/bin/bash
##
## Descript+ion: Restcomm performance test script
## Author     : George Vagenas
#

export CURRENT_FOLDER=`pwd`
export RESTCOMM_HOME=/tmp/workspace/Restcomm-JBoss-AS7-7.8.0.master-local
export WARMUP=false
export COLLECT_JMAP=true
#export USE_RMS_PID=true

export LOGLEVEL=INFO

export PERFRECORDER_VERSION=27

export RESULTS_FOLDER=$CURRENT_FOLDER/results
if [ ! -d "$RESULTS_FOLDER" ]; then
  mkdir $RESULTS_FOLDER
fi

export RESTCOMM_ADDRESS=$1
export LOCAL_ADDRESS=$2

export SIMULTANEOUS_CALLS=$3
export MAXIMUM_CALLS=$4
export CALL_RATE=$5

#export COLLECT_JMAP=$6

export TEST_NAME=$6

export VOICERSS=
#export VOICERSS=
export RESTCOMM_NEW_PASSWORD='NewPassword'

if [[  -z $VOICERSS ]] || [ "$VOICERSS" == ''  ]; then
  echo "VoiceRSS TTS Service key is not set! Will exit"
  exit 1
fi

#prepare PerfCorder tool
echo "Preparing PerfRecorder"
cp -aR ../../telscale-commons/jenkins-aws/Jenkins-Jobs/performance/mss-proxy-goals.xsl ./
export TOOLS_DIR=$CURRENT_FOLDER/report-tools
export GOALS_FILE=$CURRENT_FOLDER/mss-proxy-goals.xsl
rm -fr $TOOLS_DIR/*
mkdir -p $TOOLS_DIR
cd $TOOLS_DIR
wget -q -c --auth-no-challenge https://mobicents.ci.cloudbees.com/job/PerfCorder/$PERFRECORDER_VERSION/artifact/target/sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
unzip -q sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
chmod 777 $TOOLS_DIR/*.sh
cd $CURRENT_FOLDER

$CURRENT_FOLDER/run.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE $TEST_NAME
echo "Creating PerfCorder HTML ... "
cat $RESULTS_FOLDER/PerfCorderAnalysis.xml | $TOOLS_DIR/pc_html_gen.sh > $RESULTS_FOLDER/PerfCorderAnalysis.html 2> $RESULTS_FOLDER/htmlgen.log
