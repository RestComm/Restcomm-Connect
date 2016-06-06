#!/usr/bin/env bash

##
## Description: Configures RestComm
## Author: Lefteris Banos (eleftherios.banos@telestax.com)
##

# VARIABLES
RESTCOMM_BIN=$RESTCOMM_HOME/bin
RESTCOMM_DARS=$RESTCOMM_HOME/standalone/configuration/dars
RESTCOMM_CONF=$RESTCOMM_HOME/standalone/configuration
RESTCOMM_DEPLOY=$RESTCOMM_HOME/standalone/deployments/restcomm.war

#funcitions for  SECURESSL=false
NoSslRestConf(){
	FILE=$RESTCOMM_CONF/standalone-sip.xml
	sed -e "s/<connector name=\"https\" \(.*\)>/<\!--connector name=\"https\" \1>/" \
	-e "s/<\/connector>/<\/connector-->/" $FILE > $FILE.bak
	mv $FILE.bak $FILE
	sed -e "s/<\!--connector name=\"http\" \(.*\)-->/<connector name=\"http\" \1\/>/" $FILE > $FILE.bak
	mv $FILE.bak $FILE
}

NoSslRmsConf(){
	FILE=$MMS_HOME/bin/run.sh
	sed -e "/# Setup MMS specific properties/ {
		N; s|JAVA_OPTS=.*|JAVA_OPTS=\"-Dprogram\.name=\\\$PROGNAME \\\$JAVA_OPTS\"|
	}" $FILE > $FILE.bak
	mv $FILE.bak $FILE
}

