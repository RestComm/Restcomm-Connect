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

olympusPortConf(){
FILE=$BASEDIR/standalone/deployments/olympus.war/resources/js/controllers/register.js

 #Check for Por Offset
    if (( $PORT_OFFSET > 0 )); then
		local SIP_PORT_WS=$((SIP_PORT_WS + PORT_OFFSET))
		local SIP_PORT_WSS=$((SIP_PORT_WSS + PORT_OFFSET))

		#change port due to Port-Offset
		sed -i "s|5082|${SIP_PORT_WS}|" $FILE
	fi

    if [ -n "$SECURESSL" ]; then
        sed -i "s|ws:|wss:|" $FILE
        sed -i "s|${SIP_PORT_WS}|${SIP_PORT_WSS}|" $FILE
    fi
}



# MAIN
echo 'Configuring Olympus...'
#Reload Variables
olympusPortConf