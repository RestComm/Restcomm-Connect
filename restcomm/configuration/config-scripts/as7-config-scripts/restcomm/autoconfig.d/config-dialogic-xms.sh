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
		N; s|<compatibility>.*<\/compatibility>|<compatibility>$MS_COMPATIBILITY_MODE<\/compatibility>|
		N; s|<media-server \".*\">|<media-server name=\"Dialogic XMS\" class=\"com.dialogic.dlg309\">|
		N; s|<address>.*<\/address>|<address>$ms_address<\/address>|
	}" $restcomm_conf > $restcomm_conf.bak
	mv -f $restcomm_conf.bak $restcomm_conf
	echo '...activated Dialogic XMS...'
}

fetchExternalResources() {
	if [[ "$MS_COMPATIBILITY_MODE" == "xms" ]]; then

    	echo "Checking required libraries ..."

    	if [ -f $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib/dlgmsc-5.0-alpha.jar ]; then
    		echo "JSR309 library ready"
    	else
    		echo "Downloading JSR309 library ..."
    		cd $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib
    		wget -O dlgmsc-5.0-alpha.jar https://www.dialogic.com/files/jsr-309/4.1/dlgmsc-5.0-alpha.jar
		fi

		if [ -f $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib/dlgcsmiltypes-4.1.429.jar ]; then
    		echo "SMIL Types library ready"
    	else
    		echo "Downloading SMIL Types library ..."
    		cd $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib
    		wget -O dlgcsmiltypes-4.1.429.jar https://www.dialogic.com/files/jsr-309/4.1/dlgcsmiltypes-4.1.429.jar
		fi

		if [ -f $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib/msmltypes-4.1.429.jar ]; then
    		echo "MSML Types library ready"
    	else
    		echo "Downloading SMIL Types library ..."
    		cd $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/lib
    		wget -O msmltypes-4.1.429.jar https://www.dialogic.com/files/jsr-309/4.1/msmltypes-4.1.429.jar
		fi
	fi
}

#MAIN
echo "Configuring Dialogic XMS...MS_MODE: $MS_COMPATIBILITY_MODE"
activateXMS $MS_ADDRESS
fetchExternalResources
echo '...finished configuring Dialogic XMS!'