#funcitions for  SECURESSL=SELF" ||  "AUTH"
SslRestCommConf(){
	FILE=$RESTCOMM_CONF/standalone-sip.xml
	echo "Will properly configure HTTPS Connector ";
	if [  "${DISABLE_HTTP^^}" = "TRUE"  ]; then
		echo "DISABLE_HTTP is '$DISABLE_HTTP'. Will disable HTTP Connector"
		sed -e "s/<connector name=\"http\" \(.*\)\/>/<\!--connector name=\"http\" \1-->/" $FILE > $FILE.bak
		mv $FILE.bak $FILE
	else
		sed -e "s/<\!--connector name=\"http\" \(.*\)-->/<connector name=\"http\" \1\/>/" $FILE > $FILE.bak
		mv $FILE.bak $FILE
	fi
	if [[ "$TRUSTSTORE_FILE" = /* ]]; then
		CERTIFICATION_FILE=$TRUSTSTORE_FILE
	else
		CERTIFICATION_FILE="\\\${jboss.server.config.dir}/$TRUSTSTORE_FILE"
	fi
	echo "Will use trust store at location: $CERTIFICATION_FILE"
	sed -e "s/<\!--connector name=\"https\" \(.*\)>/<connector name=\"https\" \1>/" \
	-e "s|<ssl name=\"https\" key-alias=\".*\" password=\".*\" certificate-key-file=\".*\" \(.*\)\/>|<ssl name=\"https\" key-alias=\"$TRUSTSTORE_ALIAS\" password=\"$TRUSTSTORE_PASSWORD\" certificate-key-file=\"$CERTIFICATION_FILE\" cipher-suite=\"TLS_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA\" verify-client=\"false\" \1\/>|" \
	-e "s/<\/connector-->/<\/connector>/" $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo "Properly configured HTTPS Connector to use trustStore file $CERTIFICATION_FILE"
}

SslRmsConf(){
	echo "TRUSTSTORE_PASSWORD is set to '$TRUSTSTORE_PASSWORD' will properly configure MMS";
	FILE=$MMS_HOME/bin/run.sh
	if [[ "$TRUSTSTORE_FILE" = /* ]]; then
		CERTIFICATION_FILE=$TRUSTSTORE_FILE
	else
		CERTIFICATION_FILE=$RESTCOMM_HOME/standalone/configuration/$TRUSTSTORE_FILE
	fi
	JAVA_OPTS_TRUSTORE="-Djavax.net.ssl.trustStore=$CERTIFICATION_FILE -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"
	sed -e "/# Setup MMS specific properties/ {
	  N; s|JAVA_OPTS=.*|JAVA_OPTS=\"-Dprogram\.name=\\\$PROGNAME \\\$JAVA_OPTS $JAVA_OPTS_TRUSTORE\"|
	}" $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo "Properly configured MMS to use trustStore file $RESTCOMM_HOME/standalone/configuration/$TRUSTSTORE_FILE"
}

CertConfigure(){
  #Certificate setup (Authority certificate or self-signed)
  if [ "$SECURESSL" = "AUTH" ]; then
      echo "Authorized certificate is used"
  elif [ "$SECURESSL" = "SELF"  ]; then
	echo "TRUSTSTORE_FILE is not provided but SECURE is TRUE. We will create and configure self signed certificate"

	if [[ "$TRUSTSTORE_FILE" = /* ]]; then
		TRUSTSTORE_LOCATION=$TRUSTSTORE_FILE
	else
		TRUSTSTORE_LOCATION=$RESTCOMM_HOME/standalone/configuration/$TRUSTSTORE_FILE
	fi

	 echo "TRUSTSTORE_LOCATION: $TRUSTSTORE_LOCATION"
	 echo "PUBLIC_IP: $PUBLIC_IP"
	 echo "RESTCOMM_HOST: $RESTCOMM_HOST"
	if [ -n "$RESTCOMM_HOST" ]; then
		HOSTNAME="${RESTCOMM_HOST}"
		keytool -genkey -alias $TRUSTSTORE_ALIAS -keyalg RSA -keystore $TRUSTSTORE_LOCATION -dname "CN=$HOSTNAME" -storepass $TRUSTSTORE_PASSWORD -keypass $TRUSTSTORE_PASSWORD
	else
		HOSTNAME="${PUBLIC_IP}"
		keytool -genkey -alias $TRUSTSTORE_ALIAS -keyalg RSA -keystore $TRUSTSTORE_LOCATION -dname "CN=restcomm" -ext san=ip:"$HOSTNAME" -storepass $TRUSTSTORE_PASSWORD -keypass $TRUSTSTORE_PASSWORD
	fi

	echo "The generated truststore file at $TRUSTSTORE_LOCATION "
  fi


  sed -i "s|protocol=\"TLSv1,TLSv1.1,TLSv1.2\"|protocol=\"TLSv1,TLSv1.1,TLSv1.2,SSLv2Hello\"|" $RESTCOMM_CONF/standalone-sip.xml
  grep -q 'ephemeralDHKeySize' $RESTCOMM_BIN/standalone.conf || sed -i "s|-Djava.awt.headless=true|& -Djdk.tls.ephemeralDHKeySize=2048|" $RESTCOMM_BIN/standalone.conf
  grep -q 'https.protocols' $RESTCOMM_BIN/standalone.conf || sed -i "s|-Djava.awt.headless=true|& -Dhttps.protocols=TLSv1.1,TLSv1.2|" $RESTCOMM_BIN/standalone.conf
}

MssStackConf(){

	FILE=$RESTCOMM_CONF/mss-sip-stack.properties

	if  grep -q 'gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled' "$FILE"; then
   		sed -i '/gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled/,+5d' $FILE
 	fi

	if [[ "$TRUSTSTORE_FILE" = /* ]]; then
		TRUSTSTORE_LOCATION=$TRUSTSTORE_FILE
	else
		TRUSTSTORE_LOCATION=$RESTCOMM_HOME/standalone/configuration/$TRUSTSTORE_FILE
	fi

   sed -i '/org.mobicents.ha.javax.sip.LOCAL_SSL_PORT='"$HTTPS_PORT"'/ a \
  \gov.nist.javax.sip.TLS_CLIENT_AUTH_TYPE=Disabled\
  \javax.net.ssl.keyStore='"$TRUSTSTORE_LOCATION"'\
  \javax.net.ssl.keyStorePassword='" $TRUSTSTORE_PASSWORD"'\
  \javax.net.ssl.trustStorePassword='"$TRUSTSTORE_PASSWORD"'\
  \javax.net.ssl.trustStore='"$TRUSTSTORE_LOCATION"'\
  \javax.net.ssl.keyStoreType=JKS' $RESTCOMM_CONF/mss-sip-stack.properties
}

# MAIN
echo 'RestComm SSL Configuring ...'

if [[ "$SECURESSL" = "SELF" ||  "$SECURESSL" = "AUTH" ]]; then
  	if [[ -z $TRUSTSTORE_ALIAS || -z $TRUSTSTORE_PASSWORD || -z $TRUSTSTORE_FILE ]]; then
  		echo 'Need to set all: TRUSTSTORE_ALIAS, TRUSTSTORE_PASSWORD,TRUSTSTORE_FILE '
	else
  		echo "SECURE $SECURESSL"
		SslRestCommConf
		SslRmsConf
		CertConfigure
		MssStackConf
	fi
elif [[ "${SECURESSL^^}" = "FALSE"  ]]; then
	NoSslRestConf
	NoSslRmsConf
else
    echo "Allowed values for SECURESSL: SELF, AUTH, FALSE"
fi