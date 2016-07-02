#!/bin/bash
##
## Description: Configures RestComm && RMS logs level
## Authos: Lefteris Banos (eleftherios.banos@telestax.com)

# VARIABLES
RESTCOMM_BIN=$RESTCOMM_HOME/bin
RESTCOMM_CONF=$RESTCOMM_HOME/standalone/configuration

configure_RC_component_log(){
    sed -i '' "/<logger category=\"${1}\">/ {
    N; s|<level name=\".*\"\/>|<level name=\"${2}\"\/>|
    }" $RESTCOMM_CONF/standalone-sip.xml
}

configure_RC_logs(){
    sed -i '' "s|<logger category=\"org.mobicents.servlet.sip\">|<logger category=\"org.mobicents.servlet\">|" $RESTCOMM_CONF/standalone-sip.xml

    sed -i '' "/ <console-handler name=\"CONSOLE\">/ {
    N; s|<level name=\".*\"/>|<level name=\"${LOG_LEVEL}\"/>|
	}" $RESTCOMM_CONF/standalone-sip.xml
}

configure_RMS_log(){
    FILE=$MMS_HOME/conf/log4j.xml
    sed -i '' "s|<param name=\"Threshold\" value=\".*\" />|<param name=\"Threshold\" value=\"${LOG_LEVEL}\" />|"  $FILE
    sed -i '' "s|<priority value=\".*\"/>|<priority value=\"${LOG_LEVEL}\"/>|"  $FILE
}

config_on_thefly(){
    FILE=$RESTCOMM_BIN/restcomm/set-log-level.sh
    if [ -n "$GRAYLOG_SERVER" ]; then
        sed -i '' "s|jboss-cli.sh --connect controller=.*|jboss-cli.sh --connect controller=$BIND_ADDRESS --file=\"\$CLIFILE\"|" $FILE
    else
        sed -i '' "s|jboss-cli.sh --connect controller=.*|jboss-cli.sh --connect controller=127.0.0.1 --file=\"\$CLIFILE\"|" $FILE
    fi
}

config_AKKA_logs(){
    FILE=$RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf
    echo "Update AKKA log level to ${AKKA_LOG_LEVEL}"
    sed -i '' "s|loglevel = \".*\"|loglevel = \"${AKKA_LOG_LEVEL}\"|" $FILE
    sed -i '' "s|stdout-loglevel = \".*\"|stdout-loglevel = \"${AKKA_LOG_LEVEL}\"|" $FILE
}


#MAIN
if [ -n "$LOG_LEVEL" ]; then
    configure_RMS_log
    configure_RC_logs
    config_on_thefly
    config_AKKA_logs
    for i in $( set -o posix ; set | grep ^LOG_LEVEL_COMPONENT_ | sort -rn ); do
        component=$(echo ${i} | cut -d = -f1 | cut -d _ -f4 )
        level=$(echo ${i} | cut -d = -f2)
        case "$component" in
            SIPSERVLET)
                COMPONENT=org.mobicents.servlet
                ;;
            GOVNIST)
                COMPONENT=gov.nist
                ;;
            *)
                echo "$component not possible to configure need to add it."
                continue
            esac

        echo "Configuring log level for: $component -> $level"
        configure_RC_component_log "$COMPONENT" "$level"
     done
 fi


