#!/usr/bin/env bash

source /etc/container_environment.sh

BASEDIR=/opt/Restcomm-JBoss-AS7
RESTCOMM_CORE_LOG=$BASEDIR/standalone/log
MMS_LOGS=$BASEDIR/mediaserver/log

#Patch provided for the RVD backup Directory for the migration process (in order to save the backup at the host need to  mount this directory e.g. -v host_path:RVD_MIGRATION_BACKUP).
if [ -n "$RVD_MIGRATION_BACKUP" ]; then
    echo "RVD_MIGRATION_BACKUP $RVD_MIGRATION_BACKUP"
    sed -i "s|<workspaceBackupLocation>.*</workspaceBackupLocation>|<workspaceBackupLocation>${RVD_MIGRATION_BACKUP}</workspaceBackupLocation>|" $BASEDIR/standalone/deployments/restcomm-rvd.war/WEB-INF/rvd.xml
fi

if [ -n "$PATCHRURI" ]; then
   echo "PATCHRURI $PATCHRURI"
   sed -i "s|<patch-for-nat-b2bua-sessions>true</patch-for-nat-b2bua-sessions>|<patch-for-nat-b2bua-sessions>${PATCHRURI}</patch-for-nat-b2bua-sessions>|" $BASEDIR/standalone/deployments/restcomm.war/WEB-INF/conf/restcomm.xml
fi

if [[ "$ALERTING" != "TRUE" ]]; then
    echo "ALERTING FALSE"

    FILE=/etc/cron.d/graylog_cron
    if [ -f $FILE ]; then
         rm  $FILE
    fi
fi

#logs configuration
if [ -n "$RESTCOMM_LOGS" ]; then
  echo "RESTCOMM_LOGS $RESTCOMM_LOGS"
  sed -i "s|BASEDIR=.*| |" $BASEDIR/bin/restcomm/logs_collect.sh
  sed -i "s|./jvmtop.sh|${BASEDIR}/bin/restcomm/jvmtop.sh|" $BASEDIR/bin/restcomm/logs_collect.sh
  sed -i "s|LOGS_DIR_ZIP=.*|LOGS_DIR_ZIP=$RESTCOMM_LOGS/\$DIR_NAME|" $BASEDIR/bin/restcomm/logs_collect.sh
  sed -i "s|RESTCOMM_LOG_BASE=.*|RESTCOMM_LOG_BASE=${RESTCOMM_LOGS}|" /opt/embed/restcomm_docker.sh

  LOGS_LOCATE=$RESTCOMM_LOGS
  mkdir -p "$LOGS_LOCATE/extralogs"
  RESTCOMM_CORE_LOG=$LOGS_LOCATE
  MMS_LOGS=$LOGS_LOCATE
  LOGS_TRACE=$LOGS_LOCATE
fi

if [ -n "$CORE_LOGS_LOCATION" ]; then
  echo "CORE_LOGS_LOCATION $CORE_LOGS_LOCATION"
  mkdir -p  $RESTCOMM_CORE_LOG/$CORE_LOGS_LOCATION

  sed -i "s|find .*restcomm_core_|find $RESTCOMM_CORE_LOG/$CORE_LOGS_LOCATION/restcommCore-server.log|" /etc/cron.d/restcommcore-cron
  sed -i "s|<file relative-to=\"jboss.server.log.dir\" path=\".*\"\/>|<file path=\"$RESTCOMM_CORE_LOG/$CORE_LOGS_LOCATION/restcommCore-server.log\"\/>|" $BASEDIR/standalone/configuration/standalone-sip.xml
  #logs collect script conficuration
  sed -i "s/RESTCOMM_CORE_FILE=server.log/RESTCOMM_CORE_FILE=restcommCore-server.log/" /opt/Restcomm-JBoss-AS7/bin/restcomm/logs_collect.sh
  sed -i "s|RESTCOMM_CORE_LOG=.*|RESTCOMM_CORE_LOG=$RESTCOMM_CORE_LOG/$CORE_LOGS_LOCATION|" /opt/Restcomm-JBoss-AS7/bin/restcomm/logs_collect.sh
  sed -i "s|RESTCOMM_LOG_BASE=.*|RESTCOMM_LOG_BASE=$CORE_LOGS_LOCATION|" /opt/Restcomm-JBoss-AS7/bin/restcomm/logs_collect.sh

