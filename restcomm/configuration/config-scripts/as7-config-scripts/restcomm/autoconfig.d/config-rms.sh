#!/bin/bash
## Description: Configures the Media Server
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configServerBeans() {
	FILE=$MMS_HOME/conf/mediaserver.xml
	MSERVER_EXTERNAL_ADDRESS="$MEDIASERVER_EXTERNAL_ADDRESS"

	if [ "$MSERVER_EXTERNAL_ADDRESS" = "$1" ]; then
   		MSERVER_EXTERNAL_ADDRESS="\<null\/\>"
	fi

	#Check for Por Offset
	local REMOTEMGCP=$((REMOTEMGCP + PORT_OFFSET))

	sed -e "s|<bindAddress>.*<\/bindAddress>|<bindAddress>$1<\/bindAddress>|" \
		-e "s|<externalAddress>.*</externalAddress>|<externalAddress>$MSERVER_EXTERNAL_ADDRESS</externalAddress>|" \
	    -e "s|<network>.*<\/network>|<network>$2<\/network>|" \
	    -e "s|<subnet>.*<\/subnet>|<subnet>$3<\/subnet>|" \
	    -e "s|<sbc>.*<\/sbc>|<sbc>$USESBC<\/sbc>|" \
	    -e "s|<address>.*<\/address>|<address>$1<\/address>|" \
    	-e "s|<port>.*<\/port>|<port>${REMOTEMGCP}<\/port>|" \
	    -e "s|<lowPort>.*<\/lowPort>|<lowPort>$MEDIASERVER_LOWEST_PORT<\/lowPort>|" \
	    -e "s|<highPort>.*<\/highPort>|<highPort>$MEDIASERVER_HIGHEST_PORT<\/highPort>|" \
	    -e "s|<dtmfDetector poolSize=\"\(.*\)\" dbi=\"\(.*\)\" \/>|<dtmfDetector poolSize=\"\1\" dbi=\"$DTMFDBI\" \/>|" \
	    $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Configured UDP Manager'
}

configRMSJavaOpts() {
    FILE=$MMS_HOME/bin/run.sh
	echo "Add mediaserver extra java options: $RMS_JAVA_OPTS"

	sed -e "/# Setup MMS specific properties/ {
	  N; s|JAVA_OPTS=.*|JAVA_OPTS=\"-Dprogram\.name=\\\$PROGNAME $RMS_JAVA_OPTS\"|
	}" $FILE > $FILE.bak
	mv $FILE.bak $FILE
}

## Description: Configures Media Server Log Directory
configLogDirectory() {
	FILE=$MMS_HOME/conf/log4j.xml
	DIRECTORY=$MMS_HOME/log

	sed -e "/<param name=\"File\" value=\".*server.log\" \/>/ s|value=\".*server.log\"|value=\"$DIRECTORY/server.log\"|" \
	    $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Updated log configuration'
}

## Description: Configures Media Server Manager
## Parameters :
## 		1.BIND_ADDRESS
## 		2.MEDIASERVER_EXTERNAL_ADDRESS
configMediaServerManager() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	bind_address="$1"
	ms_address="$2"
	ms_external_address="$3"

	    #Check for Por Offset
		local LOCALMGCP=$((LOCALMGCP + PORT_OFFSET))
		local REMOTEMGCP=$((REMOTEMGCP + PORT_OFFSET))

		sed -e "s|<local-address>.*</local-address>|<local-address>$bind_address</local-address>|" \
			-e "s|<local-port>.*</local-port>|<local-port>$LOCALMGCP</local-port>|" \
			-e "s|<remote-address>.*</remote-address>|<remote-address>$ms_address</remote-address>|" \
			-e "s|<remote-port>.*</remote-port>|<remote-port>$REMOTEMGCP</remote-port>|" \
			-e "s|<response-timeout>.*</response-timeout>|<response-timeout>500</response-timeout>|" \
			-e "s|<\!--.*<external-address>.*</external-address>.*-->|<external-address>$ms_external_address</external-address>|" \
			-e "s|<external-address>.*</external-address>|<external-address>$ms_external_address</external-address>|" $FILE > $FILE.bak

		mv $FILE.bak $FILE
		echo 'Configured Media Server Manager'
}

