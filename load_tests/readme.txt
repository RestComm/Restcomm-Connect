##
## DOWNLOAD SIPp TOOL
##

Start by download version 3.4.x of SIPp tool, from official repo
https://github.com/SIPp/sipp/releases

Read official documentation on how to install SIPp with pcap support
http://sipp.sourceforge.net/doc/reference.html#Installing+SIPp


##
## PREPARATIONS
##
To execute load tests, it is recommended that:

1. Log level to WARN for JBoss and MMS. Number one bottleneck is logging!

2. Disable AKKA logging. Edit the file $RESTCOMM/standalone/deployments/restcomm.war/WEB-INF/classes/application.conf  like:

    # Event handlers to register at boot time (Logging$DefaultLogger logs to STDOUT)
    event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
    
    # Log level used by the configured loggers (see "event-handlers") as soon
    # as they have been started; before that, see "stdout-loglevel"
    # Options: OFF, ERROR, WARNING, INFO, DEBUG
    loglevel = "OFF"
    
    # Log level for the very basic logger activated during AkkaApplication startup
    # Options: OFF, ERROR, WARNING, INFO, DEBUG
    stdout-loglevel = "OFF"
    
    # Log the complete configuration at INFO level when the actor system is started.
    # This is useful when you are uncertain of what configuration is used.
    log-config-on-start = off

3. mss-sip-stack.properties:

    gov.nist.javax.sip.THREAD_POOL_SIZE=512
    gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE=131072
    gov.nist.javax.sip.SEND_UDP_BUFFER_SIZE=131072

4. Edit JBoss JVM settings, provide as much memory as permitted:
   JAVA_OPTS="-Xms2048m -Xmx8192m -Xmn512m -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=100 -XX:CMSIncrementalDutyCycleMin=100 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:MaxPermSize=512m"

5. Edit MMS JVM settings, provide as much memory as permitted:
   JAVA_OPTS="-Xms1024m -Xmx2048m -Xmn512m -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=100 -XX:CMSIncrementalDutyCycleMin=100 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:MaxPermSize=512m"

6. Provide a big RTP range for MMS. Edit $RESTCOMM/mediaserver/deploy/server-beans.xml and either remove the
    <property name="highestPort">65535</property>
    <property name="lowestPort">64534</property>
   so it would default to a huge range or provide a huge range.

##
## RESOURCES
##

/resources - contains media resources to be used in test scenarios
   /audio
   /pcap

/tests - contains the test scenarios to be executed
   /test1
      + test-sipp.xml (SIPp test scenario)
      + test.sh       (Script that executes the SIPp command)
      + test-rvd.zip  (Restcomm application to be imported to the RVD)

##
## IMPORTANT NOTES
##

1. For short applications such as hello-play or hello-world (+1234 or +1235) bottleneck is the database among the others already mentioned. This is because this is a short call and when under heavy load, Restcomm will access DB many times to query the application for the given DID and also to update CDRs.

2. For complex applications such as Voicemail which involve creating connections, start playing wav files, start DTMF collectors and recording files
    a) bottleneck under heavy load is MMS which wont be able process all requests and thus there will be MGCP re-transmissions from the MGCP client (still investigating this case since CPU and MEM are in normal levels)  and
    b) The Linux OS open file limitation (to many open files in order to access wav files and also to record call to files) - this can be changed from the OS.
