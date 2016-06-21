#!/bin/bash
## Description: Configures SIP connectors
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
## Author     : Pavel Slegr (pavel.slegr@telestax.com)

## Description: Configures the connectors for RestComm & configures Proxy if enabled
## Parameters : 1.Public IP
configConnectors() {
	FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
	static_address="$1"

    #delete additional connectors if any added to erlier run of the script.
    if  grep -q "<!-- new-conectors -->" $FILE
    then
          echo "Additional Connectors Created earlier, going to delete the"
          sed '/<!-- new-conectors -->/,/<!-- new-conectors -->/d' $FILE > $FILE.bak
          mv $FILE.bak $FILE
    else
         echo "Additional Connectors not Created earlier"
    fi

	#Check for Por Offset
	if (( $PORT_OFFSET > 0 )); then
		local SIP_PORT_UDP=$((SIP_PORT_UDP + PORT_OFFSET))
		local SIP_PORT_TCP=$((SIP_PORT_TCP + PORT_OFFSET))
		local SIP_PORT_TLS=$((SIP_PORT_TLS + PORT_OFFSET))
		local SIP_PORT_WS=$((SIP_PORT_WS + PORT_OFFSET))
		local SIP_PORT_WSS=$((SIP_PORT_WSS + PORT_OFFSET))
	fi

    #IF LB activated. (Algorithm "use-load-balancer" used).
	if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
		if [ -z "$LB_INTERNAL_IP" ]; then
      		LB_INTERNAL_IP=$LB_PUBLIC_IP
		fi
		sed -e "s|path-name=\"org.mobicents.ext\" \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ha.balancing.only\" \1|" \
		    -e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$LB_PUBLIC_IP\" static-server-port=\"$SIP_PORT_UDP\" use-load-balancer=\"true\" load-balancer-address=\"$LB_INTERNAL_IP\" load-balancer-rmi-port=\"$LB_RMI_PORT\"  load-balancer-sip-port=\"$LB_SIP_PORT_UDP\"/>|" \
	        -e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$LB_PUBLIC_IP\" static-server-port=\"$SIP_PORT_TCP\" use-load-balancer=\"true\" load-balancer-address=\"$LB_INTERNAL_IP\" load-balancer-rmi-port=\"$LB_RMI_PORT\"  load-balancer-sip-port=\"$LB_SIP_PORT_TCP\"/>|" \
	        -e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$LB_PUBLIC_IP\" static-server-port=\"$SIP_PORT_TLS\" use-load-balancer=\"true\" load-balancer-address=\"$LB_INTERNAL_IP\" load-balancer-rmi-port=\"$LB_RMI_PORT\"  load-balancer-sip-port=\"$LB_SIP_PORT_TLS\"/>|" \
	        -e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$LB_PUBLIC_IP\" static-server-port=\"$SIP_PORT_WS\" use-load-balancer=\"true\" load-balancer-address=\"$LB_INTERNAL_IP\" load-balancer-rmi-port=\"$LB_RMI_PORT\"  load-balancer-sip-port=\"$LB_SIP_PORT_WS\"/>|" \
	        -e "s|<connector name=\"sip-wss\" .*/>|<connector name=\"sip-wss\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-wss\" use-static-address=\"true\" static-server-address=\"$LB_PUBLIC_IP\" static-server-port=\"$SIP_PORT_WSS\" use-load-balancer=\"true\" load-balancer-address=\"$LB_INTERNAL_IP\" load-balancer-rmi-port=\"$LB_RMI_PORT\"  load-balancer-sip-port=\"$LB_SIP_PORT_WSS\"/>|" \
	    $FILE > $FILE.bak

	else
			sed -e "s|path-name=\".*\"  \(app-dispatcher-class=.*\)|path-name=\"org.mobicents.ext\"  \1|" \
			-e "s|<connector name=\"sip-udp\" .*/>|<connector name=\"sip-udp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-udp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"$SIP_PORT_UDP\"/>|" \
			-e "s|<connector name=\"sip-tcp\" .*/>|<connector name=\"sip-tcp\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tcp\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"$SIP_PORT_TCP\"/>|" \
			-e "s|<connector name=\"sip-tls\" .*/>|<connector name=\"sip-tls\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-tls\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"$SIP_PORT_TLS\"/>|" \
			-e "s|<connector name=\"sip-ws\" .*/>|<connector name=\"sip-ws\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-ws\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"$SIP_PORT_WS\"/>|" \
			-e "s|<connector name=\"sip-wss\" .*/>|<connector name=\"sip-wss\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"sip-wss\" use-static-address=\"true\" static-server-address=\"$static_address\" static-server-port=\"$SIP_PORT_WSS\"/>|" \
		    $FILE > $FILE.bak
	fi
	mv $FILE.bak $FILE
	echo 'Configured SIP Connectors and Bindings'


	#Enable SipServlet statistics
	grep -q 'gather-statistics' $FILE || sed -i "s|congestion-control-interval=\".*\"|& gather-statistics=\"true\"|" $FILE
	echo "Configured gather-statistics"
}

