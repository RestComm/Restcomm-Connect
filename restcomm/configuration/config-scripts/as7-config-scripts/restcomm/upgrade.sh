#!/bin/bash

##
## Description: Upgrade script
## Author : George Vagenas
##

current_folder=$(pwd)
source $current_folder/upgrade.conf
DATE=$(date +%b_%d_%Y_%H_%M_%S)

db_upgrade_folder=$current_folder/upgrade_scripts/db_upgrade
db_backup_folder=$current_folder/upgrade_scripts/backups
db_backup_file="$RESTCOMM_DB"_dump_$DATE

migrate() {
  $db_upgrade_folder/flyway baseline
  $db_upgrade_folder/flyway migrate
  $db_upgrade_folder/flyway info
}

sed -i "s|flyway.url=.*|flyway.url=jdbc:mysql://$MYSQL_IP_ADDRESS:$MYSQL_PORT/restcomm?useSSL=false|" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.user=.*/flyway.user=$MYSQL_USER/" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.password=.*/flyway.password=$MYSQL_PASSWORD/" $db_upgrade_folder/conf/flyway.conf
sed -i "s/flyway.placeholders.RESTCOMM_DBNAME=.*/flyway.placeholders.RESTCOMM_DBNAME=$RESTCOMM_DB/" $db_upgrade_folder/conf/flyway.conf

chmod +x $db_upgrade_folder/flyway

echo "Mysql ipaddress:port -> $MYSQL_IP_ADDRESS:$MYSQL_PORT"
echo "MySQL backup: $MYSQL_BACKUP"

if [ "$MYSQL_BACKUP" = "TRUE" ] || [ "$MYSQL_BACKUP" = "true" ]; then
  if [ -z "$(command -v mysqldump)" ]; then
  	echo "ERROR: mysqldump is not installed! Install it and try again."
  	echo "Centos/RHEL: yum install mysql-client"
  	echo "Debian/Ubuntu: apt-get install mysql-client"
  	exit 1
  fi
  echo "Will create backup $db_backup_folder/$db_backup_file.tgz"
  mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD -h $MYSQL_IP_ADDRESS -P $MYSQL_PORT --add-drop-table --lock-tables $RESTCOMM_DB  -r $db_backup_folder/$db_backup_file
  tar cvzf $db_backup_folder/$db_backup_file.tgz -C $db_backup_folder $db_backup_file
  rm $db_backup_folder/$db_backup_file
fi

migrate
