#!/usr/bin/env bash

source /etc/container_environment.sh

BASEDIR=/opt/Restcomm-JBoss-AS7


if [ -n "$RCADVCONF_GRAYLOG_SERVER" ]; then
   #Modify the Script for Graylog to docker needs.
   sed -i "s|hdusage=.*|hdusage=\$\(df -h \| grep /dev/xvda1 \| awk -F \" \" '{print \$5}'\)|" $BASEDIR/bin/restcomm/monitoring/Graylog_Monitoring.sh
   sed -i "s|cut -d \" \" -f 1|cut -d \" \" -f 2|" $BASEDIR/bin/restcomm/monitoring/Graylog_Monitoring.sh
   sed -i "s|cut -f3,4,5,6,7,8 -d ' '|cut -f4,5,6,7,8,9 -d ' '|" $BASEDIR/bin/restcomm/monitoring/Graylog_Monitoring.sh
fi

chmod +x $BASEDIR/bin/*.sh
chmod +x $BASEDIR/bin/restcomm/*.sh
chmod +x $BASEDIR/bin/restcomm/monitoring/*.sh
chmod +x /opt/embed/*.sh
mkdir -p "${RESTCOMM_LOGS}"/opt/
cp /tmp/version "${RESTCOMM_LOGS}"
cp /opt/embed/dockercleanup.sh  "${RESTCOMM_LOGS}"/opt/
cp /opt/embed/restcomm_docker.sh  "${RESTCOMM_LOGS}"/opt/

# Ensure cron is allowed to run"
sed -i 's/^\(session\s\+required\s\+pam_loginuid\.so.*$\)/# \1/g' /etc/pam.d/cron

mkdir -p /etc/service/restcomm
mv /tmp/start-restcomm.sh $BASEDIR/bin/restcomm/start-restcomm.sh
chmod +x $BASEDIR/bin/restcomm/start-restcomm.sh
mv /tmp/restcomm_service.sh /etc/service/restcomm/run
chmod +x /etc/service/restcomm/run

mkdir -p /etc/sv/rms
mv /tmp/start-mediaserver.sh $BASEDIR/mediaserver/start-mediaserver.sh
chmod +x $BASEDIR/mediaserver/start-mediaserver.sh
mv /tmp/rms_service.sh /etc/sv/rms/run
chmod +x /etc/sv/rms/run

echo "RestComm configured Properly!"

#auto delete script after run once. No need more.
rm -- "$0"