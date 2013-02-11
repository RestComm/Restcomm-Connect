#!/bin/bash
echo "=============================================================================="
echo "==                   RestComm Update Uitility [Started]                     =="
echo "==                                  - . -                                   =="
echo "==              Thank you for running Mobicents Community code              =="
echo "==   For Commercial Grade Support, please request a TelScale Subscription   =="
echo "==                         http://www.telestax.com/                         =="
echo "=============================================================================="

# Save our workspace path
WORKSPACE_PATH="/tmp"

# Save our script's location.
if [ -L $0 ] ; then
    SCRIPT_PATH=$(dirname $(readlink -f $0)) ;
else
    SCRIPT_PATH=$(dirname $0) ;
fi

# Save our original install location.
TOMCAT_PATH=`/bin/readlink -f $SCRIPT_PATH/..`

# Move to our workspace.
echo "Moving to the workspace located @ $WORKSPACE_PATH"
cd $WORKSPACE_PATH

# Backup our old installation.
echo "Backing up the current installation located @ $TOMCAT_PATH."
tar -czf restcomm-backup.tar.gz $TOMCAT_PATH

# Download the latest build of RestComm.
echo "Downloading the latest build. This may take a while."
wget -q https://mobicents.ci.cloudbees.com/job/RestComm/lastSuccessfulBuild/artifact/restcomm-saas-tomcat-1.0.0.CR1-SNAPSHOT.zip

# Unpack RestComm.
echo "Unpacking RestComm."
unzip -q restcomm-saas-tomcat-1.0.0.CR1-SNAPSHOT.zip
mv restcomm-saas-tomcat-1.0.0.CR1-SNAPSHOT restcomm

# Copy configuration files from the old installation.
echo "Configuring RestComm."
cp $TOMCAT_PATH/conf/server.xml restcomm/conf/server.xml
cp $TOMCAT_PATH/mobicents-media-server/conf/log4j.xml restcomm/mobicents-media-server/conf/log4j.xml
cp $TOMCAT_PATH/mobicents-media-server/deploy/server-beans.xml restcomm/mobicents-media-server/deploy/server-beans.xml
cp $TOMCAT_PATH/conf/logging.properties restcomm/conf/logging.properties
cp $TOMCAT_PATH/webapps/restcomm/WEB-INF/conf/restcomm.xml restcomm/webapps/restcomm/WEB-INF/conf/restcomm.xml

# Copy HSQL data files from the old installation.
cp $TOMCAT_PATH/webapps/restcomm/WEB-INF/data/hsql/* restcomm/webapps/restcomm/WEB-INF/data/hsql/

# Copy media data from the old installation.
echo "Updating media data."
cp -r $TOMCAT_PATH/webapps/restcomm/cache/acapela/ restcomm/webapps/restcomm/cache/acapela/
cp -r $TOMCAT_PATH/webapps/restcomm/cache/ttsapi/ restcomm/webapps/restcomm/cache/ttsapi/
cp -r $TOMCAT_PATH/webapps/restcomm/recordings/ restcomm/webapps/restcomm/recordings/

# Finish
cp restcomm-backup.tar.gz restcomm/
rm -r $TOMCAT_PATH
mv restcomm $TOMCAT_PATH

# Cleanup!
rm -r restcomm*

echo "Thank you for updating to the latest RestComm build."

