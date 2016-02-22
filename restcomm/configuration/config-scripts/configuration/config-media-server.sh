#!/bin/bash
## Description : Configures the Media Server
## Dependencies:
## 				1. RESTCOMM_HOME variable
##				2. read-network-props script
## Author: Henrique Rosa

# IMPORTS
NET_PROPS="../utils/read-network-props.sh"
MMS_INSTALL=$RESTCOMM_HOME/telscale-media/telscale-media-server

# PARAMETERS VALIDATION
if [ -z "$RESTCOMM_HOME" ]; then
	echo "RESTCOMM_HOME is not defined. Please setup this environment variable and try again."
	exit 1
fi

if [ ! -f $NET_PROPS ]; then
	echo "Network Properties dependency not found: $NET_PROPS"
	exit 1
fi

# IMPORTS
source $NET_PROPS

## Description: Updates UDP Manager configuration
## Parameters : 1. Private IP
## 				2. Local Network
## 				3. Local Subnet
configUdpManager() {
	FILE=$MMS_INSTALL/deploy/server-beans.xml
		
	sed -e "s|<property name=\"bindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"bindAddress\">$1<\/property>|" \
	    -e "s|<property name=\"localBindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localBindAddress\">$1<\/property>|" \
	    -e "s|<property name=\"localNetwork\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localNetwork\">$2<\/property>|" \
	    -e "s|<property name=\"localSubnet\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localSubnet\">$3<\/property>|" \
	    -e 's|<property name="useSbc">.*</property>|<property name="useSbc">true</property>|' \
	    -e 's|<property name="dtmfDetectorDbi">.*</property>|<property name="dtmfDetectorDbi">36</property>|' \
	    -e 's|<response-timeout>.*</response-timeout>|<response-timeout>5000</response-timeout>|' \
	    -e 's|<property name="lowestPort">.*</property>|<property name="lowestPort">64534</property>|' \
	    -e 's|<property name="highestPort">.*</property>|<property name="highestPort">65535</property>|' \
	    $FILE > $FILE.bak
	    
	grep -q -e '<property name="lowestPort">.*</property>' $FILE.bak || sed -i '/rtpTimeout/ a\
    <property name="lowestPort">64534</property>' $FILE.bak
    
    grep -q -e '<property name="highestPort">.*</property>' $FILE.bak || sed -i '/rtpTimeout/ a\
    <property name="highestPort">65535</property>' $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured UDP Manager'
}

configLogDirectory() {
	FILE=$MMS_INSTALL/conf/log4j.xml
	DIRECTORY=$MMS_INSTALL/log
	
	sed -e "/<param name=\"File\" value=\".*server.log\" \/>/ s|value=\".*server.log\"|value=\"$DIRECTORY/server.log\"|" $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Updated log configuration'
}

# MAIN
echo 'Configuring Mobicents Media Server...'
configUdpManager $PRIVATE_IP $NETWORK $SUBNET_MASK
configLogDirectory
echo 'Finished configuring Mobicents Media Server!'