#Socket Binding configuration for standalone-sip.xml
configSocketbinding() {
FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml

    #delete additional bindings if any added to erlier run of the script.
    if grep -q "###new-binding###"  $FILE
    then
          echo "Additional Bindings Created earlier, going to delete the"
          sed '/<!-- new-bindings -->/,/<!-- new-bindings -->/d' $FILE > $FILE.bak
          mv $FILE.bak $FILE
    else
         echo "Additional Bindings not Created earlier"
    fi
   
	#check for port offset
	if (( $PORT_OFFSET > 0 )); then
    	sed -i "s|\port-offset=\".*\"|port-offset=\"${PORT_OFFSET}\"|" $FILE
	else
		sed -i "s|\port-offset=\".*\"|port-offset=\"\$\{jboss\.socket\.binding\.port\-offset\:0\}\"|" $FILE
	fi
	sed -e "s|<socket-binding name=\"http\" port=\".*\"/>|<socket-binding name=\"http\" port=\"$HTTP_PORT\"/>|" \
        -e "s|<socket-binding name=\"https\" port=\".*\"/>|<socket-binding name=\"https\" port=\"$HTTPS_PORT\"/>|" \
        -e "s|<socket-binding name=\"sip-udp\" port=\".*\"/>|<socket-binding name=\"sip-udp\" port=\"$SIP_PORT_UDP\"/>|" \
        -e "s|<socket-binding name=\"sip-tcp\" port=\".*\"/>|<socket-binding name=\"sip-tcp\" port=\"$SIP_PORT_TCP\"/>|" \
        -e "s|<socket-binding name=\"sip-tls\" port=\".*\"/>|<socket-binding name=\"sip-tls\" port=\"$SIP_PORT_TLS\"/>|" \
        -e "s|<socket-binding name=\"sip-ws\" port=\".*\"/>|<socket-binding name=\"sip-ws\" port=\"$SIP_PORT_WS\"/>|" \
        -e "s|<socket-binding name=\"sip-wss\" port=\".*\"/>|<socket-binding name=\"sip-wss\" port=\"$SIP_PORT_WSS\"/>|" \
        $FILE > $FILE.bak
        mv $FILE.bak $FILE
}

setMoreConnectors(){
flag1=false
flag2=false
    for i in $( set -o posix ; set | grep ^ADDITIONAL_CONNECTOR_ | sort -rn ); do
        connector=$(echo ${i} | cut -d = -f2  | cut -d _ -f2 | cut -d : -f1)
        port=$(echo ${i} | cut -d = -f2 | cut -d _ -f2 | cut -d : -f2)
        if [ "$flag1" = false ] ; then
            setInitialSign
            flag1=true
        fi
        addConector $connector $port
        addSocketBinding $connector $port
        echo "Configuring log level for: $connector -> $port"
        flag2=true
    done

    if [ "$flag2" = true ] ; then
        setFinalSign
    fi
}

addConector(){
FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
connector=$1
port=$2

    #check for port offset at the new connectors.
    if (( $PORT_OFFSET > 0 )); then
        local port=$((port + PORT_OFFSET))
    fi

    if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
		if [ -z "$LB_INTERNAL_IP" ]; then
      		LB_INTERNAL_IP=$LB_PUBLIC_IP
		fi
         grep -q "connector name=\"${connector}\"" $FILE || sed -e "/path-name=\"org.mobicents.ha.balancing.only\"/a\
               <connector name=\"${connector}\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"${connector}\" use-static-address=\"true\" static-server-address=\"${LB_PUBLIC_IP}\" static-server-port=\"${port}\" use-load-balancer=\"true\" load-balancer-address=\"${LB_INTERNAL_IP}\" load-balancer-rmi-port=\"${LB_RMI_PORT}\"  load-balancer-sip-port=\"${LB_SIP_PORT_UDP}\"/>" $FILE > $FILE.bak

    else
         grep -q "connector name=\"${connector}\"" $FILE || sed -e "/path-name=\"org.mobicents.ext\"/a\
			   <connector name=\"${connector}\" protocol=\"SIP/2.0\" scheme=\"sip\" socket-binding=\"${connector}\" use-static-address=\"true\" static-server-address=\"${static_address}\" static-server-port=\"${port}\"/>" $FILE > $FILE.bak
	fi
	mv $FILE.bak $FILE
	echo 'Configured SIP Connectors and Bindings'
}

addSocketBinding(){
FILE=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
connector=$1
port=$2

  grep -q "socket-binding name=\"${connector}\"" $FILE || sed "/name=\"management-https\"/a <socket-binding name=\"${connector}\" port=\"${port}\"/>" $FILE > $FILE.bak
  mv $FILE.bak $FILE
}

setInitialSign(){
    if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
		if [ -z "$LB_INTERNAL_IP" ]; then
      		LB_INTERNAL_IP=$LB_PUBLIC_IP
		fi
         sed -e "/path-name=\"org.mobicents.ha.balancing.only\"/a\
               <!-- new-conectors -->" $FILE > $FILE.bak

    else
          sed -e "/path-name=\"org.mobicents.ext\"/a\
			   <!-- new-conectors -->" $FILE > $FILE.bak
	fi
	mv $FILE.bak $FILE

	sed "/name=\"management-https\"/a <!-- new-bindings -->" $FILE > $FILE.bak
  mv $FILE.bak $FILE

}

setFinalSign(){
    if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
		if [ -z "$LB_INTERNAL_IP" ]; then
      		LB_INTERNAL_IP=$LB_PUBLIC_IP
		fi
         sed -e "/path-name=\"org.mobicents.ha.balancing.only\"/a\
               <!-- new-conectors -->" $FILE > $FILE.bak

    else
          sed -e "/path-name=\"org.mobicents.ext\"/a\
			   <!-- new-conectors -->" $FILE > $FILE.bak
	fi
	mv $FILE.bak $FILE

	sed "/name=\"management-https\"/a <!-- new-bindings -->" $FILE > $FILE.bak
  mv $FILE.bak $FILE
}

#MAIN
echo 'Configuring Application Server...'
configSocketbinding
configConnectors "$PUBLIC_IP"
setMoreConnectors
echo 'Finished configuring Application Server!'
