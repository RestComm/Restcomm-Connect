#SQL Script for MySQL/MariaDB to update DB with the schema changes
#Date: Aug 07, 2017
#Author: Oleg Agafonov

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE restcomm;

#Modify table "restcomm_clients", add column for push client identity to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_clients;
ALTER TABLE temp_table ADD push_client_identity VARCHAR(34);
INSERT INTO temp_table
(
  sid,
  date_created,
  date_updated,
  account_sid,
  api_version,
  friendly_name,
  login,
  password,
  status,
  voice_url,
  voice_method,
  voice_fallback_url,
  voice_fallback_method,
  voice_application_sid,
  uri
) SELECT
  sid as sid,
  date_created as date_created,
  date_updated as date_updated,
  account_sid as account_sid,
  api_version as api_version,
  friendly_name as friendly_name,
  login as login,
  password as password,
  status as status,
  voice_url as voice_url,
  voice_method as voice_method,
  voice_fallback_url as voice_fallback_url,
  voice_fallback_method as voice_fallback_method,
  voice_application_sid as voice_application_sid,
  uri as uri
FROM restcomm_clients;
DROP TABLE restcomm_clients;
ALTER TABLE temp_table RENAME restcomm_clients;