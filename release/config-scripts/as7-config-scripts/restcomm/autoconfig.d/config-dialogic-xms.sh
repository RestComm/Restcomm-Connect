#!/bin/bash
## Description: Configures Dialogic XMS
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

RESTCOMM_STANDALONE=$RESTCOMM_HOME/standalone
RESTCOMM_DEPLOY=$RESTCOMM_STANDALONE/deployments/restcomm.war

## Description: Elects Dialogic XMS as the active Media Server for RestComm
activateXMS() {
	restcomm_conf=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	ms_address="$1"
	
	sed -e "/<mscontrol>/ {
		N; s|<compatibility>.*<\/compatibility>|<compatibility>xms<\/compatibility>|
		N; s|<media-server \".*\">|<media-server name=\"Dialogic XMS\" class=\"com.dialogic.dlg309\">|
		N; s|<address>.*<\/address>|<address>$ms_address<\/address>|
	}" $restcomm_conf > $restcomm_conf.bak
	mv -f $restcomm_conf.bak $restcomm_conf
	echo '...activated Dialogic XMS...'
}

#MAIN
echo 'Configuring Dialogic XMS...'
activateXMS $MS_ADDRESS
echo '...finished configuring Dialogic XMS!'