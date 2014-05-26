#! /bin/bash
##
## Description: Configures SIP Load Balancer
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
##

## FUNCTIONS
configLoadBalancer() {
	lb_file="$LB_HOME/lb-configuration.properties"
	bind_address="$1"

	sed -e "s|^host=.*|host=$bind_address|" $lb_file > $lb_file.bak
	mv $lb_file.bak $lb_file
	echo 'Updated Load Balancer configuration file'
}

configSipStack() {
	lb_file="$RESTCOMM_HOME/standalone/configuration/mss-sip-stack.properties"
	bind_address="$1"
	
	if [[ "$RUN_MODE" == *"-lb" ]]; then
		sed -e 's|^#org.mobicents.ha.javax.sip.BALANCERS=|org.mobicents.ha.javax.sip.BALANCERS=|' \
		    -e "s|org.mobicents.ha.javax.sip.BALANCERS=.*|org.mobicents.ha.javax.sip.BALANCERS=$bind_address:5065|" \
		    $lb_file > $lb_file.bak
		echo 'Activated Load Balancer on SIP stack configuration file'
	else
		sed -e 's|^org.mobicents.ha.javax.sip.BALANCERS=|#org.mobicents.ha.javax.sip.BALANCERS=|' \
			$lb_file > $lb_file.bak
		echo 'Deactivated Load Balancer on SIP stack configuration file'
	fi
	mv $lb_file.bak $lb_file
}

configLogs() {
	# Create directory to keep logs
	mkdir -p $LB_HOME/logs
	echo "Created logging directory $LB_HOME/logs"
	
	# make log location absolute
	lb_file="$LB_HOME/lb-log4j.xml"
	sed -e "s|<param name=\"file\" value=\".*\"/>|<param name=\"file\" value=\"$LB_HOME/logs/load-balancer.log\"/>|" $lb_file > $lb_file.bak
	mv -f $lb_file.bak $lb_file
}

configStandalone() {
	lb_file="$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml"
	
	path_name='org.mobicents.ext'
	if [[ "$RUN_MODE" == *"-lb" ]]; then
		path_name="org.mobicents.ha.balancing.only"
	fi
	
	sed -e "s|stack-properties=\"configuration/mss-sip-stack.properties\" path-name=\".*\" |stack-properties=\"configuration/mss-sip-stack.properties\" path-name=\"$path_name\" |" $lb_file > $lb_file.bak
	mv -f $lb_file.bak $lb_file
}

## MAIN
configLogs
configLoadBalancer "$BIND_ADDRESS"
configSipStack
configStandalone