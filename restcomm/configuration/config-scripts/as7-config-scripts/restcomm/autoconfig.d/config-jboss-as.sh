#! /bin/bash
##
## Description: Configures JBoss AS
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
## Author     : Tsakiridis Orestis (orestis.tsakiridis@telestax.com)
##

## FUNCTIONS
disableSplashScreen() {
	FILE="$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml"
	sed -e 's|enable-welcome-root=".*"|enable-welcome-root="false"|' $FILE > $FILE.bak
	mv -f $FILE.bak $FILE
}

configMigrationLog() {
	FILE="$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml"
        sed -e '/<file-handler name="MIGRATION">/,/<\/file-handler>/ d' \
            -e '/<subsystem xmlns\="urn:jboss:domain:logging:1.2">/ a\
                <file-handler name\="MIGRATION"> \
                        <level name\="ALL"/> \
                        <file relative-to="jboss.server.base.dir" path="../migration.log"/> \
                </file-handler>' \
            -e '/<logger category\="org.mobicents.servlet.restcomm.identity.migration">/,/<\/logger>/ d' \
            -e '/<root-logger>/ i\
                <logger category\="org.mobicents.servlet.restcomm.identity.migration">\
                        <level name\="DEBUG"/>\
                        <handlers>\
                                <handler name\="MIGRATION"/>\
                        </handlers>\
                </logger>' $FILE > $FILE.bak
        mv $FILE.bak $FILE
        echo "Using migration log: " $RESTCOMM_HOME/migration.log
}

## MAIN
echo 'Configuring JBoss AS...'
disableSplashScreen
echo '...disabled JBoss splash screen...'
configMigrationLog
echo 'Finished configuring JBoss AS!'
