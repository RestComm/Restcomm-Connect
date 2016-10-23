export JBOSS_CONFIG=standalone

echo "Restcomm server location $RESTCOMM_HOME"

echo "Update RestComm log level to WARN"
FILE=$RESTCOMM_HOME/$JBOSS_CONFIG/configuration/standalone-sip.xml
sed -e "s|INFO|WARN|" \
	-e "s|ERROR|WARN|" \
	-e "s|DEBUG|WARN|" \
	$FILE > $FILE.bak;
mv $FILE.bak $FILE

echo "Update RestComm JVM Heap size options"
FILE=$RESTCOMM_HOME/bin/standalone.conf
sed -e "s|Xms64m|Xms2048m|" \
	-e "s|XX:MaxPermSize=256m|XX:MaxPermSize=512m|" \
	-e "s|Xmx512m|Xmx8192m -Xmn512m -XX:+UseG1GC -XX:ParallelGCThreads=8 -XX:ConcGCThreads=8 -XX:G1RSetUpdatingPauseTimePercent=10 -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=4m -XX:G1HeapWastePercent=5 -XX:InitiatingHeapOccupancyPercent=85 -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85 -XX:+AlwaysPreTouch -XX:+UseCompressedOops -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000|" \
	$FILE > $FILE.bak;
mv $FILE.bak $FILE

echo "Update MMS JVM Heap size options"
FILE=$RESTCOMM_HOME/mediaserver/bin/run.sh
sed -e "s|JAVA_OPTS=\"$JAVA_OPTS.*|JAVA_OPTS=\"$JAVA_OPTS -Djava.net.preferIPv4Stack=true -Xmx8192m -Xmn512m -XX:MaxPermSize=512m -XX:+UseG1GC -XX:ParallelGCThreads=8 -XX:ConcGCThreads=8 -XX:G1RSetUpdatingPauseTimePercent=10 -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=4m -XX:G1HeapWastePercent=5 -XX:InitiatingHeapOccupancyPercent=85 -XX:+UnlockExperimentalVMOptions -XX:G1MixedGCLiveThresholdPercent=85 -XX:+AlwaysPreTouch -XX:+UseCompressedOops -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000\"|" $FILE > $FILE.bak
mv $FILE.bak $FILE

echo "Update MMS log level to WARN"
FILE=$RESTCOMM_HOME/mediaserver/conf/log4j.xml
sed -e "s|INFO|WARN|" \
	-e "s|ERROR|WARN|" \
	-e "s|DEBUG|WARN|" \
	$FILE > $FILE.bak;
mv $FILE.bak $FILE

# echo "Update AKKA log level to OFF"
FILE=$RESTCOMM_HOME/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf
sed -e "s|INFO|OFF|" $FILE > $FILE.bak
mv $FILE.bak $FILE

echo "Update MGCP Timeout"
FILE=$RESTCOMM_HOME/$JBOSS_CONFIG/deployments/restcomm.war/WEB-INF/conf/restcomm.xml
sed -e "s|<response-timeout>.*<\/response-timeout>|<response-timeout>1000<\/response-timeout>|" $FILE > $FILE.bak
mv $FILE.bak $FILE

echo "Update mss-sip-stack.properties"
FILE=$RESTCOMM_HOME/$JBOSS_CONFIG/configuration/mss-sip-stack.properties
sed -e "s|gov.nist.javax.sip.THREAD_POOL_SIZE=.*|gov.nist.javax.sip.THREAD_POOL_SIZE=1024|" \
	-e "s|gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE=.*|gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE=262144|" \
	-e "s|gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE=.*|gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE=262144|" \
	$FILE > $FILE.bak;
mv $FILE.bak $FILE

FILE=$RESTCOMM_HOME/bin/restcomm/restcomm.conf
if [ -n "$VOICERSS" ]; then
  #Add the VOICERSS key to restcomm.conf
  echo "Updating Restcomm conf for VoiceRSS key"
  sed -e "s|VOICERSS_KEY='.*'|VOICERSS_KEY='$VOICERSS'|" $FILE > $FILE.bak
  mv $FILE.bak $FILE
else
  sed -e "s/VOICERSS_KEY='.*'|VOICERSS_KEY=''|" $FILE > $FILE.bak
  mv $FILE.bak $FILE
fi