set_pool_size () {
    property=$1
    value=$2
    FILE=$MMS_HOME/conf/mediaserver.xml

    case $property  in
        "ivr"|"bridge"|"cnf" )
            sed -e "s|<endpoint name=\"\(.*\)/${property}/\" class=\"\(.*\)\" poolSize=\"\(.*\)\" \/>|<endpoint name=\"\1/${property}/\" class=\"\2\" poolSize=\"${value}\" \/>|" \
            $FILE > $FILE.bak
	        mv $FILE.bak $FILE;;
        "localConnection"|"remoteConnection"|"player"|"recorder" )
            sed -e "s|<${property} poolSize=\".*\" />|<${property} poolSize=\"${value}\" />|"  $FILE > $FILE.bak
            mv $FILE.bak $FILE ;;
         "dtmfDetector" )
            sed -e "s|<${property} poolSize=\".*\" \(.*\) />|<${property} poolSize=\"${value}\" \1 />|"  $FILE > $FILE.bak
            mv $FILE.bak $FILE ;;
          "dtmfGenerator" )
            sed -e "s|<${property} poolSize=\".*\" \(.*\) \(.*\) />|<${property} poolSize=\"${value}\" \1 \2 />|"  $FILE > $FILE.bak
            mv $FILE.bak $FILE ;;
        "mgcp" )
            sed -e "s|<poolSize>.*</poolSize>|<poolSize>${value}</poolSize>|"  $FILE > $FILE.bak
            mv $FILE.bak $FILE ;;
        *) echo "This property ${property} can not be configured";;
    esac
}

configOther(){
    echo "MGCP_RESPONSE_TIMEOUT $MGCP_RESPONSE_TIMEOUT"
    FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
    sed -e "s|<response-timeout>.*</response-timeout>|<response-timeout>${MGCP_RESPONSE_TIMEOUT}</response-timeout>|"  $FILE > $FILE.bak
}

## MAIN
if [[ -z "$MEDIASERVER_LOWEST_PORT" ]]; then
	MEDIASERVER_LOWEST_PORT="34534"
fi
if [[ -z "$MEDIASERVER_HIGHEST_PORT" ]]; then
	MEDIASERVER_HIGHEST_PORT="65534"
fi

echo "Configuring Mobicents Media Server... MS_ADDRESS $MS_ADDRESS BIND_ADDRESS $BIND_ADDRESS NETWORK $NETWORK SUBNET_MASK $SUBNET_MASK RTP_LOW_PORT $MEDIASERVER_LOWEST_PORT RTP_HIGH_PORT $MEDIASERVER_HIGHEST_PORT"
if [ -z "$MS_ADDRESS" ]; then
		MS_ADDRESS=$BIND_ADDRESS
fi

if [ -z "$MS_NETWORK" ]; then
      MS_NETWORK=$NETWORK
fi

if [ -z "$MS_SUBNET_MASK" ]; then
      MS_SUBNET_MASK=$SUBNET_MASK
fi

configServerBeans "$MS_ADDRESS" "$MS_NETWORK" "$MS_SUBNET_MASK"
configRMSJavaOpts
configLogDirectory
configMediaServerManager "$BIND_ADDRESS" "$MS_ADDRESS" "$MEDIASERVER_EXTERNAL_ADDRESS"
configOther
#Contribution by: https://github.com/hamsterksu
#set pool size of RMS resources
for i in $( set -o posix ; set |  grep -e ^MGCPCONTROLLER_ -e ^RESOURCE_  | sort -rn ); do
    reg=$(echo ${i} | cut -d = -f1 | cut -d _ -f2)
    val=$(echo ${i} | cut -d = -f2)

    echo "Update resources pool size: $reg -> $val"
    set_pool_size $reg $val
done
echo 'Finished configuring Mobicents Media Server!'
