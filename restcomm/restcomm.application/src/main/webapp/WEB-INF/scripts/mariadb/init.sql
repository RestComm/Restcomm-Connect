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
iso_country VARCHAR(2) NOT NULL,
voice_capable BOOLEAN,
sms_capable BOOLEAN,
mms_capable BOOLEAN,
fax_capable BOOLEAN
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
uri MEDIUMTEXT NOT NULL,
voice_capable BOOLEAN,
sms_capable BOOLEAN,
mms_capable BOOLEAN,
fax_capable BOOLEAN
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
"uninitialized",
"77f8c12cc7b8f8423e5c38b035249166",
"Administrator",
"/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf");

/* Bind default DID to demo apps */
INSERT INTO restcomm_incoming_phone_numbers VALUES('PNdd7a0a0248244615978bd5781598e5eb','2013-10-04 17:42:02.500000000','2013-10-04 17:42:02.500000000','234','ACae6e420f425248d6a26948c17a9e2acf','+1234','2012-04-24',FALSE,'/restcomm/demos/hello-play.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PNdd7a0a0248244615978bd5781598e5eb', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN146638eec1e2415d832785e30d227598','2013-10-11 14:56:08.549000000','2013-10-11 14:56:08.549000000','235','ACae6e420f425248d6a26948c17a9e2acf','+1235','2012-04-24',FALSE,'/restcomm/demos/hello-world.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN146638eec1e2415d832785e30d227598', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PNabf9c98b95d64b26b5993ad52e809566','2013-10-11 14:55:56.670000000','2013-10-11 14:55:56.670000000','236','ACae6e420f425248d6a26948c17a9e2acf','+1236','2012-04-24',FALSE,'/restcomm/demos/gather/hello-gather.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PNabf9c98b95d64b26b5993ad52e809566', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN91275300c95547039c3723a1e58b5662','2013-10-31 22:12:19.318000000','2013-10-31 22:12:19.318000000','237','ACae6e420f425248d6a26948c17a9e2acf','+1237','2012-04-24',FALSE,'/restcomm/demos/dial/sip/dial-sip.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN91275300c95547039c3723a1e58b5662', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN0b4201c6c87749f29367e6cf000686cb','2013-11-04 12:14:10.520000000','2013-11-04 12:14:10.520000000','238','ACae6e420f425248d6a26948c17a9e2acf','+1238','2012-04-24',FALSE,'/restcomm/demos/dial/client/dial-client.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN0b4201c6c87749f29367e6cf000686cb', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN9f27f81e725640d988486ff15f48ad18','2013-11-04 12:42:11.530000000','2013-11-04 12:42:11.530000000','310','ACae6e420f425248d6a26948c17a9e2acf','+1310','2012-04-24',FALSE,'/restcomm/demos/dial/conference/dial-conference.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN9f27f81e725640d988486ff15f48ad18', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN3862668c51634d18ae027c63438b4583','2013-11-04 12:42:44.777000000','2013-11-04 12:42:44.777000000','311','ACae6e420f425248d6a26948c17a9e2acf','+1311','2012-04-24',FALSE,'/restcomm/demos/dial/conference/dial-conference-moderator.xml','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN3862668c51634d18ae027c63438b4583', true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PNc2b81d68a221482ea387b6b4e2cbd9d7','2014-02-17 22:36:58.008000000','2014-02-17 22:36:58.008000000','239','ACae6e420f425248d6a26948c17a9e2acf','+1239','2012-04-24',FALSE,'/restcomm-rvd/services/apps/rvdSayVerbDemo/controller','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PNc2b81d68a221482ea387b6b4e2cbd9d7',true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PN46678e5b01d44973bf184f6527bc33f7','2014-02-17 22:37:08.709000000','2014-02-17 22:37:08.709000000','240','ACae6e420f425248d6a26948c17a9e2acf','+1240','2012-04-24',FALSE,'/restcomm-rvd/services/apps/rvdCollectVerbDemo/controller','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN46678e5b01d44973bf184f6527bc33f7',true, false, false, false);
INSERT INTO restcomm_incoming_phone_numbers VALUES('PNb43ed9e641364277b6432547ff1109e9','2014-02-17 22:37:19.392000000','2014-02-17 22:37:19.392000000','241','ACae6e420f425248d6a26948c17a9e2acf','+1241','2012-04-24',FALSE,'/restcomm-rvd/services/apps/rvdESDemo/controller','POST',NULL,'POST',NULL,'POST',NULL,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PNb43ed9e641364277b6432547ff1109e9',true, false, false, false);

/* Create demo clients */
INSERT INTO restcomm_clients VALUES('CLa2b99142e111427fbb489c3de357f60a','2013-11-04 12:52:44.144000000','2013-11-04 12:52:44.144000000','ACae6e420f425248d6a26948c17a9e2acf','2012-04-24','alice','alice','1234',1,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Clients/CLa2b99142e111427fbb489c3de357f60a');
INSERT INTO restcomm_clients VALUES('CL3003328d0de04ba68f38de85b732ed56','2013-11-04 16:33:39.248000000','2013-11-04 16:33:39.248000000','ACae6e420f425248d6a26948c17a9e2acf','2012-04-24','bob','bob','1234',1,NULL,'POST',NULL,'POST',NULL,'/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Clients/CL3003328d0de04ba68f38de85b732ed56');
