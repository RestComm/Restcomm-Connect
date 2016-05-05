#! /bin/bash
##
## Description: Configures SIP Load Balancer
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
## Author     : Pavel Slegr (pavel.slegr@telestax.com)
## Author     : Charles Roufay (charles.roufay@telestax.com)
##
## Last update: 22/03/2016
## Change Log: Move away from Telestax Proxy and configure LB from restcomm.conf
## FUNCTIONS
##
##
##
##
configSipStack() {
	lb_sipstack_file="$RESTCOMM_HOME/standalone/configuration/mss-sip-stack.properties"

        if [ "$ACTIVATE_LB" == "true" ] || [ "$ACTIVATE_LB" == "TRUE" ]; then
		sed -e 's|^#org.mobicents.ha.javax.sip.BALANCERS=|org.mobicents.ha.javax.sip.BALANCERS=|' \
		    -e "s|org.mobicents.ha.javax.sip.BALANCERS=.*|org.mobicents.ha.javax.sip.BALANCERS=$LB_ADDRESS:$LB_INTERNAL_PORT|" \
   		    -e 's|^#org.mobicents.ha.javax.sip.REACHABLE_CHECK=|org.mobicents.ha.javax.sip.REACHABLE_CHECK=|' \
		    -e "s|org.mobicents.ha.javax.sip.REACHABLE_CHECK=.*|org.mobicents.ha.javax.sip.REACHABLE_CHECK=false|" \
		    $lb_sipstack_file > $lb_sipstack_file.bak

		echo 'Load Balancer has been activated and mss-sip-stack.properties file updated'
	else
			sed -e 's|^org.mobicents.ha.javax.sip.BALANCERS=|#org.mobicents.ha.javax.sip.BALANCERS=|' \
			    -e 's|^org.mobicents.ha.javax.sip.REACHABLE_CHECK=|#org.mobicents.ha.javax.sip.REACHABLE_CHECK=|' \
				$lb_sipstack_file > $lb_sipstack_file.bak
			echo 'Deactivated Load Balancer on SIP stack configuration file'

	fi
	mv $lb_sipstack_file.bak $lb_sipstack_file
}


configStandalone() {
	lb_standalone_file="$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml"
	
	path_name='org.mobicents.ext'
	if [[ "$RUN_MODE" == *"-lb" ]]; then
		path_name="org.mobicents.ha.balancing.only"
	fi
	
	sed -e "s|subsystem xmlns=\"urn:org.mobicents:sip-servlets-as7:1.0\" application-router=\"configuration/dars/mobicents-dar.properties\" stack-properties=\"configuration/mss-sip-stack.properties\" path-name=\".*\" app-dispatcher-class=\"org.mobicents.servlet.sip.core.SipApplicationDispatcherImpl\" concurrency-control-mode=\"SipApplicationSession\" congestion-control-interval=\"-1\"|subsystem xmlns=\"urn:org.mobicents:sip-servlets-as7:1.0\" application-router=\"configuration/dars/mobicents-dar.properties\" stack-properties=\"configuration/mss-sip-stack.properties\" path-name=\"$path_name\" app-dispatcher-class=\"org.mobicents.servlet.sip.core.SipApplicationDispatcherImpl\" concurrency-control-mode=\"SipApplicationSession\" congestion-control-interval=\"-1\"|" $lb_standalone_file > $lb_standalone_file.bak
	mv -f $lb_standalone_file.bak lb_standalone_file
}



## MAIN
configSipStack 
configStandalone


