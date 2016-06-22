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

    if [  "${ACTIVATE_S3_BUCKET^^}" = "TRUE"  ]; then
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
            sed -i "s|<bucket-region>.*</bucket-region>|<bucket-region>${S3_BUCKET_REGION}</bucket-region>|" $FILE > $FILE.bak;
            mv $FILE.bak $FILE
        fi
    fi
}

setINITPassword(){
    if [ -n "$INIT_PASSWORD" ]; then
        # chnange admin password
         SQL_FILE=$RESTCOMM_DEPLOY/WEB-INF/data/hsql/restcomm.script
         PATTERN="'ACae6e420f425248d6a26948c17a9e2acf','2012-04-24 00:00:00.000000000','2012-04-24 00:00:00.000000000','administrator@company.com',\
'Default Administrator Account',NULL,'Full','uninitialized','77f8c12cc7b8f8423e5c38b035249166','Administrator','/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf'"

        if grep -q "$PATTERN" $SQL_FILE; then
           PASSWORD_ENCRYPTED=`echo -n "${INIT_PASSWORD}" | md5sum |cut -d " " -f1`
            #echo "Update password to ${INIT_PASSWORD}($PASSWORD_ENCRYPTED)"
            sed -i "s/uninitialized/active/g" $SQL_FILE
            sed -i "s/77f8c12cc7b8f8423e5c38b035249166/$PASSWORD_ENCRYPTED/g" $SQL_FILE
            sed -i "s/2012-04-24 00:00:00.000000000/`echo "$(date +'%Y-%m-%d %H:%M:%S.%N')"`/" $SQL_FILE
            sed -i "s/2012-04-24 00:00:00.000000000/`echo "$(date +'%Y-%m-%d %H:%M:%S.%N')"`/" $SQL_FILE
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
            sed -i "/<smtp-notify>/ {
            N; s|<host>.*</host>|<host>`echo $SMTP_HOST`</host>|
            N; s|<user>.*</user>|<user>`echo $SMTP_USER`</user>|
            N; s|<password>.*</password>|<password>`echo $SMTP_PASSWORD`</password>|
            }" $FILE

            sed -i "/<smtp-service>/ {
            N; s|<host>.*</host>|<host>`echo $SMTP_HOST`</host>|
            N; s|<user>.*</user>|<user>`echo $SMTP_USER`</user>|
            N; s|<password>.*</password>|<password>`echo $SMTP_PASSWORD`</password>|
            }" $FILE
    fi
}


# MAIN
configS3Bucket
setINITPassword
configSMTP