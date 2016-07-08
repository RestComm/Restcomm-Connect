#!/bin/bash
##
## Description: Configures RestComm
## Author: Henrique Rosa (henrique.rosa@telestax.com)
## Author: Pavel Slegr (pavel.slegr@telestax.com)
## Authos: Lefteris Banos (eleftherios.banos@telestax.com)

# VARIABLES
RESTCOMM_BIN=$RESTCOMM_HOME/bin
RESTCOMM_DEPLOY=$RESTCOMM_HOME/standalone/deployments/restcomm.war

configS3Bucket() {
    FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml

    if [[ "$ACTIVATE_S3_BUCKET" == "true" || "$ACTIVATE_S3_BUCKET" == "TRUE" ]]; then
        echo "S3_BUCKET_NAME $S3_BUCKET_NAME S3_ACCESS_KEY $S3_ACCESS_KEY S3_SECURITY_KEY $S3_SECURITY_KEY"
        sed -e "/<amazon-s3>/ {
            N; s|<enabled>.*</enabled>|<enabled>true</enabled>|
            N; s|<bucket-name>.*</bucket-name>|<bucket-name>${S3_BUCKET_NAME}</bucket-name>|
            N; s|<folder>.*</folder>|<folder>logs</folder>|
            N; s|<access-key>.*</access-key>|<access-key>${S3_ACCESS_KEY}</access-key>|
            N; s|<security-key>.*</security-key>|<security-key>${S3_SECURITY_KEY}</security-key>|
        }" $FILE > $FILE.bak;
        mv $FILE.bak $FILE

        if [ -n "$S3_BUCKET_REGION" ]; then
            echo "S3_BUCKET_REGION $S3_BUCKET_REGION"
            sed -e "s|<bucket-region>.*</bucket-region>|<bucket-region>${S3_BUCKET_REGION}</bucket-region>|" $FILE > $FILE.bak;
            mv $FILE.bak $FILE
        fi
    fi
}

initUserPassword(){
     SQL_FILE=$RESTCOMM_DEPLOY/WEB-INF/data/hsql/restcomm.script
    if [ -n "$INITIAL_ADMIN_USER" ]; then
        # change admin user
        if grep -q "uninitialized" $SQL_FILE; then
            echo "Update Admin user"
            sed -e "s/administrator@company.com/${INITIAL_ADMIN_USER}/g" $SQL_FILE > $SQL_FILE.bak
            mv $SQL_FILE.bak $SQL_FILE
        else
            echo "Adminitrator User Already changed"
        fi
    fi

    if [ -n "$INITIAL_ADMIN_PASSWORD" ]; then
        # change admin password
        if grep -q "uninitialized" $SQL_FILE; then
           PASSWORD_ENCRYPTED=`echo -n "${INITIAL_ADMIN_PASSWORD}" | md5sum |cut -d " " -f1`
            #echo "Update password to ${INITIAL_ADMIN_PASSWORD}($PASSWORD_ENCRYPTED)"
            sed -e "s/uninitialized/active/g" \
            sed -e "s/77f8c12cc7b8f8423e5c38b035249166/$PASSWORD_ENCRYPTED/g" \
            sed -e "s/2012-04-24 00:00:00.000000000/`echo "$(date +'%Y-%m-%d %H:%M:%S.%N')"`/" \
            sed -e "s/2012-04-24 00:00:00.000000000/`echo "$(date +'%Y-%m-%d %H:%M:%S.%N')"`/" $SQL_FILE > $SQL_FILE.bak
            mv $SQL_FILE.bak $SQL_FILE
        else
            echo "Adminitrator Password Already changed"
        fi
    fi
}

configSMTP(){
    FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
    if [[ -z $SMTP_USER || -z $SMTP_PASSWORD || -z $SMTP_HOST ]]; then
            echo 'one or more variables are undefined'
            echo  'Not possible to continue with SMTP configuration'

    else
        echo "SMTP_USER $SMTP_USER SMTP_PASSWORD $SMTP_PASSWORD SMTP_HOST $SMTP_HOST"
        sed -e "/<smtp-notify>/ {
            N; s|<host>.*</host>|<host>${SMTP_HOST}</host>|
            N; s|<user>.*</user>|<user>${SMTP_USER}</user>|
            N; s|<password>.*</password>|<password>${SMTP_PASSWORD}</password>|
            N; s|<port>.*</port>|<port>${SMTP_PORT}</port>|
            }" $FILE > $FILE.bak
            mv  $FILE.bak $FILE

        sed -e "/<smtp-service>/ {
            N; s|<host>.*</host>|<host>${SMTP_HOST}</host>|
            N; s|<user>.*</user>|<user>${SMTP_USER}</user>|
            N; s|<password>.*</password>|<password>${SMTP_PASSWORD}</password>|
            N; s|<port>.*</port>|<port>${SMTP_PORT}</port>|
            }" $FILE > $FILE.bak
            mv  $FILE.bak $FILE
    fi
}

