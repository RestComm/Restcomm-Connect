#!/bin/bash
#Description Script to run load tests against Restcomm for specific branch
#Author George Vagenas

echo "About to rise max open files to 120000"
ulimit -n 120000
echo "ulimit -n: " `ulimit -n`

# arguments
# BRANCH_NAME the branch to use, this will be used to run the load tests and build the binary
# TELSCALE whether to use Telscale Restcomm Connect or Restcomm-Connect

PERFRECORDER_VERSION=34
export LOCAL_RESTCOMM_ADDRESS='192.168.1.151'
export LOCAL_VOICERSS='5aa416d17f5d40fa990194cd9b3df41d'
export LOCAL_INTERFACE='wlan0'

export CURRENT_DIR=`pwd`

read -p 'Restcomm branch name [master]: ' RESTCOMM_BRANCH
RESTCOMM_BRANCH=${RESTCOMM_BRANCH:-master}
echo "...Restcomm branch \"$RESTCOMM_BRANCH\""

read -p 'Workspace folder [/tmp/workspace]: ' WORKSPACE
WORKSPACE=${WORKSPACE:-/tmp/workspace}
echo "...Workspace \"$WORKSPACE\""

read -p 'Restcomm Major Version number [8.0.0]: ' MAJOR_VERSION_NUMBER
MAJOR_VERSION_NUMBER=${MAJOR_VERSION_NUMBER:-8.0.0}
echo "...Major version number \"$MAJOR_VERSION_NUMBER\""

read -p "Restcomm ip address [$LOCAL_RESTCOMM_ADDRESS]: " RESTCOMM_ADDRESS
RESTCOMM_ADDRESS=${RESTCOMM_ADDRESS:-$LOCAL_RESTCOMM_ADDRESS}
if [ -z $RESTCOMM_ADDRESS ] || [ "$RESTCOMM_ADDRESS" == ''  ]; then
 echo "\nRestcomm IP Address is not set! Will exit"
 exit 1
fi
echo "...Restcomm IP Address \"$RESTCOMM_ADDRESS\""
LOCAL_ADDRESS=$RESTCOMM_ADDRESS

read -p 'Load test name [helloplay]: ' TEST_NAME
TEST_NAME=${TEST_NAME:-helloplay}
echo "...Load test name \"$TEST_NAME\""

read -p 'Simultaneous calls [20]: ' SIMULTANEOUS_CALLS
SIMULTANEOUS_CALLS=${SIMULTANEOUS_CALLS:-20}
echo "...Simultaneous calls \"$SIMULTANEOUS_CALLS\""

read -p 'Maximum calls [500]: ' MAXIMUM_CALLS
MAXIMUM_CALLS=${MAXIMUM_CALLS:-500}
echo "...Maximum calls \"$MAXIMUM_CALLS\""

read -p 'Call rate [20]: ' CALL_RATE
CALL_RATE=${CALL_RATE:-20}
echo "...Call rate \"$CALL_RATE\""

read -p "VoiceRSS TTS Engine key [$LOCAL_VOICERSS]: " VOICERSS
VOICERSS=${VOICERSS:-$LOCAL_VOICERSS}
if [ -z $VOICERSS ] || [ "$VOICERSS" == ''  ]; then
 echo "\nVoiceRSS TTS Engine key is not set! Will exit"
 exit 1
fi
export VOICERSS=$VOICERSS
echo "...VoiceRSS TTS Engine key \"$VOICERSS\""

read -p 'Collect JMAP [false]: ' COLLECT_JMAP
COLLECT_JMAP=${COLLECT_JMAP:-false}
echo "...Collect JMAP \"$COLLECT_JMAP\""
export COLLECT_JMAP=$COLLECT_JMAP

read -p 'Run a warmup test [false]:' WARMUP
WARMUP=${WARMUP:-false}
export WARMUP=$WARMUP
echo "...Run a warm test $WARMUP"

read -p 'Log level [INFO]:' LOGLEVEL
LOGLEVEL=${LOGLEVEL:-INFO}
export LOGLEVEL=$LOGLEVEL
echo "...Log level $LOGLEVEL"

read -p 'Remove existing workspace [true]: ' REMOVE_EXISTING_WORKSPACE
REMOVE_EXISTING_WORKSPACE=${REMOVE_EXISTING_WORKSPACE:-true}
echo "...Remove existing workspace \"$REMOVE_EXISTING_WORKSPACE\""

export GITHUB_RESTCOMM_MASTER=$WORKSPACE/github-master
export GITHUB_RESTCOMM_HOME=$WORKSPACE/github-restcomm
export RELEASE=$WORKSPACE/release

if [ $REMOVE_EXISTING_WORKSPACE == "true" ] || [ $REMOVE_EXISTING_WORKSPACE == "TRUE" ]; then
    rm -rf $WORKSPACE

    echo "Will clone Restcomm to $GITHUB_RESTCOMM_MASTER"
    echo "Will clone Restcomm to $GITHUB_RESTCOMM_HOME"

    git clone -b master https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_MASTER
    git clone -b $RESTCOMM_BRANCH https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_HOME
    # cp -ar $GITHUB_RESTCOMM_MASTER/load_tests $GITHUB_RESTCOMM_HOME/load_tests

    cp -ar $CURRENT_DIR/* $GITHUB_RESTCOMM_HOME/load_tests

    cd $GITHUB_RESTCOMM_HOME/load-tests/
    ./build-restcomm-local.sh $RESTCOMM_BRANCH $WORKSPACE $MAJOR_VERSION_NUMBER
    unzip $WORKSPACE/Restcomm-JBoss-AS7.zip -d $RELEASE
    mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
else
    echo "Remove existing workspace \"$REMOVE_EXISTING_WORKSPACE\". Will remove extracted folder and unzip a fresh folder"
    rm -rf $RELEASE
    unzip $WORKSPACE/Restcomm-JBoss-AS7.zip -d $RELEASE
    mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
fi
export RESTCOMM_HOME=$RELEASE/TelScale-Restcomm-JBoss-AS7
#gvagenas@telestax.com VOICERSS key
export RESTCOMM_NEW_PASSWORD='NewPassword1234!@#$'

#prepare PerfCorder tool
export TOOLS_DIR=$WORKSPACE/report-tools
export GOALS_FILE=$CURRENT_DIR/../../telscale-commons/jenkins-aws/Jenkins-Jobs/performance/mss-proxy-goals.xsl
cp $GOALS_FILE $WORKSPACE
rm -fr $TOOLS_DIR/*
mkdir -p $TOOLS_DIR
cd $TOOLS_DIR
wget -q -c --auth-no-challenge https://mobicents.ci.cloudbees.com/job/PerfCorder/$PERFRECORDER_VERSION/artifact/target/sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
unzip -q sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
chmod 777 $TOOLS_DIR/*.sh

echo "*******************************************************************"
echo "About to start Restcomm performance test"
echo "Application: $TEST_NAME"
echo "Restcomm Address: $RESTCOMM_ADDRESS"
echo "Local Address: $LOCAL_ADDRESS"
echo "Simultaneous Calls: $SIMULTANEOUS_CALLS"
echo "Maximum Calls: $MAXIMUM_CALLS"
echo "Call Rate: $CALL_RATE"
echo "Collect JMAP: $COLLECT_JMAP"
echo "*******************************************************************"
cd $GITHUB_RESTCOMM_HOME/load_tests/
./run.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE $TEST_NAME
