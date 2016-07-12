#!/usr/bin/env bash
##
## Description: Configures RestComm
## Author: Lefteris Banos (eleftherios.banos@telestax.com)
##

BASEDIR=$RESTCOMM_HOME

if [ ! -d "$BASEDIR/standalone/deployments/olympus.war" ]; then
    mkdir $BASEDIR/standalone/deployments/olympus-exploded.war
    unzip -q $BASEDIR/standalone/deployments/olympus.war -d $BASEDIR/standalone/deployments/olympus-exploded.war/
    rm -f $BASEDIR/standalone/deployments/olympus.war
    mv -f $BASEDIR/standalone/deployments/olympus-exploded.war $BASEDIR/standalone/deployments/olympus.war
fi

#Set Olympus port if WSS is used.
olympusPortConf(){
FILE=$BASEDIR/standalone/deployments/olympus.war/resources/js/controllers/register.js

    #Check for Por Offset
    local SIP_PORT_WS=$((SIP_PORT_WS + PORT_OFFSET))
    local SIP_PORT_WSS=$((SIP_PORT_WSS + PORT_OFFSET))

    if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
            if [ -n "$SECURESSL" ]; then
                sed -e "s|ws:|wss:|" $FILE > $FILE.bak
                mv $FILE.bak $FILE
            fi
            sed -e "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${LB_SIP_PORT_WS}'|" \
                -e "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${LB_SIP_PORT_WS}';|" $FILE > $FILE.bak
    else
         if [ -n "$SECURESSL" ]; then
            sed -e "s|ws:|wss:|" \
                -e "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${SIP_PORT_WSS}'|" \
                -e "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${SIP_PORT_WSS}';|" $FILE > $FILE.bak
         else
            sed -e "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${SIP_PORT_WS}'|" \
                -e "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${SIP_PORT_WS}';|" $FILE > $FILE.bak
         fi
    fi
    mv $FILE.bak $FILE
}



# MAIN
echo 'Configuring Olympus...'
olympusPortConf