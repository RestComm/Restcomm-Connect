#!/bin/bash
#Maintainer George Vagenas - gvagenas@telestax.com
#Maintainer Jean Deruelle - jean.deruelle@telestax.com
#Maintainer Lefteris Banos -liblefty@telestax.com

source /etc/container_environment.sh

BASEDIR=/opt/Restcomm-JBoss-AS7

TRUSTSTORE_FILE_NAME=restcomm-combined.jks
TRUSTSTORE_FILE=$BASEDIR/standalone/configuration/$TRUSTSTORE_FILE_NAME
DERFILE=$BASEDIR/ca-authority.der
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
                exit 1;
  fi
}

if [[ "$SECURESSL" = "SELF" ||  "$SECURESSL" = "AUTH" ]]; then
     # Add StartComm certificate to the truststore to avoid SSL Exception when fetching the URL
    keytool -importcert -alias startssl -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -file $BASEDIR/ca-startcom.der -noprompt
	sed -i "s|SECURESSL=.*|SECURESSL='${SECURESSL}'|" $BASEDIR/bin/restcomm/advanced.conf
	sed -i "s|TRUSTSTORE_FILE=.*|TRUSTSTORE_FILE='${TRUSTSTORE_FILE_NAME}'|" $BASEDIR/bin/restcomm/advanced.conf
fi

if [ -n "$CERTCONFURL" ]; then
  echo "Certification file URL is: $CERTCONFURL"
  if [ -n "$CERTREPOUSR"  ] && [ -n "$CERTREPOPWRD" ]; then
  		USR="--user=${CERTREPOUSR}"
  		PASS="--password=${CERTREPOPWRD}"
  fi
   URL="$CERTCONFURL $USR $PASS"
   download_conf $URL $TRUSTSTORE_FILE
fi

#auto delete script after run once. No need more.
rm -- "$0"