#!/bin/bash

BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
source ../$BASEDIR/advanced.conf

if [ -z ${GRAYLOG_SERVER+x} ]; then
    echo "Graylog Monitoring is not configured";
else
    echo "GRAYLOG_SERVER is: $GRAYLOG_SERVER";

    #write out current crontab
    crontab -l 2>/dev/null; > mycron

    #echo new cron into cron file
    crontab -l | grep -q 'MAILTO=""'  && echo 'entry exists' || echo "MAILTO=""" >> mycron
    if [ "${HD_MONITOR^^}" = "TRUE" ]; then echo "HD_MONITOR: $HD_MONITOR"; else crontab -l | grep -q 'Graylog_Monitoring.sh HDmonitor' && echo 'entry exists' || echo "0/30 * * * * $BASEDIR/Graylog_Monitoring.sh HDmonitor" >> mycron; fi
    if [ "${RMSJVM_MONITOR^^}" = "TRUE" ]; then echo "RMSJVM_MONITOR: $RMSJVM_MONITOR"; else crontab -l | grep -q 'Graylog_Monitoring.sh  RMSJVMonitor'  && echo 'entry exists' || echo "* * * * * $BASEDIR/Graylog_Monitoring.sh  RMSJVMonitor" >> mycron; fi
    if [ "${RCJVM_MONITOR^^}" = "TRUE" ]; then echo "RCJVM_MONITOR: $RCJVM_MONITOR"; else crontab -l | grep -q 'Graylog_Monitoring.sh  RCJVMonitor'  && echo 'entry exists' || echo "* * * * * $BASEDIR/Graylog_Monitoring.sh  RCJVMonitor" >> mycron; fi
    if [ "${RAM_MONITOR^^}" = "TRUE" ]; then echo "RAM_MONITOR: $RAM_MONITOR"; else crontab -l | grep -q 'Graylog_Monitoring.sh SERVERAMonitor'  && echo 'entry exists' || echo "* * * * * $BASEDIR/Graylog_Monitoring.sh SERVERAMonitor" >> mycron; fi
    #install new cron file
    crontab mycron
    rm mycron
 fi

