CREATE DATABASE IF NOT EXISTS restcomm;
USE restcomm;

CREATE TABLE restcomm_accounts (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
email_address MEDIUMTEXT NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34),
type VARCHAR(8) NOT NULL,
status VARCHAR(16) NOT NULL,
auth_token VARCHAR(32) NOT NULL,
role VARCHAR(64) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_announcements (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
account_sid VARCHAR(34),
gender VARCHAR(8) NOT NULL,
language VARCHAR(16) NOT NULL,
text VARCHAR(32) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_available_phone_numbers (
friendly_name VARCHAR(64) NOT NULL,
phone_number VARCHAR(15) NOT NULL PRIMARY KEY,
lata SMALLINT,
rate_center VARCHAR(32),
latitude DOUBLE,
longitude DOUBLE,
region VARCHAR(2),
postal_code INT,
iso_country VARCHAR(2) NOT NULL
);

CREATE TABLE restcomm_outgoing_caller_ids (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
phone_number VARCHAR(15) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_http_cookies (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
comment MEDIUMTEXT,
domain MEDIUMTEXT,
expiration_date DATETIME,
name MEDIUMTEXT NOT NULL,
path MEDIUMTEXT,
value MEDIUMTEXT,
version INT
);

CREATE TABLE restcomm_incoming_phone_numbers (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
phone_number VARCHAR(15) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_caller_id_lookup BOOLEAN NOT NULL,
voice_url MEDIUMTEXT,
voice_method VARCHAR(4),
voice_fallback_url MEDIUMTEXT,
voice_fallback_method VARCHAR(4),
status_callback MEDIUMTEXT,
status_callback_method VARCHAR(4),
voice_application_sid VARCHAR(34),
sms_url MEDIUMTEXT,
sms_method VARCHAR(4),
sms_fallback_url MEDIUMTEXT,
sms_fallback_method VARCHAR(4),
sms_application_sid VARCHAR(34),
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_applications (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_url MEDIUMTEXT,
voice_method VARCHAR(4),
voice_fallback_url MEDIUMTEXT,
voice_fallback_method VARCHAR(4),
status_callback MEDIUMTEXT,
status_callback_method VARCHAR(4),
voice_caller_id_lookup BOOLEAN NOT NULL,
sms_url MEDIUMTEXT,
sms_method VARCHAR(4),
sms_fallback_url MEDIUMTEXT,
sms_fallback_method VARCHAR(4),
sms_status_callback MEDIUMTEXT,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_call_detail_records (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
parent_call_sid VARCHAR(34),
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
sender VARCHAR(15) NOT NULL,
recipient VARCHAR(15) NOT NULL,
phone_number_sid VARCHAR(34),
status VARCHAR(11) NOT NULL,
start_time DATETIME,
end_time DATETIME,
duration INT,
price VARCHAR(8),
direction VARCHAR(13) NOT NULL,
answered_by VARCHAR(7),
api_version VARCHAR(10) NOT NULL,
forwarded_from VARCHAR(15),
caller_name VARCHAR(30),
uri MEDIUMTEXT NOT NULL,
call_path VARCHAR(255)
);

CREATE TABLE restcomm_clients (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
login VARCHAR(64) NOT NULL,
password VARCHAR(64) NOT NULL,
status INT NOT NULL,
voice_url MEDIUMTEXT,
voice_method VARCHAR(4),
voice_fallback_url MEDIUMTEXT,
voice_fallback_method VARCHAR(4),
voice_application_sid VARCHAR(34),
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_registrations (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
date_expires DATETIME NOT NULL,
address_of_record MEDIUMTEXT NOT NULL,
display_name VARCHAR(255),
user_name VARCHAR(64) NOT NULL,
user_agent MEDIUMTEXT,
ttl INT NOT NULL,
location MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_short_codes (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
short_code INT NOT NULL,
api_version VARCHAR(10) NOT NULL,
sms_url MEDIUMTEXT,
sms_method VARCHAR(4),
sms_fallback_url MEDIUMTEXT,
sms_fallback_method VARCHAR(4),
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_sms_messages (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
date_sent DATETIME,
account_sid VARCHAR(34) NOT NULL,
sender VARCHAR(15) NOT NULL,
recipient VARCHAR(15) NOT NULL,
body VARCHAR(160) NOT NULL,
status VARCHAR(7) NOT NULL,
direction VARCHAR(14) NOT NULL,
price VARCHAR(8) NOT NULL,
api_version VARCHAR(10) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_recordings (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
call_sid VARCHAR(34) NOT NULL,
duration DOUBLE NOT NULL,
api_version VARCHAR(10) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_transcriptions (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
status VARCHAR(11) NOT NULL,
recording_sid VARCHAR(34) NOT NULL,
duration DOUBLE NOT NULL,
transcription_text MEDIUMTEXT NOT NULL,
price VARCHAR(8) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_notifications (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
call_sid VARCHAR(34),
api_version VARCHAR(10) NOT NULL,
log TINYINT NOT NULL,
error_code SMALLINT NOT NULL,
more_info MEDIUMTEXT NOT NULL,
message_text MEDIUMTEXT NOT NULL,
message_date DATETIME NOT NULL,
request_url MEDIUMTEXT NOT NULL,
request_method VARCHAR(4) NOT NULL,
request_variables MEDIUMTEXT NOT NULL,
response_headers MEDIUMTEXT,
response_body MEDIUMTEXT,
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_sand_boxes (
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
pin VARCHAR(8) NOT NULL,
account_sid VARCHAR(34) NOT NULL PRIMARY KEY,
phone_number VARCHAR(15) NOT NULL,
application_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_url MEDIUMTEXT,
voice_method VARCHAR(4),
sms_url MEDIUMTEXT,
sms_method VARCHAR(4),
status_callback MEDIUMTEXT,
status_callback_method VARCHAR(4),
uri MEDIUMTEXT NOT NULL
);

CREATE TABLE restcomm_gateways (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
friendly_name VARCHAR(64),
user_name VARCHAR(255),
password VARCHAR(255),
proxy MEDIUMTEXT NOT NULL,
register BOOLEAN NOT NULL,
ttl INT NOT NULL,
uri MEDIUMTEXT NOT NULL
);

INSERT INTO restcomm_accounts VALUES (
"ACae6e420f425248d6a26948c17a9e2acf",
Date("2012-04-24"),
Date("2012-04-24"),
"administrator@company.com",
"Default Administrator Account",
null,
"Full",
"active",
"77f8c12cc7b8f8423e5c38b035249166",
"Administrator",
"/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf");