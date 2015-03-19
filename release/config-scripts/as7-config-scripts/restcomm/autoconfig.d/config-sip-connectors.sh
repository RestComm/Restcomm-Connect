#!/bin/bash
## Description: Configures SIP connectors
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
## Author     : Pavel Slegr (pavel.slegr@telestax.com)

## Description: Configures the connectors for RestComm & configures Proxy if enabled
## Parameters : 1.Public IP
## Parameters : 2.Proxy IP
configConnectors() {
	FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
	static_address="$1"
	proxy_address="$2"

	if [ "$ACTIVE_PROXY" == "true" ] || [ "$ACTIVE_PROXY" == "TRUE" ]; then
		sed -e "s|path-name=\"org.mobicents.ext\"  \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ha.balancing.only\" \1|" \
		-e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$proxy_address\" static-server-port=\"5060\"/>|" \
	        -e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$proxy_address\" static-server-port=\"5060\"/>|" \
	        -e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$proxy_address\" static-server-port=\"5061\"/>|" \
	        -e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$proxy_address\" static-server-port=\"5062\"/>|" \
	    $FILE > $FILE.bak

	else

		if [ -n "$static_address" ]; then
			sed -e "s|path-name=\".*\"  \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ext\"  \1|" \
			-e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5080\"/>|" \
			-e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5080\"/>|" \
			-e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5081\"/>|" \
			-e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"5082\"/>|" \
		    $FILE > $FILE.bak
		else
			sed -e "s|path-name=\".*\" \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ext\" \1|" \
			-e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" static-server-port=\"5080\"/>|" \
			-e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" static-server-port=\"5080\"/>|" \
			-e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" static-server-port=\"5081\"/>|" \
			-e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" static-server-port=\"5082\"/>|" \
		    $FILE > $FILE.bak
		fi
	fi

	mv $FILE.bak $FILE
	echo 'Configured SIP Connectors and Bindings'
}

#MAIN
echo 'Configuring Application Server...'
configConnectors "$STATIC_ADDRESS" "$PROXY_IP"
echo 'Finished configuring Application Server!'