configMonitoring(){

    if hash crontab 2>/dev/null; then
        echo "Ok crontab installed. Can proceed with monitoring configuration"
    else
        echo "INFO: \"crontab\" programm does not exist ('dnsutils' package) please make sure that crontab is installed or disable Graylog configuration."
        return 0
    fi

    if [ -z ${GRAYLOG_SERVER} ]; then
        echo "Graylog Monitoring is not configured";
        FILE=mycron

        crontab -l 2>/dev/null > $FILE
        crontab -l | grep -q 'HDmonitor' && sed -e '/HDmonitor/d' $FILE > $FILE.new
        crontab -l | grep -q 'RMSJVMonitor' && sed -e '/RMSJVMonitor/d' $FILE.new > $FILE
        crontab -l | grep -q 'RCJVMonitor' && sed -e '/RCJVMonitor/d' $FILE > $FILE.new
        crontab -l | grep -q 'SERVERAMonitor' && sed -e '/SERVERAMonitor/d' $FILE.new > $FILE
        #install new cron file
        crontab $FILE
        rm $FILE
    else
        echo "GRAYLOG_SERVER is: $GRAYLOG_SERVER";
         FILE=mycron
        #write out current crontab RMSJVMonitor
        crontab -l 2>/dev/null > $FILE

        #echo new cron into cron file
        crontab -l | grep -q 'MAILTO=""'  && echo 'entry exists' || echo "MAILTO=\"\"" >> $FILE
        if [[ "$HD_MONITOR" == "false" || "$HD_MONITOR" == "FALSE" ]]; then
            sed -e '/HDmonitor/d' mycron > $FILE.bak
            mv $FILE.bak $FILE
            echo "HD_MONITOR: $HD_MONITOR"
        else
            crontab -l | grep -q 'Graylog_Monitoring.sh HDmonitor' && echo 'entry exists' || echo "*/30 * * * * $RESTCOMM_BIN/restcomm/monitoring/Graylog_Monitoring.sh HDmonitor" >> $FILE;
        fi

        if [[ "$RMSJVM_MONITOR" == "false" || "$RMSJVM_MONITOR" == "FALSE" ]]; then
            sed -e '/RMSJVMonitor/d' $FILE > $FILE.bak
            mv $FILE.bak $FILE
            echo "RMSJVM_MONITOR: $RMSJVM_MONITOR";
        else
            crontab -l | grep -q 'Graylog_Monitoring.sh RMSJVMonitor' && echo 'entry exists' || echo "* * * * * $RESTCOMM_BIN/restcomm/monitoring/Graylog_Monitoring.sh RMSJVMonitor" >> $FILE;
        fi

        if [[ "$RCJVM_MONITOR" == "false" || "$RCJVM_MONITOR" == "FALSE" ]]; then
            sed -e '/RCJVMonitor/d' $FILE > $FILE.bak
            mv $FILE.bak $FILE
            echo "RCJVM_MONITOR: $RCJVM_MONITOR";
        else
            crontab -l | grep -q 'Graylog_Monitoring.sh RCJVMonitor' && echo 'entry exists' || echo "* * * * * $RESTCOMM_BIN/restcomm/monitoring/Graylog_Monitoring.sh RCJVMonitor" >> $FILE;
        fi

        if [[ "$RAM_MONITOR" == "false" || "$RAM_MONITOR" == "FALSE" ]]; then
            sed -e '/SERVERAMonitor/d' $FILE > $FILE.bak
            mv $FILE.bak $FILE
            echo "RAM_MONITOR: $RAM_MONITOR";
        else
            crontab -l | grep -q 'Graylog_Monitoring.sh SERVERAMonitor' && echo 'entry exists' || echo "* * * * * $RESTCOMM_BIN/restcomm/monitoring/Graylog_Monitoring.sh SERVERAMonitor" >> $FILE;
        fi

        #install new cron file
        crontab $FILE
        rm $FILE

        #set Server Label
        FILE=$RESTCOMM_BIN/restcomm/monitoring/Graylog_Monitoring.sh;
        sed -e "s|SERVERLABEL=.*|SERVERLABEL=\"${SERVERLABEL}\"|" $FILE > $FILE.bak
        sed -e "s|GRAYLOG_SERVER=.*|GRAYLOG_SERVER=\"${GRAYLOG_SERVER}\"|" $FILE.bak > $FILE
     fi
}

# MAIN
configS3Bucket
initUserPassword
configSMTP
configMonitoring