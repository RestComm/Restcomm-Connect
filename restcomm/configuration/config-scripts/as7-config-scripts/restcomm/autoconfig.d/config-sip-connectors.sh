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
		sed -e "s|path-name=\"org.mobicents.ext\" \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ha.balancing.only\" \1|" \
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


	#Enable SipServlet statistics
	grep -q 'gather-statistics' $FILE || sed -i "s|congestion-control-interval=\".*\"|& gather-statistics=\"true\"|" $FILE
	echo "Configured gather-statistics"


	if [[ "$TRUSTSTORE_FILE" == '' ]]; then
		echo "TRUSTSTORE_FILE is not set";
		sed -e "s/<connector name=\"https\" \(.*\)>/<\!--connector name=\"https\" \1>/" \
		 -e "s/<\/connector>/<\/connector-->/" $FILE > $FILE.bak
		 mv $FILE.bak $FILE
		 sed -e "s/<\!--connector name=\"http\" \(.*\)-->/<connector name=\"http\" \1\/>/" $FILE > $FILE.bak
		 mv $FILE.bak $FILE
	else
		if [[ "$TRUSTSTORE_PASSWORD"  == '' ]]; then
			echo "TRUSTSTORE_PASSWORD is not set";
		else
			if [[ "$TRUSTSTORE_ALIAS" == '' ]]; then
				echo "TRUSTSTORE_ALIAS is not set";
			else
				echo "TRUSTORE_FILE is set to '$TRUSTSTORE_FILE' ";
				echo "TRUSTSTORE_PASSWORD is set to '$TRUSTSTORE_PASSWORD' ";
				echo "TRUSTSTORE_ALIAS is set to '$TRUSTSTORE_ALIAS' ";
				echo "Will properly configure HTTPS Connector ";
				if [ "$DISABLE_HTTP" == "true" ] || [ "$DISABLE_HTTP" == "TRUE" ]; then
					echo "DISABLE_HTTP is '$DISABLE_HTTP'. Will disable HTTP Connector"
					sed -e "s/<connector name=\"http\" \(.*\)\/>/<\!--connector name=\"http\" \1-->/" $FILE > $FILE.bak
					mv $FILE.bak $FILE
				else
					sed -e "s/<\!--connector name=\"http\" \(.*\)-->/<connector name=\"http\" \1\/>/" $FILE > $FILE.bak
					mv $FILE.bak $FILE
				fi
				if [[ "$TRUSTSTORE_FILE" = /* ]]; then
					CERTIFICATION_FILE=$TRUSTSTORE_FILE
				else
					CERTIFICATION_FILE="\\\${jboss.server.config.dir}/$TRUSTSTORE_FILE"
				fi
				echo "Will use trust store at location: $CERTIFICATION_FILE"
				sed -e "s/<\!--connector name=\"https\" \(.*\)>/<connector name=\"https\" \1>/" \
				-e "s|<ssl name=\"https\" key-alias=\".*\" password=\".*\" certificate-key-file=\".*\" \(.*\)\/>|<ssl name=\"https\" key-alias=\"$TRUSTSTORE_ALIAS\" password=\"$TRUSTSTORE_PASSWORD\" certificate-key-file=\"$CERTIFICATION_FILE\" cipher-suite=\"TLS_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA\" verify-client=\"false\" \1\/>|" \
				-e "s/<\/connector-->/<\/connector>/" $FILE > $FILE.bak
				mv $FILE.bak $FILE
				echo "Properly configured HTTPS Connector to use trustStore file $CERTIFICATION_FILE"
			fi
		fi
	fi
}

#MAIN
echo 'Configuring Application Server...'
configConnectors "$STATIC_ADDRESS" "$PROXY_IP"
echo 'Finished configuring Application Server!'
