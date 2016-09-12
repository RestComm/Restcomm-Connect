#!/bin/bash

##
## Description: Upgrade script
## Author : George Vagenas
##

current_folder=$(pwd)
source $current_folder/upgrade.conf
DATE=$(date +%b_%d_%Y_%H_%M_%S)

db_upgrade_folder=$current_folder/upgrade_scripts/db_upgrade

status() {
  $db_upgrade_folder/flyway info
}

sed -i "s|flyway.url=.*|flyway.url=jdbc:mysql://$MYSQL_IP_ADDRESS:$MYSQL_PORT/$RESTCOMM_DB?useSSL=false|" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.user=.*/flyway.user=$MYSQL_USER/" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.password=.*/flyway.password=$MYSQL_PASSWORD/" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.placeholders.RESTCOMM_DBNAME=.*/flyway.placeholders.RESTCOMM_DBNAME=$RESTCOMM_DB/" $db_upgrade_folder/conf/flyway.conf

chmod +x $db_upgrade_folder/flyway

echo "Mysql ipaddress:port -> $MYSQL_IP_ADDRESS:$MYSQL_PORT"

migrate
