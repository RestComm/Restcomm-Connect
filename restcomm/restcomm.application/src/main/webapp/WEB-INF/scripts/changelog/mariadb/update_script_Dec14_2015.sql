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

-- applying new applications structure
SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_applications'
AND column_name = 'project_sid';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Modify table "restcomm_applications", move column project_sid to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_applications;
ALTER TABLE temp_table DROP project_sid;
ALTER TABLE temp_table ADD project_sid VARCHAR(34);
INSERT INTO temp_table
(
  sid,
  date_created,
  date_updated,
  friendly_name,
  account_sid,
  api_version,
  voice_caller_id_lookup,
  uri,
  rcml_url,
  kind,
  project_sid
)
SELECT
  sid as sid,
  date_created as date_created,
  date_updated as date_updated,
  friendly_name as friendly_name,
  account_sid as account_sid,
  api_version as api_version,
  voice_caller_id_lookup as voice_caller_id_lookup,
  uri as uri,
  rcml_url as rcml_url,
  kind as kind,
  project_sid as project_sid
FROM restcomm_applications;
DROP TABLE restcomm_applications;
ALTER TABLE temp_table RENAME restcomm_applications;
END IF;

