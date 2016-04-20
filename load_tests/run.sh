#!/bin/bash
##
## Descript+ion: Restcomm performance test script
## Author     : George Vagenas
#
export CURRENT_FOLDER=`pwd`
echo "Current folder $CURRENT_FOLDER"
export SIPP_EXECUTABLE=$CURRENT_FOLDER/sipp
echo "SIPP Executable $SIPP_EXECUTABLE"
export JVMTOP_EXECUTABLE=$CURRENT_FOLDER/jvmtop.sh
echo "JVMTOP Executable $JVMTOP_EXECUTABLE"
export SIPP_REPORT_EXECUTABLE="java -jar $CURRENT_FOLDER/sipp-report-0.2-SNAPSHOT-with-dependencies.jar -a"

export RESULTS_FOLDER=$CURRENT_FOLDER/results
if [ ! -d "$RESULTS_FOLDER" ]; then
  mkdir $RESULTS_FOLDER
fi

#First prepare Restcomm
echo $'\n********** About to start preparing Restcomm\n'
$CURRENT_FOLDER/prepare-restcomm-for-perf.sh
echo $'\n********** Finished  preparing Restcomm\n'

if [ $# -lt 6 ]; then
    echo "No proper arguments provided"
    echo "Usage instructions: "
    echo './run.sh $RESTCOMM_ADDRESS $LOCAL_ADDRESS $SIMULTANEOUS_CALLS $MAXIMUM_CALLS $CALL_RATE $TEST_NAME'
    echo "Example: ./run.sh 192.168.1.11 192.168.1.12 100 10000 30 Hello-Play"
    exit 1
fi

case "$TEST_NAME" in
"helloplay")
    echo "Testing HelloPlay"
    $RESTCOMM_HOME/bin/restcomm/start-restcomm.sh
    echo $'\n********** Restcomm started\n'
    sleep 45
    $CURRENT_FOLDER/tests/hello-play/helloplay.sh
    sleep 45
    $RESTCOMM_HOME/bin/restcomm/stop-restcomm.sh
    echo $'\n********** Restcomm stopped\n'
    ;;
"conference")
    echo "Testing Conference"
    cp $CURRENT_FOLDER/tests/conference/conference-app.xml $RESTCOMM_HOME/standalone/deployments/restcomm.war/demos/conference-app.xml
    $RESTCOMM_HOME/bin/restcomm/start-restcomm.sh
    echo $'\n********** Restcomm started\n'
    sleep 45
    echo $'\nChange default administrator password\n'
    curl -X PUT http://ACae6e420f425248d6a26948c17a9e2acf:77f8c12cc7b8f8423e5c38b035249166@$RESTCOMM_ADDRESS:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf -d "Password=$RESTCOMM_NEW_PASSWORD"
    echo $'\nAdd new IncomingPhoneNumber 2222 for Conference application\n'
    curl -X POST  http://administrator%40company.com:$RESTCOMM_NEW_PASSWORD@$RESTCOMM_ADDRESS:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers.json -d "PhoneNumber=2222" -d "VoiceUrl=/restcomm/demos/conference-app.xml" -d "isSIP=true"
    echo ""
    sleep 15
    $CURRENT_FOLDER/tests/conference/conference.sh
    sleep 45
    $RESTCOMM_HOME/bin/restcomm/stop-restcomm.sh
    echo $'\n********** Restcomm stopped\n'
    ;;
"helloplay-one-minute")
    echo "Testing Hello-Play One Minute"
    cp -ar $CURRENT_FOLDER/resource/audio/demo-prompt-one-minute.wav $RESTCOMM_HOME/standalone/deployments/restcomm.war/audio/demo-prompt.wav
    rm -rf $RESTCOMM_HOME/standalone/deployments/restcomm.war/cache/AC*
    $RESTCOMM_HOME/bin/restcomm/start-restcomm.sh
    echo $'\n********** Restcomm started\n'
    sleep 45
    $CURRENT_FOLDER/tests/hello-play-one-minute/helloplay-one-minute.sh
    sleep 45
    $RESTCOMM_HOME/bin/restcomm/stop-restcomm.sh
    echo $'\n********** Restcomm stopped\n'
    ;;
*) echo "Not known test: $TEST_NAME"
   ;;
esac
