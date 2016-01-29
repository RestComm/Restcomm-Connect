#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #530
#Date: Dec 14
#Author: Guilherme Humberto Jansen

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE restcomm;

-- backup applications
CREATE TABLE restcomm_applications_migrationbkp LIKE restcomm_applications;
INSERT restcomm_applications_migrationbkp SELECT * FROM restcomm_applications;

-- backup incoming phone numbers
CREATE TABLE restcomm_incoming_phone_numbers_migrationbkp LIKE restcomm_incoming_phone_numbers;
INSERT restcomm_incoming_phone_numbers_migrationbkp SELECT * FROM restcomm_incoming_phone_numbers;

-- backup clients
CREATE TABLE restcomm_clients_migrationbkp LIKE restcomm_clients;
INSERT restcomm_clients_migrationbkp SELECT * FROM restcomm_clients;
