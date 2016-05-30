#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1009
#Date: Mar 11, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#Modify table "restcomm_registrations", add 'instanceid' VARCHAR(255)
ALTER TABLE restcomm_registrations ADD instanceid VARCHAR(255);

INSERT INTO update_scripts VALUES ('update_script_April26_2016 for issue #1009', NOW());