fi

if [ -n "$RESTCOMM_TRACE_LOG" ]; then
  echo "RESTCOMM_TRACE_LOG $RESTCOMM_TRACE_LOG"
  mkdir -p $LOGS_TRACE/$RESTCOMM_TRACE_LOG

  sed -i "s|find .*restcomm_trace_|find $LOGS_TRACE/$RESTCOMM_TRACE_LOG/restcomm_trace_|" /etc/cron.d/restcommtcpdump-cron
  sed -i "s|RESTCOMM_TRACE=.*|RESTCOMM_TRACE=\$RESTCOMM_LOG_BASE/$RESTCOMM_TRACE_LOG|"  /opt/embed/restcomm_docker.sh

  nohup xargs bash -c "tcpdump -pni any -t -n -s 0 -G 3500 -w $LOGS_TRACE/$RESTCOMM_TRACE_LOG/restcomm_trace_%Y-%m-%d_%H:%M:%S-%Z.pcap -z gzip" &

  #Used to start TCPDUMP when restarting container
    TCPFILE="/etc/my_init.d/restcommtrace.sh"
    cat <<EOT >> $TCPFILE
#!/bin/bash
     nohup xargs bash -c "tcpdump -pni any -t -n -s 0  -G 3500 -w $LOGS_TRACE/$RESTCOMM_TRACE_LOG/restcomm_trace_%Y-%m-%d_%H:%M:%S-%Z.pcap -z gzip" &
EOT
    chmod 777 $TCPFILE
    #rm /tmp/myports
fi

#Media-server Log configuration.
if [ -n "$MEDIASERVER_LOGS_LOCATION" ]; then
  echo "MEDIASERVER_LOGS_LOCATION $MEDIASERVER_LOGS_LOCATION"
  mkdir -p $MMS_LOGS/$MEDIASERVER_LOGS_LOCATION

  sed -i "s|find .*restcomm_ms_|find $MMS_LOGS/$MEDIASERVER_LOGS_LOCATION/media-server.log|" /etc/cron.d/restcommmediaserver-cron
  sed -i "s|LOG_FILE_URL=.*|LOG_FILE_URL=$MMS_LOGS/$MEDIASERVER_LOGS_LOCATION/media-server.log|"  $BASEDIR/bin/restcomm/mediaserver.conf

  #Daily log rotation for MS.
  sed -i "s|<appender name=\"FILE\" class=\"org\.apache\.log4j\.RollingFileAppender\"|<appender name=\"FILE\" class=\"org\.apache\.log4j\.DailyRollingFileAppender\"|"  $BASEDIR/mediaserver/conf/log4j.xml
  sed -i "s|<param name=\"Append\" value=\"false\"|<param name=\"Append\" value=\"true\"|"  $BASEDIR/mediaserver/conf/log4j.xml
  sed -i "s|<param name=\"File\" value=\".*\"|<param name=\"File\" value=\"$MMS_LOGS/$MEDIASERVER_LOGS_LOCATION/media-server.log\"|"  $BASEDIR/mediaserver/conf/log4j.xml
  #logs collect script conficuration
  sed -i "s|MEDIASERVER_FILE=server.log|MEDIASERVER_FILE=media-server.log|" $BASEDIR/bin/restcomm/logs_collect.sh
  sed -i "s|MMS_LOGS=.*|MMS_LOGS=$MMS_LOGS/$MEDIASERVER_LOGS_LOCATION|" $BASEDIR/bin/restcomm/logs_collect.sh
fi

if [ -n "$RVD_PORT" ]; then
RVD_DEPLOY=$BASEDIR/standalone/deployments/restcomm-rvd.war
    echo "RVD_PORT $RVD_PORI"
    if [[ "$DISABLE_HTTP" == "true" || "$DISABLE_HTTP" == "TRUE" ]]; then
        SCHEME='https'
    else
        SCHEME='http'
    fi
    #If used means that port mapping at docker (e.g: -p 445:443) is not the default (-p 443:443)
    sed -i "s|<restcommBaseUrl>.*</restcommBaseUrl>|<restcommBaseUrl>${SCHEME}://${STATIC_ADDRESS}:${RVD_PORT}/</restcommBaseUrl>|" $RVD_DEPLOY/WEB-INF/rvd.xml
fi



#auto delete script after run once. No need more.
rm -- "$0"