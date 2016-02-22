#!/bin/bash
## Description : Enables and configures MariaDB datasource
## Dependencies:
## 				1. RESTCOMM_HOME variable
##				2. read-network-props script
## Author: Henrique Rosa

# VARIABLES
NET_PROPS="../utils/read-network-props.sh"
RESTCOMM_DEPLOY=$RESTCOMM_HOME/standalone/deployments/restcomm.war
RESTCOMM_CONFIG=$RESTCOMM_HOME/standalone/configuration

# VALIDATE PARAMETERS
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

## Description: Configures MyBatis for MariaDB
## Parameters : none
configMybatis() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/mybatis.xml
	
	grep -q '<environment id="mariadb">' $FILE || sed -i '/<environments.*>/ a \
	\	<environment id="mariadb">\
	\		<transactionManager type="JDBC"/>\
	\		<dataSource type="JNDI">\
	\			<property name="data_source" value="java:/MariaDS" />\
	\		</dataSource>\
	\	</environment>\
	' $FILE
	
	sed -e '/<environments.*>/ s|default=".*"|default="mariadb"|' $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Activated mybatis environment for MariaDB';
}

## Description: Configures MariaDB Datasource
## Parameters : 1. Private IP
configureDataSource() {
	FILE=$RESTCOMM_CONFIG/standalone-sip.xml
	
	# Update DataSource
	sed -e "/<datasource jndi-name=\"java:\/MariaDS\" .*>/ {
		N
		s|<connection-url>.*</connection-url>|<connection-url>jdbc:mariadb://$1:3306/restcomm</connection-url>|
		N
		N
		N
		N
		N
		N
		s|<user-name>.*</user-name>|<user-name>telestax</user-name>|
		s|<password>.*</password>|<password>m0b1c3nt5</password>|
	}" $FILE > $FILE.bak
	mv $FILE.bak $FILE	
	echo 'Updated MariaDB DataSource Configuration'
}

## Description: Enables MariaDB Datasource while disabling the remaining
## Parameters : none
enableDataSource() {
	FILE=$RESTCOMM_CONFIG/standalone-sip.xml
	
	# Disable all datasources but MariaDB
	sed -e '/<datasource/ s|enabled="true"|enabled="false"|' \
	    -e '/<datasource.*MariaDS/ s|enabled=".*"|enabled="true"|' \
	    $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Enabled MariaDB datasource'
}

## Description: Configures RestComm DAO manager to use MariaDB
## Params: none
configDaoManager() {
	FILE=$RESTCOMM_DEPLOY/WEB-INF/conf/restcomm.xml
	
	sed -e '/<dao-manager class="org.mobicents.servlet.restcomm.dao.mybatis.MybatisDaoManager">/ {
		N
		N
		s|<data-files>.*</data-files>|<data-files></data-files>|
		N
		s|<sql-files>.*</sql-files>|<sql-files>${restcomm:home}/WEB-INF/scripts/mariadb/sql</sql-files>|
	}' $FILE > $FILE.bak
	
	mv $FILE.bak $FILE
	echo 'Configured iBatis Dao Manager for MariaDB'
}

# MAIN
echo 'Configuring MariaDB datasource...'
configureDataSource $PRIVATE_IP
enableDataSource
configMybatis
configDaoManager
echo 'Finished configuring MariaDB datasource!'