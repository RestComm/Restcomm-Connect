#!/bin/bash
## Description: Configures RestComm
## Dependencies:
## 				1. RESTCOMM_HOME variable
##				2. read-network-props script
## Author: Henrique Rosa

# VARIABLES
NET_PROPS="../utils/read-network-props.sh"

RESTCOMM_BIN=$RESTCOMM_HOME/bin
RESTCOMM_DARS=$RESTCOMM_HOME/standalone/configuration/dars
RESTCOMM_DEPLOY=$RESTCOMM_HOME/standalone/deployments/restcomm.war

OUTBOUND_IP='0.0.0.0'
VI_LOGIN='vi_login'
VI_PASSWORD='vi_pass' 
VI_ENDPOINT='vi_endpoint'
INTERFAX_USER='ifax_user'
INTERFAX_PASSWORD='ifax_pass'
ISPEECH_KEY='ispeech_key'
ACAPELA_APPLICATION='acapela_app'
ACAPELA_LOGIN='acapalea_login'
ACAPELA_PASSWORD='acapela_pass'
VOICERSS_KEY='voicerss_key'

# DEPENDENCY VALIDATION
if [ -z "$RESTCOMM_HOME" ]; then
	echo "RESTCOMM_HOME is not defined. Please setup this environment variable and try again."
	exit 1
fi

if [ ! -f $NET_PROPS ]; then
	echo "Network Properties dependency not found: $NET_PROPS"
	exit 1
fi

# IMPORTS
source $NET_PROPS

## Description: Updates RestComm configuration file
## Parameters : 1.Private IP
## 				2.Public IP
configRestcomm() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	sed -e "s|<local-address>$IP_ADDRESS_PATTERN<\/local-address>|<local-address>$1<\/local-address>|" \
	    -e "s|<remote-address>$IP_ADDRESS_PATTERN<\/remote-address>|<remote-address>$1<\/remote-address>|" \
	    -e "s|<\!--.*<external-ip>.*<\/external-ip>.*-->|<external-ip>$2<\/external-ip>|" \
	    -e "s|<external-ip>.*<\/external-ip>|<external-ip>$2<\/external-ip>|" \
	    -e "s|<external-address>.*<\/external-address>|<external-address>$2<\/external-address>|" \
	    -e "s|<\!--.*<external-address>.*<\/external-address>.*-->|<external-address>$2<\/external-address>|" \
	    -e "s|<prompts-uri>.*<\/prompts-uri>|<prompts-uri>http:\/\/$2:8080\/restcomm\/audio<\/prompts-uri>|" \
	    -e "s|<cache-uri>.*<\/cache-uri>|<cache-uri>http:\/\/$2:8080\/restcomm\/cache<\/cache-uri>|" \
	    -e "s|<recordings-uri>.*<\/recordings-uri>|<recordings-uri>http:\/\/$2:8080\/restcomm\/recordings<\/recordings-uri>|" \
	    -e "s|<error-dictionary-uri>.*<\/error-dictionary-uri>|<error-dictionary-uri>http:\/\/$2:8080\/restcomm\/errors<\/error-dictionary-uri>|" \
	    -e "s|<outbound-proxy-uri>.*<\/outbound-proxy-uri>|<outbound-proxy-uri>$OUTBOUND_IP<\/outbound-proxy-uri>|" \
	    -e "s|<outbound-endpoint>.*<\/outbound-endpoint>|<outbound-endpoint>$OUTBOUND_IP<\/outbound-endpoint>|" \
	    -e 's|<outbound-prefix>.*</outbound-prefix>|<outbound-prefix>#</outbound-prefix>|' $FILE > $FILE.bak;
	mv $FILE.bak $FILE
	echo 'Updated RestComm configuration'
}

## Description: Configures Voip Innovations Credentials
## Parameters : 1.Login
## 				2.Password
## 				3.Endpoint
configVoipInnovations() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e "/<voip-innovations>/ {
		N; s|<login>.*</login>|<login>$1</login>|
        N; s|<password>.*</password>|<password>$2</password>|
        N; s|<endpoint>.*</endpoint>|<endpoint>$3</endpoint>|
	}" $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured Voip Innovation credentials'
}

## Description: Configures Fax Service Credentials
## Parameters : 1.Username
## 				2.Password
configFaxService() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e "/<fax-service.*>/ {
		N; s|<user>.*</user>|<user>$1</user>|
		N; s|<password>.*</password>|<password>$2</password>|
	}" $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured Fax Service credentials'
}

## Description: Configures Speech Recognizer
## Parameters : 1.iSpeech Key
configSpeechRecognizer() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e "/<speech-recognizer.*>/ {
		N; s|<api-key.*></api-key>|<api-key production=\"true\">$1</api-key>|
	}" $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured the Speech Recognizer'
}

## Description: Configures Acapela Speech Synthesizer
## Parameters : 1.Application Code
## 				2.Login
## 				3.Password
configAcapela() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e "/<speech-synthesizer class=\"org.mobicents.servlet.restcomm.tts.AcapelaSpeechSynthesizer\">/ {
		N
		N; s|<application>.*</application>|<application>$1</application>|
		N; s|<login>.*</login>|<login>$2</login>|
		N; s|<password>.*</password>|<password>$3</password>|
	}" $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured Acapela Speech Synthesizer'
}

## Description: Configures VoiceRSS Speech Synthesizer
## Parameters : 1.API key
configVoiceRSS() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e "/<speech-synthesizer class=\"org.mobicents.servlet.restcomm.tts.VoiceRSSSpeechSynthesizer\">/ {
		N
		N; s|<apikey>.*</apikey>|<apikey>$1</apikey>|
	}" $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured VoiceRSS Speech Synthesizer'
}

## Description: Updates Mobicents properties for RestComm
## Parameters : none
configMobicentsProperties() {
	FILE=$RESTCOMM_DARS/mobicents-dar.properties
	sed -e 's|^ALL=.*|ALL=("RestComm", "DAR\:From", "NEUTRAL", "", "NO_ROUTE", "0")|' $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo "Updated mobicents-dar properties"
}

# MAIN
echo 'Configuring RestComm...'
configMobicentsProperties
configRestcomm $PRIVATE_IP $PUBLIC_IP
configVoipInnovations $VI_LOGIN $VI_PASSWORD $VI_ENDPOINT
configFaxService $INTERFAX_USER $INTERFAX_PASSWORD
configSpeechRecognizer $ISPEECH_KEY
configAcapela $ACAPELA_APPLICATION $ACAPELA_LOGIN $ACAPELA_PASSWORD
configVoiceRSS $VOICERSS_KEY
echo 'Configured RestComm!'