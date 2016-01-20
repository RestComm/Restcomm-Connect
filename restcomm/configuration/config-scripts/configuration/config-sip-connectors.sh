#!/bin/bash
## Description : Configures SIP connectors
## Dependencies:
## 				1. RESTCOMM_HOME variable
##				2. read-network-props script
## Author: Henrique Rosa

NET_PROPS="../utils/read-network-props.sh"
JBOSS_CONF="$RESTCOMM_HOME/standalone/configuration/"

if [ -z "$RESTCOMM_HOME" ]; then
	echo "RESTCOMM_HOME is not defined. Please setup this environment variable and try again."
	exit 1
fi

if [ ! -f $NET_PROPS ]; then
	echo "Network Properties dependency not found: $NET_PROPS"
	exit 1
fi

# Import network properties
source $NET_PROPS

## Description: Configures the connectors for RestComm
## Parameters : 1.Public IP
configConnectors() {
	FILE=$JBOSS_CONF/standalone-sip.xml
	sed -e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$1\" static-server-port=\"5080\"/>|" \
	    -e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$1\" static-server-port=\"5080\"/>|" \
	    -e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$1\" static-server-port=\"5081\"/>|" \
	    -e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$1\" static-server-port=\"5082\"/>|" \
	    $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Configured SIP Connectors and Bindings'
}

echo 'Configuring Application Server...'
configConnectors $PUBLIC_IP
echo 'Finished configuring Application Server!'