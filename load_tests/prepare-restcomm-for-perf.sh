export JBOSS_CONFIG=standalone

echo "About to prepare Restcomm server location $RESTCOMM_HOME"

echo "VOICERSS: $VOICERSS"
echo "LOCAL_INTERFACE: $LOCAL_INTERFACE"
echo "LOG_LEVEL: $LOG_LEVEL"

FILE=$RESTCOMM_HOME/bin/restcomm/restcomm.conf
MS_FILE=$RESTCOMM_HOME/bin/restcomm/mediaserver.conf

# VoiceRSS config
if [ -n "$VOICERSS" ]; then
  #Add the VOICERSS key to restcomm.conf
  echo "Updating Restcomm conf for VoiceRSS key"
  sed -e "s|VOICERSS_KEY='.*'|VOICERSS_KEY='$VOICERSS'|" $FILE > $FILE.bak
  mv $FILE.bak $FILE
else
  sed -e "s/VOICERSS_KEY='.*'|VOICERSS_KEY=''|" $FILE > $FILE.bak
  mv $FILE.bak $FILE
fi

# Ethernet Address config
if [ -n "LOCAL_INTERFACE" ]; then
	sed -e "s/NET_INTERFACE=.*/NET_INTERFACE='$LOCAL_INTERFACE'/" $FILE > $FILE.bak
	mv $FILE.bak $FILE
else
	sed -e "s/NET_INTERFACE=.*/NET_INTERFACE='eth0'/" $FILE > $FILE.bak
	mv $FILE.bak $FILE
fi

# Configure MGCP Timeout MGCP_RESPONSE_TIMEOUT
sed -e "s/MGCP_RESPONSE_TIMEOUT=.*/MGCP_RESPONSE_TIMEOUT=1000/" $FILE > $FILE.bak
mv $FILE.bak $FILE

# Configure log level
sed -e "s/LOG_LEVEL=.*/LOG_LEVEL=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

sed -e "s/AKKA_LOG_LEVEL=.*/AKKA_LOG_LEVEL=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

sed -e "s/LOG_LEVEL_COMPONENT_GOVNIST=.*/LOG_LEVEL_COMPONENT_GOVNIST=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

sed -e "s/LOG_LEVEL_COMPONENT_SIPSERVLET=.*/LOG_LEVEL_COMPONENT_SIPSERVLET=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

sed -e "s/LOG_LEVEL_COMPONENT_SIPRESTCOMM=.*/LOG_LEVEL_COMPONENT_SIPRESTCOMM=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

sed -e "s/LOG_LEVEL_COMPONENT_RESTCOMM=.*/LOG_LEVEL_COMPONENT_RESTCOMM=$LOG_LEVEL/" $FILE > $FILE.bak
mv $FILE.bak $FILE

FILE=$RESTCOMM_HOME/bin/standalone.conf
sed -e "s/$JAVA_OPTS -Djboss.server.default.config=standalone-sip.xml/$JAVA_OPTS -Djboss.server.default.config=standalone-sip.xml -Djboss.boot.thread.stack.size=1/" $FILE > $FILE.bak
mv $FILE.bak $FILE

# MEDIA SERVER
sed -e "s|BIND_ADDRESS=.*|BIND_ADDRESS=$RESTCOMM_ADDRESS|" \
    -e "s|NETWORK=.*|NETWORK=$RESTCOMM_NETWORK|" \
    -e "s|SUBNET=.*|SUBNET=$RESTCOMM_SUBNET|" \
    -e "s|MGCP_ADDRESS=.*|MGCP_ADDRESS=$RESTCOMM_ADDRESS|" \
    -e "s|EXPECTED_LOAD=.*|EXPECTED_LOAD=$SIMULTANEOUS_CALLS|" \
    -e "s|AUDIO_CACHE_SIZE=.*|AUDIO_CACHE_SIZE=$MS_CACHE_SIZE|" \
    -e "s|AUDIO_CACHE_ENABLED=.*|AUDIO_CACHE_ENABLED=$MS_CACHE_ENABLED|" \
    -e "s|MEDIA_LOW_PORT=.*|MEDIA_LOW_PORT=$MS_MEDIA_LOW_PORT|" \
    -e "s|MEDIA_HIGH_PORT=.*|MEDIA_HIGH_PORT=$MS_MEDIA_HIGH_PORT|" \
    -e "s|MEDIA_MAX_DURATION=.*|MEDIA_MAX_DURATION=$MS_MEDIA_MAX_DURATION|" \
    -e "s|MS_OPTS=.*|MS_OPTS=\"$MS_OPTS\"|" \
    -e "s|LOG_APPENDER_CONSOLE=.*|LOG_APPENDER_CONSOLE=$LOG_LEVEL|" \
    -e "s|LOG_APPENDER_FILE=.*|LOG_APPENDER_FILE=$LOG_LEVEL|" \
    -e "/LOG_CATEGORY_MEDIA_SERVER=.*/d" \
    -e "/LOG_CATEGORY_MGCP=.*/d" \
    -e "/LOG_CATEGORY_RTP=.*/d" \
    -e "/LOG_CATEGORY_RTCP=.*/d" $MS_FILE > $MS_FILE.bak
mv -f $MS_FILE.bak $MS_FILE

echo "MEDIA SERVER CONFIGURATION: "
cat $MS_FILE
