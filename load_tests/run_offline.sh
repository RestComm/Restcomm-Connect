#!/bin/bash
#Description Script to run load tests against Restcomm for specific branch
#Author George Vagenas

echo "About to rise max open files to 120000"
ulimit -n 120000
echo "ulimit -n: " `ulimit -n`

# arguments
# BRANCH_NAME the branch to use, this will be used to run the load tests and build the binary
# TELSCALE whether to use Telscale Restcomm Connect or Restcomm-Connect

PERFRECORDER_VERSION=41
export LOCAL_RESTCOMM_ADDRESS='192.168.1.151'
RESTCOMM_NETWORK='192.168.1.0'
RESTCOMM_SUBNET='255.255.255.0'
export LOCAL_VOICERSS=''
LOCAL_INTERFACE_TMP='wlan0'



export CURRENT_DIR=`pwd`

read -p 'Restcomm branch name [master]: ' RESTCOMM_BRANCH
RESTCOMM_BRANCH=${RESTCOMM_BRANCH:-master}
echo "...Restcomm branch \"$RESTCOMM_BRANCH\""

# read -p 'Git repository (github/bitbucket) [github]: ' REPOSITORY
# REPOSITORY=${REPOSITORY:-github}
# echo "...Git repository \"$REPOSITORY\""
#
# if [ "$REPOSITORY" = "bitbucket" ]; then
#   read -p 'Bitbucket username: ' BITBUCKET_USERNAME
#   read -p 'Bitbucket password: ' BITBUCKET_PWD
#   read -p 'ci.telestax.com username: ' CI_USERNAME
#   export CI_USERNAME=$CI_USERNAME
#   read -p 'ci.telestax.com password: ' CI_PWD
#   export CI_PWD=$CI_PWD
# fi

read -p 'Workspace folder [/tmp/workspace]: ' WORKSPACE
WORKSPACE=${WORKSPACE:-/tmp/workspace}
echo "...Workspace \"$WORKSPACE\""

read -p 'Restcomm Major Version number [8.1.0]: ' MAJOR_VERSION_NUMBER
MAJOR_VERSION_NUMBER=${MAJOR_VERSION_NUMBER:-8.1.0}
echo "...Major version number \"$MAJOR_VERSION_NUMBER\""

read -p "Restcomm ip address [$LOCAL_RESTCOMM_ADDRESS]: " RESTCOMM_ADDRESS
RESTCOMM_ADDRESS=${RESTCOMM_ADDRESS:-$LOCAL_RESTCOMM_ADDRESS}
if [ -z $RESTCOMM_ADDRESS ] || [ "$RESTCOMM_ADDRESS" == ''  ]; then
 echo "\nRestcomm IP Address is not set! Will exit"
 exit 1
fi
echo "...Restcomm IP Address \"$RESTCOMM_ADDRESS\""
LOCAL_ADDRESS=$RESTCOMM_ADDRESS

read -p "Local interface [$LOCAL_INTERFACE_TMP]: " LOCAL_INTERFACE
LOCAL_INTERFACE=${LOCAL_INTERFACE:-$LOCAL_INTERFACE_TMP}
export LOCAL_INTERFACE=$LOCAL_INTERFACE
echo "...Local interface  \"$LOCAL_INTERFACE\""

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

read -p 'Collect JMAP [true]: ' COLLECT_JMAP
COLLECT_JMAP=${COLLECT_JMAP:-true}
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

read -p 'Collect logs at the end of the test [true]: ' COLLECT_LOGS
COLLECT_LOGS=${COLLECT_LOGS:-true}
echo "...Collect logs at the end of the test \"$COLLECT_LOGS\""

echo "****************************************************"
read -p 'Ready to start the test. Press enter'
echo "****************************************************"

export GITHUB_RESTCOMM_MASTER=$WORKSPACE/github-master
export RELEASE=$WORKSPACE/release

