#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #461, #4 and #87
#Date: Oct 27
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#USE restcomm;

SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_applications'
AND column_name = 'kind';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Drop and create again the "restcomm_applications" table
DROP TABLE restcomm_applications;
CREATE TABLE restcomm_applications (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_caller_id_lookup BOOLEAN NOT NULL,
uri MEDIUMTEXT NOT NULL,
rcml_url MEDIUMTEXT,
kind VARCHAR(5)
);
END IF;

SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_available_phone_numbers'
AND column_name = 'cost';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Modify table "restcomm_available_phone_numbers", move column cost to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_available_phone_numbers;
ALTER TABLE temp_table DROP cost;
ALTER TABLE temp_table ADD cost VARCHAR(10);
INSERT INTO temp_table
(
  friendly_name,
  phone_number,
  lata,
  rate_center,
  latitude,
  longitude,
  region,
  postal_code,
  iso_country,
  voice_capable,
  sms_capable,
  mms_capable,
  fax_capable,
  cost
)
SELECT
  friendly_name as friendly_name,
  phone_number as phone_number,
  lata as lata,
  rate_center as rate_center,
  latitude as latitude,
  region as region,
  postal_code as postal_code,
  iso_country as iso_country,
  voice_capable as voice_capable,
  sms_capable as sms_capable,
  mms_capable as mms_capable,
  fax_capable as fax_capable,
  cost as cost
FROM restcomm_available_phone_numbers;
DROP TABLE restcomm_available_phone_numbers;
ALTER TABLE temp_table RENAME restcomm_available_phone_numbers;
END IF;

SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_incoming_phone_numbers'
AND column_name = 'cost';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Modify table "restcomm_incoming_phone_numbers", move column cost to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_incoming_phone_numbers;
ALTER TABLE temp_table DROP cost;
ALTER TABLE temp_table ADD cost VARCHAR(10);
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
END IF;

SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_registrations'
AND column_name = 'webrtc';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Modify table "restcomm_incoming_phone_numbers", move column cost to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_registrations;
ALTER TABLE temp_table ADD webrtc BOOLEAN NOT NULL default false;
INSERT INTO temp_table
(
  sid,
  date_created,
  date_updated,
  date_expires,
  address_of_record,
  display_name,
  user_agent,
  ttl,
  location,
  webrtc
) SELECT
  sid as sid,
  date_created as date_created,
  date_updated as date_updated,
  date_expires as date_expires,
  address_of_record as address_of_record,
  display_name as display_name,
  user_agent as user_agent,
  ttl as ttl,
  location as location,
  FALSE as webrtc
FROM restcomm_registrations;
DROP TABLE restcomm_registrations;
ALTER TABLE temp_table RENAME restcomm_registrations;
END IF;