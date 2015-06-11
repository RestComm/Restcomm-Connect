#!/bin/sh
### ====================================================================== ###
##                                                                          ##
##  Mobicents Media Server Bootstrap Script                                 ##
##                                                                          ##
### ====================================================================== ###

### $Id: run.sh abhayani@redhat.com $ ###

DIRNAME=`dirname $0`
PROGNAME=`basename $0`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

#
# Helper to complain.
#
warn() {
    echo "${PROGNAME}: $*"
}

#
# Helper to puke.
#
die() {
    warn $*
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
        
    Linux)
        linux=true
        ;;
esac


# Force IPv4 on Linux systems since IPv6 doesn't work correctly with jdk5 and lower
if [ "$linux" = "true" ]; then
   JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$MMS_HOME" ] &&
        MMS_HOME=`cygpath --unix "$MMS_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup MMS_HOME
if [ "x$MMS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    MMS_HOME=`cd $DIRNAME/..; pwd`
fi
export MMS_HOME

# Increase the maximum file descriptors if we can
if [ "$cygwin" = "false" ]; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ]; then
	if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
	    # use the system max
	    MAX_FD="$MAX_FD_LIMIT"
	fi

	ulimit -n $MAX_FD
	if [ $? -ne 0 ]; then
	    warn "Could not set maximum file descriptor limit: $MAX_FD"
	fi
    else
	warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
	JAVA="$JAVA_HOME/bin/java"
    else
	JAVA="java"
    fi
fi

# Setup the classpath
runjar="$MMS_HOME/bin/run.jar"
if [ ! -f "$runjar" ]; then
    die "Missing required file: $runjar"
fi

MMS_BOOT_CLASSPATH="$runjar"

if [ "x$MMS_CLASSPATH" = "x" ]; then
    MMS_CLASSPATH="$MMS_BOOT_CLASSPATH"
else
    MMS_CLASSPATH="$MMS_CLASSPATH:$MMS_BOOT_CLASSPATH"
fi


# If -server not set in JAVA_OPTS, set it, if supported
SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
if [ "x$SERVER_SET" = "x" ]; then

    # Check for SUN(tm) JVM w/ HotSpot support
    if [ "x$HAS_HOTSPOT" = "x" ]; then
	HAS_HOTSPOT=`"$JAVA" -version 2>&1 | $GREP -i HotSpot`
    fi

    # Enable -server if we have Hotspot, unless we can't
    if [ "x$HAS_HOTSPOT" != "x" ]; then
	# MacOS does not support -server flag
	if [ "$darwin" != "true" ]; then
	    JAVA_OPTS="-server $JAVA_OPTS"
	fi
     fi
fi

# Setup MMS specific properties
JAVA_OPTS="-Dprogram.name=$PROGNAME $JAVA_OPTS"
JAVA_OPTS="$JAVA_OPTS -Xms1024m -Xmx2048m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"
#JAVA_OPTS="$JAVA_OPTS -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

# Setup the java endorsed dirs
MMS_ENDORSED_DIRS="$MMS_HOME/lib"
if [ "x$JAVA_HOME" != "x" ]; then
    if [ -d "$JAVA_HOME/jre/lib/ext" ]; then
        MMS_ENDORSED_DIRS="$MMS_ENDORSED_DIRS:$JAVA_HOME/jre/lib/ext"
    else
        echo 'ERROR: The extension lib for Java does not exist. Please configure $JAVA_HOME/jre/lib/ext'
        exit 1
    fi

# Setup path for native libs
LD_LIBRARY_PATH="$MMS_HOME/native"
export LD_LIBRARY_PATH

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    MMS_HOME=`cygpath --path --windows "$MMS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    MMS_CLASSPATH=`cygpath --path --windows "$MMS_CLASSPATH"`
    MMS_ENDORSED_DIRS=`cygpath --path --windows "$MMS_ENDORSED_DIRS"`
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  MMS Bootstrap Environment"
echo ""
echo "  MMS_HOME: $MMS_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "  CLASSPATH: $MMS_CLASSPATH"
echo ""
echo "========================================================================="
echo ""

      "$JAVA" $JAVA_OPTS \
         -Djava.ext.dirs="$MMS_ENDORSED_DIRS" \
         -Dmbrola.base="$MMS_HOME/mbrola" \
         -classpath "$MMS_CLASSPATH" \
         org.mobicents.media.server.bootstrap.Main "$@"
      MMS_STATUS=$?

#java -Djava.ext.dirs=`pwd`/lib -Dmms.home=. -cp .:mms-standalone-2.0.0.BETA1-SNAPSHOT.jar org.mobicents.media.server.bootstrap.jmx.JMXMain
