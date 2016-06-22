#!/bin/bash
## Description: Set log_level on the fly
## Author: Lefteris Banos


# VARIABLES
RESTCOMM_BIN=$RESTCOMM_HOME/bin
CLIFILE=/tmp/log.cli


changelog() {
    cat <<EOT >> $CLIFILE
    /subsystem=logging/logger=$COMPONENT:write-attribute(name=level,value=$2)
EOT
}

listlog(){
    cat <<EOT >> $CLIFILE
/subsystem=logging/logger=org.mobicents.servlet:read-resource
/subsystem=logging/logger=gov.nist:read-resource
EOT
}

arr=($1)
for compt in arr
  do
    case "$compt" in
            servlet)
                COMPONENT=org.mobicents.servlet
                changelog
                ;;

            govnist)
                COMPONENT=gov.nist
                changelog
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
      $RESTCOMM_HOME/jboss-cli.sh --connect controller=localhost --file=CLIFILE && echo "LOG level changed properly" && break;  # substitute your command here
      n=$[$n+1]
      if [ $n -eq 5 ]; then echo "Command Fail.. please try again" && break;  fi
      sleep 5
   done



rm $CLIFILE


