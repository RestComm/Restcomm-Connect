#!/bin/bash
##
## Logging configuration for RVD
## 
## standalone-sip.xml is updated with RVD handler and loggers configuration. By default, if the respective
## configuration is missing it is added. Otherwise nothing happens. If the logging LEVEL is specified in the
## command line loggers are updated/created accordingly.
##
## usage:
##
## 	./config-rvd-logging.sh	 - adds handler logger (INFO) elements if missing
##	./config-rvd-logging.sh DEBUG  - creates or updates loggers by setting level to DEBUG
##
## environment:
##
##	requires RESTCOMM_HOME variable to be set
##
## Author: otsakir@gmail.com - Orestis Tsakiridis

# Default values
STANDALONE_SIP=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml
LOG_FILE="rvd/rvd.log"; # this is relative to "jboss.server.log.dir"
RVD_LOG_LEVEL=INFO # logging level that will be used if handlers/loggers are missing
LOGGING_HANDLER=RVD # the handler to be used for RVD logging. Set this to 'FILE' to redirect all messages to the main restcomm log (server.log)

# Variables 
XML_UPDATED=false # flag to format xml file only if updated
OVERRIDE=false

error(){
    echo "error parsing standalone-sip.xml"
    exit 1
}

createHandler(){

    # create the RVD handler if it is missing
    xmlstarlet sel -Q -N logns=urn:jboss:domain:logging:1.2 -t -m "//logns:periodic-rotating-file-handler[@name='RVD']" -o "found" $STANDALONE_SIP 
    result=$?
    if [ "$result" -eq 1 ]; then
	echo "adding RVD handler"
	xmlstarlet ed -P -N domainns=urn:jboss:domain:1.4 -N logns=urn:jboss:domain:logging:1.2 -d "//logns:periodic-rotating-file-handler[@name='RVD']" -s "//logns:subsystem" -t elem -n periodic-rotating-file-handler  \
	--var handler-field '$prev' \
	-i '$handler-field' -t attr -n name -v RVD \
	-i '$handler-field' -t attr -n autoflush -v true \
	-s '$handler-field' -t elem -n formatter --var formatter-field '$prev' \
	-s '$formatter-field' -t elem -n pattern-formatter --var pattern-formatter-field '$prev' \
	-i '$pattern-formatter-field' -t attr -n pattern -v "%d{MMdd HH:mm:ss,SSS X} %p (%t) %m %n" \
	-s '$handler-field' -t elem -n file --var file-field '$prev' \
	-i '$file-field' -t attr -n relative-to -v "jboss.server.log.dir" \
	-i '$file-field' -t attr -n path -v "rvd/rvd.log" \
	-s '$handler-field' -t elem -n suffix --var suffix-field '$prev' \
	-s '$suffix-field' -t attr -n value -v ".yyyy-MM-dd" \
	-s '$handler-field' -t elem -n append --var append-field '$prev' \
	-s '$append-field' -t attr -n value -v true \
	$STANDALONE_SIP > ${STANDALONE_SIP}_tmp
	mv ${STANDALONE_SIP}_tmp $STANDALONE_SIP
        XML_UPDATED=true
    else 
        if [ "$result" -eq 3 ];
        then
            error
        fi
    fi

}

