#!/bin/bash
## Description: Configures Dialogic XMS
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

RESTCOMM_STANDALONE=$RESTCOMM_HOME/standalone
RESTCOMM_DEPLOY=$RESTCOMM_STANDALONE/deployments/restcomm.war

## Description: Elects Dialogic XMS as the active Media Server for RestComm
activateXMS() {
	restcomm_conf=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e '/<mscontrol>/ {
		N; s|<compatibility>.*</compatibility>|<compatibility>mms</compatibility>|
		N; s|<media-server name=".*">|<media-server name="Dialogic XMS">|
	}' $restcomm_conf > $restcomm_conf.bak
	mv -f $restcomm_conf.bak $restcomm_conf
	echo '...activated Dialogic XMS...'
}

## Description: Configures the connectors for RestComm & configures Proxy if enabled
## Parameters : 1.RestComm Public IP
## Parameters : 2.Media Server IP
configureXMS() {
	xms_conf=$RESTCOMM_STANDALONE/configuration/dlgc_JSR309.properties
	restcomm_address=$1
	xms_address=$2
	
	sed -e "s|connector.sip.address=.*|connector.sip.address=$restcomm_address|" \
		-e "s|connector.sip.port=.*|connector.sip.port=5080|" \
		-e "s|mediaserver.1.sip.address=.*|mediaserver.1.sip.address=$xms_address|" \
		-e "s|mediaserver.1.sip.port=.*|mediaserver.1.sip.port=5060|" \
		$xms_conf > $xms_conf.bak
	mv -f $xms_conf.bak $xms_conf
	echo '...updated Dialogic XMS configuration...'
}

#MAIN
echo 'Configuring Dialogic XMS...'
configureXMS "$STATIC_ADDRESS" "$MS_ADDRESS"
activateXMS
echo '...finished configuring Dialogic XMS!'
