export JBOSS_CONFIG=standalone

echo "Restcomm server location $RESTCOMM_HOME"

echo "Update RestComm log level to WARN"
sed -i 's/INFO/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml
sed -i 's/ERROR/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml
sed -i 's/DEBUG/WARN/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml

echo "Update RestComm JVM Heap size options"
sed -i 's/Xms64m/Xms2048m/g' $RESTCOMM_HOME/bin/standalone.conf
sed -i 's/XX:MaxPermSize=256m/XX:MaxPermSize=512m/g' $RESTCOMM_HOME/bin/standalone.conf
sed -i 's/Xmx512m/Xmx8192m -Xmn512m -XX:+UseG1GC -XX:ParallelGCThreads=8 -XX:ConcGCThreads=8 -XX:G1RSetUpdatingPauseTimePercent=10 -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=4m -XX:G1HeapWastePercent=5 -XX:InitiatingHeapOccupancyPercent=85 -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85 -XX:+AlwaysPreTouch -XX:+UseCompressedOops -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000/g' $RESTCOMM_HOME/bin/standalone.conf

echo "Update MMS JVM Heap size options"
sed -i 's/JAVA_OPTS="$JAVA_OPTS.*/JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true -Xmx8192m -Xmn512m -XX:MaxPermSize=512m -XX:+UseG1GC -XX:ParallelGCThreads=8 -XX:ConcGCThreads=8 -XX:G1RSetUpdatingPauseTimePercent=10 -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=4m -XX:G1HeapWastePercent=5 -XX:InitiatingHeapOccupancyPercent=85 -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85 -XX:+AlwaysPreTouch -XX:+UseCompressedOops -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"/g' $RESTCOMM_HOME/mediaserver/bin/run.sh

echo "Update MMS log level to WARN"
sed -i 's/INFO/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml
sed -i 's/ERROR/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml
sed -i 's/DEBUG/WARN/g' $RESTCOMM_HOME/mediaserver/conf/log4j.xml

# echo "Update AKKA log level to OFF"
sed -i 's/INFO/OFF/g' $RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf

echo "Update MGCP Timeout"
sed -i 's/<response-timeout>.*<\/response-timeout>/<response-timeout>1000<\/response-timeout>/g' $RESTCOMM_HOME/$JBOSS_CONFIG/deployments/restcomm.war/WEB-INF/conf/restcomm.xml

echo "Update mss-sip-stack.properties"
sed -i 's/gov.nist.javax.sip.THREAD_POOL_SIZE=.*/gov.nist.javax.sip.THREAD_POOL_SIZE=1024/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/mss-sip-stack.properties
sed -i 's/gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE=.*/gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE=262144/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/mss-sip-stack.properties
sed -i 's/gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE=.*/gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE=262144/g' $RESTCOMM_HOME/$JBOSS_CONFIG/configuration/mss-sip-stack.properties

if [ -n "$VOICERSS" ]; then
  #Add the VOICERSS key to restcomm.conf
  echo "Updating Restcomm conf for VoiceRSS key"
  sed -i "s/VOICERSS_KEY='.*'/VOICERSS_KEY='$VOICERSS'/g" $RESTCOMM_HOME/bin/restcomm/restcomm.conf
else
  sed -i "s/VOICERSS_KEY='.*'/VOICERSS_KEY=''/g" $RESTCOMM_HOME/bin/restcomm/restcomm.conf
fi
