#!/bin/bash
## Description: Set log_level on the fly
## Author: Lefteris Banos
##Using Jboss Command Line Interface - CLI (https://developer.jboss.org/wiki/CommandLineInterface)


# VARIABLES
BASE_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_BIN=$BASE_DIR/..
CLIFILE=$BASE_DIR/log.cli


changelog() {
    cat <<EOT >> $CLIFILE
    /subsystem=logging/logger=$1:write-attribute(name=level,value=$2)
EOT
}

changelogROOT() {
    cat <<EOT >> $CLIFILE
    /subsystem=logging/root-logger=$1:write-attribute(name=level,value=$2)
EOT
}

changelogCONSOLE() {
    cat <<EOT >> $CLIFILE
    /subsystem=logging/console-handler=$1:write-attribute(name=level,value=$2)
EOT
}


listlog(){
    cat <<EOT >> $CLIFILE
/subsystem=logging/logger=org.mobicents.servlet.sip:read-resource
/subsystem=logging/logger=org.mobicents.servlet.sip.restcomm:read-resource
/subsystem=logging/logger=org.restcomm.connect:read-resource
/subsystem=logging/logger=gov.nist:read-resource
/subsystem=logging/console-handler=CONSOLE:read-resource
/subsystem=logging/root-logger=ROOT:read-resource
EOT
}

if [ $# -eq 0 ]
  then
    arr="help"
else
    arr=( "$@" )
fi

for compt in $arr
  do
    case "$compt" in
            servlet)
                COMPONENT=org.mobicents.servlet.sip
                changelog $COMPONENT $2
                ;;

            govnist)
                COMPONENT=gov.nist
                changelog $COMPONENT $2
                ;;
            siprestcomm)
                COMPONENT=org.mobicents.servlet.sip.restcomm
                changelog $COMPONENT $2
                ;;
            restcomm)
                COMPONENT=org.restcomm.connect
                changelog $COMPONENT $2
                ;;
             root)
                 COMPONENT=ROOT
                changelogROOT $COMPONENT $2
                ;;
             console)
                 COMPONENT=CONSOLE
                changelogCONSOLE $COMPONENT $2
                ;;
            list)
                listlog
                ;;
            *)
                echo "Usage: $0 \"servlet govnist siprestcomm restscomm console root\" DEBUG. Can also set each element individually"
                echo "Usage: $0 list (To list the actual log levels)"
                exit 1
    esac
done

 n=0
   until [ $n -ge 5 ]
   do
      n=$[$n+1]
     $RESTCOMM_BIN/jboss-cli.sh --connect controller=127.0.0.1 --file="$CLIFILE" # substitute your command here
      if [ $? -eq 0 ]; then echo "LOG level changed properly" && break; fi

      if [ $n -eq 5 ]; then echo "Command Fail.. please try again"; fi
      sleep 2
   done



rm $CLIFILE


