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

   if [ -n "$SECURESSL" ]; then
       sed -i "s|ws:|wss:|" $FILE
       if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
           sed -i "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${LB_SIP_PORT_WSS}'|" $FILE
           sed -i "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${LB_SIP_PORT_WSS}';|" $FILE
       else
            sed -i "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${SIP_PORT_WSS}'|" $FILE
            sed -i "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${SIP_PORT_WSS}';|" $FILE
        fi
   else

      if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
            sed -i "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${LB_SIP_PORT_WS}'|" $FILE
            sed -i "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${LB_SIP_PORT_WS}';|" $FILE
       else
            sed -i "s|\$scope.serverAddress + ':[0-9][0-9]*'|\$scope.serverAddress + ':${SIP_PORT_WS}'|" $FILE
            sed -i "s|\$scope.serverPort = '[0-9][0-9]*';|\$scope.serverPort = '${SIP_PORT_WS}';|" $FILE
       fi
   fi
}



# MAIN
echo 'Configuring Olympus...'
#Reload Variables
olympusPortConf