createLoggers(){

    # create RVD local logger if it is missing
    xmlstarlet sel -Q -N logns=urn:jboss:domain:logging:1.2 -t -m "//logns:logger[@category='org.restcomm.connect.rvd.LOCAL']" -o "found" $STANDALONE_SIP 
    result=$?
    if [ "$result" -eq 1 -o \( "$result" = 0 -a "$OVERRIDE" = true \) ]; then
	echo "adding RVD local logger - $RVD_LOG_LEVEL/$LOGGING_HANDLER handler"
	xmlstarlet ed -P -N domainns=urn:jboss:domain:1.4 -N logns=urn:jboss:domain:logging:1.2 -d "//logns:logger[@category='org.restcomm.connect.rvd.LOCAL']" -s "//logns:subsystem" -t elem -n logger --var local-logger-field '$prev' \
	-i '$local-logger-field' -t attr -n category -v "org.restcomm.connect.rvd.LOCAL" \
	-s '$local-logger-field' -t elem -n level --var level-field '$prev' \
	-i '$level-field' -t attr -n name -v "$RVD_LOG_LEVEL" \
	-s '$local-logger-field' -t elem -n handlers --var handlers-field '$prev' \
	-s '$handlers-field' -t elem -n handler --var handler-field '$prev' \
	-s '$handler-field' -t attr -n name -v "$LOGGING_HANDLER" \
	$STANDALONE_SIP > ${STANDALONE_SIP}_tmp
	mv ${STANDALONE_SIP}_tmp $STANDALONE_SIP
        XML_UPDATED=true
    else
        if [ "$result" -eq 3 ];
        then
            error
        fi
    fi

    # create RVD global logger if it is missing
    xmlstarlet sel -Q -N logns=urn:jboss:domain:logging:1.2 -t -m "//logns:logger[@category='org.restcomm.connect.rvd.GLOBAL']" -o "found" $STANDALONE_SIP 
    result=$?
    if [ "$result" -eq 1 -o \( "$result" = 0 -a "$OVERRIDE" = true \) ]; then
	echo "adding RVD global logger - $RVD_LOG_LEVEL/$LOGGING_HANDLER handler"
	xmlstarlet ed -P -N domainns=urn:jboss:domain:1.4 -N logns=urn:jboss:domain:logging:1.2 -d "//logns:logger[@category='org.restcomm.connect.rvd.GLOBAL']" -s "//logns:subsystem" -t elem -n logger --var local-logger-field '$prev' \
	-i '$local-logger-field' -t attr -n category -v "org.restcomm.connect.rvd.GLOBAL" \
	-s '$local-logger-field' -t elem -n level --var level-field '$prev' \
	-i '$level-field' -t attr -n name -v "$RVD_LOG_LEVEL" \
	-s '$local-logger-field' -t elem -n handlers --var handlers-field '$prev' \
	-s '$handlers-field' -t elem -n handler --var handler-field '$prev' \
	-s '$handler-field' -t attr -n name -v "$LOGGING_HANDLER" \
	$STANDALONE_SIP > ${STANDALONE_SIP}_tmp
	mv ${STANDALONE_SIP}_tmp $STANDALONE_SIP
        XML_UPDATED=true
    else
        if [ "$result" -eq 3 ];
        then
            error
        fi
    fi
}

formatXml(){
    tmpfile=$(mktemp)
    xmlstarlet fo "$STANDALONE_SIP" > "$tmpfile"
    mv "$tmpfile" "$STANDALONE_SIP" 
}

# MAIN

if [ -z "$RESTCOMM_HOME" ]
then
    echo "RESTCOMM_HOME env variable not set"
    exit 1
fi

echo "Configuring RVD logging"

# if no (level) argument is given, create default loggers only if they are missing
if [ -z $1 ]
then
    # OVERRIDE is false
    createHandler
    createLoggers
else
    case "$1" in
        FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL|OFF)
            # specify logging handler to be used - RVD|FILE)
            if [ -z "$2" ]
            then
                LOGGING_HANDLER=RVD
            else
                case "$2" in
                    RVD|FILE)
                        LOGGING_HANDLER=$2
                    ;;
                    *)
                        echo "invalid arguments: handler should be one of RVD|FILE"
                        exit 1
                    ;;
                esac
            fi    
            RVD_LOG_LEVEL=$1
            OVERRIDE=true
            createHandler
            createLoggers
        ;;
        *) 
            echo "invalid arguments: level should be one of FATAL|ERROR|WARN|INFO|DEBUG|TRACE|ALL|OFF"
            exit 1
        ;;
    esac

fi

# format output if any update happened
if [ "$XML_UPDATED" = true ]
then
    formatXml
    echo "$STANDALONE_SIP updated"
fi