if [ $REMOVE_EXISTING_WORKSPACE == "true" ] || [ $REMOVE_EXISTING_WORKSPACE == "TRUE" ]; then
    rm -rf $WORKSPACE
    mkdir -p $WORKSPACE

    echo "Will use Github Restcomm repository"
    export GITHUB_RESTCOMM_HOME=$WORKSPACE/github-restcomm
    export RESULTS_DIR=$GITHUB_RESTCOMM_HOME/load_tests/results

    echo "Will clone Restcomm to $GITHUB_RESTCOMM_MASTER"
    if [ ! -d "$GITHUB_RESTCOMM_MASTER" ]; then
      mkdir -p $GITHUB_RESTCOMM_MASTER
    fi
    git clone -b master https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_MASTER

    if [ ! -d "$GITHUB_RESTCOMM_HOME" ]; then
      mkdir -p $GITHUB_RESTCOMM_HOME
    fi

    echo "Will clone Restcomm to $GITHUB_RESTCOMM_HOME"
    git clone -b $RESTCOMM_BRANCH https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_HOME

    cp -ar ./* $GITHUB_RESTCOMM_HOME/load_tests
    # cp -ar $GITHUB_RESTCOMM_MASTER/load_tests $GITHUB_RESTCOMM_HOME/load_tests

    cd $GITHUB_RESTCOMM_HOME/load_tests/
    echo "About to start building Restcomm locally to $RELEASE"
    ./build-restcomm-local.sh $RESTCOMM_BRANCH $GITHUB_RESTCOMM_HOME $MAJOR_VERSION_NUMBER
    unzip $GITHUB_RESTCOMM_HOME/Restcomm-JBoss-AS7.zip -d $RELEASE
    mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
    mv $GITHUB_RESTCOMM_HOME/Restcomm-JBoss-AS7.zip $WORKSPACE

    # if [ "$REPOSITORY" = "github" ]; then
    #     echo "Will use Github Restcomm repository"
    #     export GITHUB_RESTCOMM_HOME=$WORKSPACE/github-restcomm
    #     export RESULTS_DIR=$GITHUB_RESTCOMM_HOME/load_tests/results
    #
    #     echo "Will clone Restcomm to $GITHUB_RESTCOMM_MASTER"
    #     if [ ! -d "$GITHUB_RESTCOMM_MASTER" ]; then
    #       mkdir -p $GITHUB_RESTCOMM_MASTER
    #     fi
    #     git clone -b master https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_MASTER
    #
    #     if [ ! -d "$GITHUB_RESTCOMM_HOME" ]; then
    #       mkdir -p $GITHUB_RESTCOMM_HOME
    #     fi
    #
    #     echo "Will clone Restcomm to $GITHUB_RESTCOMM_HOME"
    #     git clone -b $RESTCOMM_BRANCH https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_HOME
    #
    #     cp -ar ./* $GITHUB_RESTCOMM_HOME/load_tests
    #     # cp -ar $GITHUB_RESTCOMM_MASTER/load_tests $GITHUB_RESTCOMM_HOME/load_tests
    #
    #     cd $GITHUB_RESTCOMM_HOME/load_tests/
    #     echo "About to start building Restcomm locally"
    #     ./build-restcomm-local.sh $RESTCOMM_BRANCH $GITHUB_RESTCOMM_HOME $MAJOR_VERSION_NUMBER
    #     unzip $GITHUB_RESTCOMM_HOME/Restcomm-JBoss-AS7.zip -d $RELEASE
    #     mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
    # else
    #     echo "Will use Telestax Restcomm repository"
    #
    #     export BITBUCKET_RESTCOMM_HOME=$WORKSPACE/bitbucket-restcomm
    #     export RESULTS_DIR=$BITBUCKET_RESTCOMM_HOME/load_tests/results
    #
    #     echo "Will clone Restcomm to $GITHUB_RESTCOMM_MASTER"
    #     if [ ! -d "$GITHUB_RESTCOMM_MASTER" ]; then
    #       mkdir -p $GITHUB_RESTCOMM_MASTER
    #     fi
    #     git clone -b master https://github.com/RestComm/RestComm-Core.git $GITHUB_RESTCOMM_MASTER
    #
    #     if [ ! -d "$BITBUCKET_RESTCOMM_HOME" ]; then
    #       mkdir -p $BITBUCKET_RESTCOMM_HOME
    #     fi
    #
    #     echo "Will clone Restcomm to $BITBUCKET_RESTCOMM_HOME"
    #     git clone -b $RESTCOMM_BRANCH https://$BITBUCKET_USERNAME:$BITBUCKET_PWD@bitbucket.org/telestax/telscale-restcomm.git $BITBUCKET_RESTCOMM_HOME
    #
    #     cp -ar ./* $BITBUCKET_RESTCOMM_HOME/load_tests/
    #
    #     cd $BITBUCKET_RESTCOMM_HOME/load_tests/
    #     echo "About to start building Restcomm locally"
    #     ./build-telscale-restcomm-local.sh $RESTCOMM_BRANCH $BITBUCKET_RESTCOMM_HOME $MAJOR_VERSION_NUMBER
    #     unzip $BITBUCKET_RESTCOMM_HOME/Restcomm-JBoss-AS7.zip -d $RELEASE
    #     mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
    # fi

else
    echo "Remove existing workspace \"$REMOVE_EXISTING_WORKSPACE\". Will remove extracted folder and unzip a fresh folder"
    rm -rf $RELEASE
    unzip $WORKSPACE/Restcomm-JBoss-AS7.zip -d $RELEASE
    mv $RELEASE/Restcomm-JBoss-AS7-*/ $RELEASE/TelScale-Restcomm-JBoss-AS7/
    export GITHUB_RESTCOMM_HOME=$WORKSPACE/github-restcomm
    export RESULTS_DIR=$GITHUB_RESTCOMM_HOME/load_tests/results
