#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #798
#Date: Jan 21, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE restcomm;

#Modify table "restcomm_incoming_phone_numbers", add columns for ussd url, fallback and ussd application sid to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_incoming_phone_numbers;
ALTER TABLE temp_table ADD ussd_url MEDIUMTEXT;
ALTER TABLE temp_table ADD ussd_method VARCHAR(4);
ALTER TABLE temp_table ADD ussd_fallback_url MEDIUMTEXT;
ALTER TABLE temp_table ADD ussd_fallback_method VARCHAR(4);
ALTER TABLE temp_table ADD ussd_application_sid VARCHAR(34);
INSERT INTO temp_table
(
  sid,
  date_created,
  date_updated,
  friendly_name,
  account_sid,
  phone_number,
  api_version,
  voice_caller_id_lookup,
  voice_url,
  voice_method,
  voice_fallback_url,
  voice_fallback_method,
  status_callback,
  status_callback_method,
  voice_application_sid,
  sms_url,
  sms_method,
  sms_fallback_url,
  sms_fallback_method,
  sms_application_sid,
  uri,
  voice_capable,
  sms_capable,
  mms_capable,
  fax_capable,
  pure_sip,
  cost
) SELECT
  sid as sid,
  date_created as date_created,
  date_updated as date_updated,
  friendly_name as friendly_name,
  account_sid as account_sid,
  phone_number as phone_number,
  api_version as api_version,
  voice_caller_id_lookup as voice_caller_id_lookup,
  voice_url as voice_url,
  voice_method as voice_method,
  voice_fallback_url as voice_fallback_url,
  voice_fallback_method as voice_fallback_method,
  status_callback as status_callback,
  status_callback_method as status_callback_method,
  voice_application_sid as voice_application_sid,
  sms_url as sms_url,
  sms_method as sms_method,
  sms_fallback_url as sms_fallback_url,
  sms_fallback_method as sms_fallback_method,
  sms_application_sid as sms_application_sid,
  uri as uri,
  voice_capable as voice_capable,
  sms_capable as sms_capable,
  mms_capable as mms_capable,
  fax_capable as fax_capable,
  pure_sip as pure_sip,
  cost as cost
FROM restcomm_incoming_phone_numbers;
DROP TABLE restcomm_incoming_phone_numbers;
ALTER TABLE temp_table RENAME restcomm_incoming_phone_numbers;