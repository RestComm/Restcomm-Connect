#!/bin/bash
##
## Description: Configures RestComm && RMS logs level
## Authos: Lefteris Banos (eleftherios.banos@telestax.com)

# VARIABLES
RESTCOMM_BIN=$RESTCOMM_HOME/bin
RESTCOMM_CONF=$RESTCOMM_HOME/standalone/configuration


check_if_logger_exist(){
FILE=$RESTCOMM_CONF/standalone-sip.xml
 #delete additional bindings if any added to erlier run of the script.
  echo
   if grep -q "${1}"  $FILE
    then
         echo "Logger exist"
    else
	 echo "Need to add logger ${1}"
         sed -i "/<logger category=\"org.mobicents.servlet.sip\">/i <logger category=\"${1}\">\\n<level name=\"INFO\"/>\\n</logger>" $FILE
    fi

}

configure_RC_component_log(){
  check_if_logger_exist $1
  sed -i "/<logger category=\"${1}\">/ {N; s/<level name=\".*\"\/>/<level name=\"${2}\"\/>/}" $RESTCOMM_CONF/standalone-sip.xml
}

configure_RC_logs(){
    sed -i "/ <console-handler name=\"CONSOLE\">/ {
    N; s|<level name=\".*\"/>|<level name=\"${LOG_LEVEL}\"/>|
	}" $RESTCOMM_CONF/standalone-sip.xml
}

config_on_thefly(){
    FILE=$RESTCOMM_BIN/restcomm/set-log-level.sh
    MNGMTPORT=$((9999 + PORT_OFFSET))
    sed -i "s|jboss-cli.sh --connect controller=.*|jboss-cli.sh --connect controller=$BIND_ADDRESS:${MNGMTPORT} --file=\"\$CLIFILE\"|" $FILE
}

config_AKKA_logs(){
    FILE=$RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf
    echo "Update AKKA log level to ${AKKA_LOG_LEVEL}"
    sed -i "s|loglevel = \".*\"|loglevel = \"${AKKA_LOG_LEVEL}\"|" $FILE
    sed -i "s|stdout-loglevel = \".*\"|stdout-loglevel = \"${AKKA_LOG_LEVEL}\"|" $FILE
}


#MAIN
if [ -n "$LOG_LEVEL" ]; then
    configure_RC_logs
    config_on_thefly
    config_AKKA_logs
    for i in $( set -o posix ; set | grep ^LOG_LEVEL_COMPONENT_ | sort -rn ); do
        component=$(echo ${i} | cut -d = -f1 | cut -d _ -f4 )
        level=$(echo ${i} | cut -d = -f2)
        case "$component" in
            SIPSERVLET)
                COMPONENT=org.mobicents.servlet.sip
                ;;
            GOVNIST)
                COMPONENT=gov.nist
                ;;
            SIPRESTCOMM)
		        COMPONENT=org.mobicents.servlet.sip.restcomm
                ;;
	        RESTCOMM)
		        COMPONENT=org.restcomm.connect
                ;;
            *)
                echo "$component not possible to configure need to add it."
                continue
            esac

        echo "Configuring log level for: $component -> $level"
        configure_RC_component_log "$COMPONENT" "$level"
     done
 fi


