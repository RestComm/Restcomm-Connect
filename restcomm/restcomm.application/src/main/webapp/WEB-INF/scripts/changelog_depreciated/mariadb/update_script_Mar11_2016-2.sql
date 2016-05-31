#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #521
#Date: Mar 11, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#Modify table "restcomm_call_detail_records", add 'instanceid' VARCHAR(255)
ALTER TABLE restcomm_call_detail_records ADD instanceid VARCHAR(255);

INSERT INTO update_scripts VALUES ('update_script_Mar11_2016-2 for issue #521', NOW());