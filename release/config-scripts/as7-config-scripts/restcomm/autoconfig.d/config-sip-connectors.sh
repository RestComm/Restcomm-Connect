#!/bin/bash
## Description: Configures SIP connectors
## Author: Henrique Rosa

## Description: Configures the connectors for RestComm
## Parameters : 1.Public IP
configConnectors() {
	FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
	static_address="$1"
	
	if [ -n "$static_address" ]; then
		sed -e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5080\"/>|" \
	        -e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5080\"/>|" \
	        -e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5081\"/>|" \
	        -e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5082\"/>|" \
	    $FILE > $FILE.bak
	else
		sed -e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" static-server-port=\"5080\"/>|" \
	        -e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" static-server-port=\"5080\"/>|" \
	        -e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" static-server-port=\"5081\"/>|" \
	        -e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" static-server-port=\"5082\"/>|" \
	    $FILE > $FILE.bak
	fi

	mv $FILE.bak $FILE
	echo 'Configured SIP Connectors and Bindings'
}

echo 'Configuring Application Server...'
configConnectors "$STATIC_ADDRESS"
echo 'Finished configuring Application Server!'