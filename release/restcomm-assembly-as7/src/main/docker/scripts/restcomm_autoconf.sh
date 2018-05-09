#!/bin/bash

#Maintainer Lefteris Banos -liblefty@telestax.com

echo "Will check if Enviromental variables script file is set"

CONFFILE=/tmp/conf.sh

function download_conf(){
echo "url $1 $2 $3"
if [[ `wget -S --spider $1 $2 $3 2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
               if [ -n "$2" ] && [ -n "$3" ]; then
                    wget $1 $2 $3 -O $4
               else
                    wget $1 -O $2
                fi
                return 0;
        else
                echo "false"
                wget -S --spider $1 $2 $3
                exit 1;
  fi
}


function run_conf(){
  echo "Run configuratin file /tmp/conf.sh"
  if [ -f $CONFFILE ]; then
  		chmod +x $CONFFILE
  		source $CONFFILE
  		rm -f $CONFFILE
  		return 0
	else
		echo "$CONFFILE not found."
		return 1
	fi
}

if [ -n "$ENVCONFURL" ]; then
  echo "Configuration file URL is: $ENVCONFURL"
  if [ -n "$REPOUSR" ]  &&  [ -n "$REPOPWRD" ]; then
  		USR="--user=$REPOUSR"
  		PASS="--password=$REPOPWRD"
  fi
   URL="$ENVCONFURL $USR $PASS"
   download_conf $URL $CONFFILE
   run_conf
fi

#auto delete script after run once. No need more.
rm -- "$0"



