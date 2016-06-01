#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #923
#Date: Mar 11, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#Modify table "restcomm_instance_id", add 'host' VARCHAR(255)
ALTER TABLE restcomm_instance_id ADD host VARCHAR(255);

INSERT INTO update_scripts VALUES ('update_script_Mar11_2016', NOW());