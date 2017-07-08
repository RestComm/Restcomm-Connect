#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1026
#Date: Apr 29
#Author: Guilherme Humberto Jansen

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#USE restcomm;

UPDATE restcomm_incoming_phone_numbers SET voice_method = 'POST' WHERE voice_method IS NULL AND voice_application_sid IS NOT NULL;
UPDATE restcomm_incoming_phone_numbers SET sms_method = 'POST' WHERE sms_method IS NULL AND sms_application_sid IS NOT NULL;
UPDATE restcomm_incoming_phone_numbers SET ussd_method = 'POST' WHERE ussd_method IS NULL AND ussd_application_sid IS NOT NULL;