fi
export RESTCOMM_HOME=$RELEASE/TelScale-Restcomm-JBoss-AS7
#gvagenas@telestax.com VOICERSS key
export RESTCOMM_NEW_PASSWORD='NewPassword1234'

#prepare PerfCorder tool
export TOOLS_DIR=$WORKSPACE/report-tools
export GOALS_FILE=$CURRENT_DIR/mss-proxy-goals.xsl
cp $GOALS_FILE $WORKSPACE
rm -fr $TOOLS_DIR/*
mkdir -p $TOOLS_DIR
cd $TOOLS_DIR
wget -q -c --auth-no-challenge https://mobicents.ci.cloudbees.com/job/PerfCorder/$PERFRECORDER_VERSION/artifact/target/sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
unzip -q sipp-report-0.2.$PERFRECORDER_VERSION-with-dependencies.jar
chmod 777 $TOOLS_DIR/*.sh

if [ ! -d "$RESULTS_DIR" ]; then
  mkdir -p $RESULTS_DIR
fi


echo "*******************************************************************"
echo "About to start Restcomm performance test"
echo "Application: $TEST_NAME"
echo "Restcomm Address: $RESTCOMM_ADDRESS"
echo "Local Address: $LOCAL_ADDRESS"
echo "Simultaneous Calls: $SIMULTANEOUS_CALLS"
echo "Maximum Calls: $MAXIMUM_CALLS"
echo "Call Rate: $CALL_RATE"
echo "Collect JMAP: $COLLECT_JMAP"
echo "Results Dir: $RESULTS_DIR"
echo "*******************************************************************"

cd $GITHUB_RESTCOMM_HOME/load_tests/
echo "Current dir: $(pwd)"
./run.sh $RESTCOMM_ADDRESS $RESTCOMM_NETWORK $RESTCOMM_SUBNET $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE $TEST_NAME
echo "Creating PerfCorder HTML ... "
cat $RESULTS_DIR/PerfCorderAnalysis.xml | $TOOLS_DIR/pc_html_gen.sh > $RESULTS_DIR/PerfCorderAnalysis.html 2> $RESULTS_DIR/htmlgen.log
#prepare logs to be archived
if [ "$COLLECT_LOGS" == "true"  ]; then
	echo "Collecting logs ..."
	chmod 777 $RESTCOMM_HOME/bin/restcomm/logs_collect.sh
	$RESTCOMM_HOME/bin/restcomm/logs_collect.sh -z
fi
