#! /bin/bash
##
## Description: Configures JBoss AS
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
##

## FUNCTIONS
disableSplashScreen() {
	FILE="$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml"
	sed -e 's|enable-welcome-root=".*"|enable-welcome-root="false"|' $FILE > $FILE.bak
	mv -f $FILE.bak $FILE
}

## MAIN
echo 'Configuring JBoss AS...'
disableSplashScreen
echo '...disabled JBoss splash screen...'
echo 'Finished configuring JBoss AS!'