#!/bin/bash
## Description: Set log_level on the fly
## Author: Lefteris Banos


# VARIABLES
BASE_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_BIN=$BASE_DIR/..
CLIFILE=$BASE_DIR/log.cli


changelog() {
    cat <<EOT >> $CLIFILE
    /subsystem=logging/logger=$1:write-attribute(name=level,value=$2)
EOT
}

listlog(){
    cat <<EOT >> $CLIFILE
/subsystem=logging/logger=org.mobicents.servlet:read-resource
/subsystem=logging/logger=gov.nist:read-resource
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
                COMPONENT=org.mobicents.servlet
                changelog $COMPONENT $2
                ;;

            govnist)
                COMPONENT=gov.nist
                changelog $COMPONENT $2
                ;;
            list)
                listlog
                ;;
            *)
                echo "Usage: $0 \"servlet govnist\" DEBUG"
                echo "Usage: $0 list (To list the actual log levels)"
                exit 1
    esac
done

 n=0
   until [ $n -ge 5 ]
   do
      n=$[$n+1]
     $RESTCOMM_BIN/jboss-cli.sh --connect controller=localhost --file="$CLIFILE" # substitute your command here
      if [ $? -eq 0 ]; then echo "LOG level changed properly" && break; fi

      if [ $n -eq 5 ]; then echo "Command Fail.. please try again"; fi
      sleep 2
   done



rm $CLIFILE


