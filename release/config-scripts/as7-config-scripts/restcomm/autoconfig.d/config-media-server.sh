#!/bin/bash
## Description: Configures the Media Server
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configUdpManager() {
	FILE=$MMS_HOME/deploy/server-beans.xml
		
	sed -e "s|<property name=\"bindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"bindAddress\">$1<\/property>|" \
	    -e "s|<property name=\"localBindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localBindAddress\">$1<\/property>|" \
	    -e "s|<property name=\"localNetwork\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localNetwork\">$2<\/property>|" \
	    -e "s|<property name=\"localSubnet\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localSubnet\">$3<\/property>|" \
	    -e 's|<property name="useSbc">.*</property>|<property name="useSbc">true</property>|' \
	    -e 's|<property name="dtmfDetectorDbi">.*</property>|<property name="dtmfDetectorDbi">36</property>|' \
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

configJavaOpts() {
    FILE=$MMS_HOME/bin/run.sh
    
	# Find total available memory on the instance
    TOTAL_MEM=$(free -m -t | grep 'Total:' | awk '{print $2}')
    # get 20 percent of available memory
    # need to use division by 1 for scale to be read
    CHUNK_MEM=$(echo "scale=0; ($TOTAL_MEM * 0.2)/1" | bc -l)
    # divide chunk memory into units of 64mb
    MULTIPLIER=$(echo "scale=0; $CHUNK_MEM/64" | bc -l)
    # use multiples of 64mb to know effective memory
    FINAL_MEM=$(echo "$MULTIPLIER * 64" | bc -l)
    MEM_UNIT='m'
    
    MMS_OPTS="-Xms$FINAL_MEM$MEM_UNIT -Xmx$FINAL_MEM$MEM_UNIT -XX:MaxPermSize=256m -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"
    
	sed -e "/# Setup MMS specific properties/ {
		N; s|JAVA_OPTS=.*|JAVA_OPTS=\"$MMS_OPTS\"|
	}" $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo "Updated Java Options for Media Server: $MMS_OPTS"
}

configLogDirectory() {
	FILE=$MMS_HOME/conf/log4j.xml
	DIRECTORY=$MMS_HOME/log
	
	sed -e "/<param name=\"File\" value=\".*server.log\" \/>/ s|value=\".*server.log\"|value=\"$DIRECTORY/server.log\"|" \
	    $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Updated log configuration'
}

## MAIN
echo 'Configuring Mobicents Media Server...'
configUdpManager $BIND_ADDRESS $NETWORK $SUBNET_MASK
#configJavaOpts
configLogDirectory
echo 'Finished configuring Mobicents Media Server!'
