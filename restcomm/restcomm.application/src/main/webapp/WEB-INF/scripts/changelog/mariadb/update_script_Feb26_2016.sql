#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #900
#Date: Feb 26, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#Modify table "restcomm_sms_messages", change 'body' length to 999
ALTER TABLE restcomm_sms_messages MODIFY body VARCHAR(999);

INSERT INTO update_scripts VALUES ('update_script_Feb26_2016', NOW());