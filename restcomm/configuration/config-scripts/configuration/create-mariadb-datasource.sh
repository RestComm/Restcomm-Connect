#!/bin/bash
## Description : Installs MariaDB driver as a JBoss module.
##				 Creates a datasource for MariaDB. Use config-mariadb script to activate 
##				 and configure the datasource.
## Dependencies: 
##				 1. RESTCOMM_HOME variable must be set.
## Author: Henrique Rosa

# Validate RESTCOMM_HOME
if [ -z "$RESTCOMM_HOME" ]; then
	echo "RESTCOMM_HOME is not defined. Please setup this environment variable and try again."
	exit 1
fi

# Variables
MARIADB_MODULE=$RESTCOMM_HOME/modules/org/mariadb/jdbc/main
STANDALONE_SIP=$RESTCOMM_HOME/standalone/configuration/standalone-sip.xml

# Download and install MariaDB driver as a JBoss module
mkdir -p $MARIADB_MODULE
wget https://downloads.mariadb.org/f/client-java-1.1.3/mariadb-java-client-1.1.3.jar/from/http:/mirror.stshosting.co.uk/mariadb -O /tmp/mariadb-java-client-1.1.3.jar
cp /tmp/mariadb-java-client-1.1.3.jar $MARIADB_MODULE
rm -f /tmp/mariadb-java-client-1.1.3.jar

cat > $MARIADB_MODULE/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8" ?>
<module xmlns="urn:jboss:module:1.1" name="org.mariadb.jdbc">
    <resources>
        <resource-root path="mariadb-java-client-1.1.3.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
EOF

# Update JBoss configuration to create a MariaDB datasource
sed -e '/<drivers>/ a\
\                    <driver name="mariadb" module="org.mariadb.jdbc">\
\                        <xa-datasource-class>org.mariadb.jdbc.MySQLDataSource</xa-datasource-class>\
\                    </driver>' \
    -e '/<datasources>/ a\
\                <datasource jndi-name="java:/MariaDS" pool-name="MariaDS" enabled="false"> \
\                    <connection-url>jdbc:mariadb://localhost:3306/restcomm</connection-url> \
\                    <driver>mariadb</driver> \
\                    <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation> \
\                    <pool> \
\                        <min-pool-size>100</min-pool-size> \
\                        <max-pool-size>200</max-pool-size> \
\                    </pool> \
\                    <security> \
\                        <user-name>sa</user-name> \
\                        <password>sa</password> \
\                    </security> \
\                    <statement> \
\                        <prepared-statement-cache-size>100</prepared-statement-cache-size> \
\                        <share-prepared-statements/> \
\                    </statement> \
\                </datasource>' $STANDALONE_SIP > $STANDALONE_SIP.bak
mv $STANDALONE_SIP.bak $STANDALONE_SIP