export RESTCOMM_HOME=
export JBOSS_CONFIG=standalone

echo "Update RestComm log level to WARN"
sed -i 's/INFO/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml
sed -i 's/ERROR/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml
sed -i 's/DEBUG/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml

echo "Update RestComm JVM Heap size options"
sed -i 's/Xms64m/Xms2048m/g' $RESTCOMM_HOME/bin/standalone.conf
sed -i 's/Xmx512/Xmx8192m -Xmn512m -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=100 -XX:CMSIncrementalDutyCycleMin=100 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode/g' $RESTCOMM_HOME/bin/standalone.conf
sed -i 's/XX:MaxPermSize=256m/XX:MaxPermSize=512m/g' $RESTCOMM_HOME/bin/standalone.conf

echo "Update MMS JVM Heap size options"
sed -i 's/java.net.preferIPv4Stack=true/java.net.preferIPv4Stack=true -Xmx8192m -Xmn512m -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=100 -XX:CMSIncrementalDutyCycleMin=100 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:MaxPermSize=512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000/g' $RESTCOMM_HOME/mediaserver/bin/run.sh

echo "Update MMS log level to WARN"
sed -i 's/INFO/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml
sed -i 's/ERROR/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml
sed -i 's/DEBUG/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml

echo "Update AKKA log level to OFF"
sed -i 's/INFO/OFF/g' $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf
