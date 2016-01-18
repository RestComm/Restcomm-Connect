# External parameters
#KEYCLOAK_DOWNLOAD_URL=http://downloads.jboss.org/keycloak/1.6.1.Final/keycloak-1.6.1.Final.tar.gz
#KEYCLOAK_DOWNLOAD_ADAPTER_URL=http://downloads.jboss.org/keycloak/1.6.1.Final/adapters/keycloak-oidc/keycloak-wf9-adapter-dist-1.6.1.Final.tar.gz

STARTUP_PATH=`pwd`

KEYCLOAK_FILE=`echo ${KEYCLOAK_DOWNLOAD_URL} | sed 's_.*/\([^/].*\)$_\1_'`
KEYCLOAK_DIR=`echo ${KEYCLOAK_DOWNLOAD_URL} | sed 's_.*/\([^/].*\).tar.gz$_\1_'`
KEYCLOAK_VERSION=`echo ${KEYCLOAK_DIR} | sed 's/keycloak-\(.*\)$/\1/'`

echo "--- setting up keycloak ${KEYCLOAK_VERSION} for testing"
echo "Startup environment"
echo "KEYCLOAK_DOWNLOAD_URL: $KEYCLOAK_DOWNLOAD_URL"
echo "KEYCLOAK_DOWNLOAD_ADAPTER_URL: $KEYCLOAK_DOWNLOAD_ADAPTER_URL"

echo "--- downloading keycloak binary"
wget -N ${KEYCLOAK_DOWNLOAD_URL}

# uncompress if not already there
if [ ! -d ${KEYCLOAK_DIR} ]
then
	tar zxf ${KEYCLOAK_FILE}
fi

# use a fixed name also
rm keycloak
ln -s ${KEYCLOAK_DIR} keycloak

echo "--- installing keycloak adapter"
cd ${KEYCLOAK_DIR}
wget -N ${KEYCLOAK_DOWNLOAD_ADAPTER_URL}
KEYCLOAK_ADAPTER_FILE=`echo ${KEYCLOAK_DOWNLOAD_ADAPTER_URL} | sed 's_.*/\([^/].*\)$_\1_'`
tar zxf ${KEYCLOAK_ADAPTER_FILE}
echo "--- keycloak adapter installed"
cd ..


# configure keycloak to host clients too
echo "--- customizing keycloak configuration"
sed -e '/<extension module\="org.keycloak.keycloak-adapter-subsystem"\/>/ d' \
    -e '/<\/extensions>/ i\
        <extension module\="org.keycloak.keycloak-adapter-subsystem"\/>' \
    -e '/<subsystem xmlns\="urn:jboss:domain:keycloak:1.1"\/>/ d' \
    -e '/<\/profile>/ i\
        <subsystem xmlns\="urn:jboss:domain:keycloak:1.1"\/>' \
    < ${KEYCLOAK_DIR}/standalone/configuration/standalone.xml > ${KEYCLOAK_DIR}/standalone/configuration/standalone-for-identity-proxy.xml

# retrieve identity-proxy src
echo "--- cloning identity-proxy src"
git clone https://github.com/RestComm/restcomm-identity.git
echo "--- identity-proxy src cloned"
# build identity-proxy
cd restcomm-identity/identity-proxy/
mvn clean package
echo "--- identity-proxy built"
cd ../..

# start keycloak
echo "--- starting keycloak..."
${KEYCLOAK_DIR}/bin/standalone.sh -b 127.0.0.1 -Djboss.management.http.port=9980 -Djboss.management.https.port=9983 -Djboss.ajp.port=8010 -Djboss.http.port=8081 -Djboss.https.port=8444 -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=$STARTUP_PATH/release/master-realm-for-testing.json -Dkeycloak.migration.strategy=OVERWRITE_EXISTING --server-config standalone-for-identity-proxy.xml &

# wait keycloak to start
KEYCLOAKWAIT_STATUS=000
KEYCLOAKWAIT_MAX=30
KEYCLOAKWAIT_COUNT=0
until [ $KEYCLOAKWAIT_STATUS -eq 200 ]; do
	KEYCLOAKWAIT_STATUS=$(curl -sL -w "%{http_code}\\n" "http://127.0.0.1:8081/auth/" -o /dev/null)
	echo Waiting for keycloak to start - status: $KEYCLOAKWAIT_STATUS
	KEYCLOAKWAIT_COUNT=$(expr $KEYCLOAKWAIT_COUNT + 1)
	if [ $KEYCLOAKWAIT_COUNT -eq $KEYCLOAKWAIT_MAX ]; then
		break;
	fi
	sleep 2
done
echo "--- keycloak started"

# deploy proxy identity on keycloak wildfly
./keycloak/bin/jboss-cli.sh --commands="connect localhost:9980,deploy restcomm-identity/identity-proxy/target/restcomm-identity.war"
echo "--- identity proxy deployed"

# do the testing
# ...
# ...

#stop keycloak
#./keycloak/bin/jboss-cli.sh --commands="connect localhost:9980,shutdown"
