CREATE TABLE accounts (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
type VARCHAR(8) NOT NULL,
status VARCHAR(16) NOT NULL,
auth_token VARCHAR(32) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE sub_accounts (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
type VARCHAR(8) NOT NULL,
status VARCHAR(16) NOT NULL,
auth_token VARCHAR(32) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE available_phone_numbers (
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

CREATE TABLE outgoing_caller_ids (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
phone_number VARCHAR(15) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE incoming_phone_numbers (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
phone_number VARCHAR(15) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_caller_id_lookup BOOLEAN NOT NULL,
voice_url LONGVARCHAR,
voice_method VARCHAR(4),
voice_fallback_url LONGVARCHAR,
voice_fallback_method VARCHAR(4),
status_callback LONGVARCHAR,
status_callback_method VARCHAR(4),
voice_application_sid VARCHAR(34),
sms_url LONGVARCHAR,
sms_method VARCHAR(4),
sms_fallback_url LONGVARCHAR,
sms_fallback_method VARCHAR(4),
sms_application_sid VARCHAR(34),
uri LONGVARCHAR NOT NULL
);

CREATE TABLE applications (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_url LONGVARCHAR,
voice_method VARCHAR(4),
voice_fallback_url LONGVARCHAR,
voice_fallback_method VARCHAR(4),
status_callback LONGVARCHAR,
status_callback_method VARCHAR(4),
voice_caller_id_lookup BOOLEAN NOT NULL,
sms_url LONGVARCHAR,
sms_method VARCHAR(4),
sms_fallback_url LONGVARCHAR,
sms_fallback_method VARCHAR(4),
sms_status_callback LONGVARCHAR,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE short_codes (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
friendly_name VARCHAR(64) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
short_code INT NOT NULL,
api_version VARCHAR(10) NOT NULL,
sms_url LONGVARCHAR,
sms_method VARCHAR(4),
sms_fallback_url LONGVARCHAR,
sms_fallback_method VARCHAR(4),
uri LONGVARCHAR NOT NULL
);

CREATE TABLE sms_messages (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
date_sent DATE NOT NULL,
account_sid VARCHAR(34) NOT NULL,
sender VARCHAR(15) NOT NULL,
recipient VARCHAR(15) NOT NULL,
body VARCHAR(160) NOT NULL,
status VARCHAR(7) NOT NULL,
direction VARCHAR(14) NOT NULL,
price VARCHAR(8) NOT NULL,
api_version VARCHAR(10) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE recordings (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
account_sid VARCHAR(34) NOT NULL,
call_sid VARCHAR(34) NOT NULL,
duration INT NOT NULL,
api_version VARCHAR(10) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE transcriptions (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
account_sid VARCHAR(34) NOT NULL,
status VARCHAR(11) NOT NULL,
recording_sid VARCHAR(34) NOT NULL,
duration INT NOT NULL,
transcription_text LONGVARCHAR NOT NULL,
price VARCHAR(8) NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE notifications (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
account_sid VARCHAR(34) NOT NULL,
call_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
log TINYINT NOT NULL,
error_code SMALLINT NOT NULL,
more_info LONGVARCHAR NOT NULL,
message_text LONGVARCHAR NOT NULL,
message_date DATE NOT NULL,
request_url LONGVARCHAR NOT NULL,
request_method VARCHAR(4) NOT NULL,
request_variables LONGVARCHAR NOT NULL,
response_headers LONGVARCHAR NOT NULL,
response_body LONGVARCHAR NOT NULL,
uri LONGVARCHAR NOT NULL
);

CREATE TABLE sand_boxes (
date_created DATE NOT NULL,
date_updated DATE NOT NULL,
pin VARCHAR(8) NOT NULL,
account_sid VARCHAR(34) NOT NULL,
phone_number VARCHAR(15) NOT NULL,
application_sid VARCHAR(34) NOT NULL,
api_version VARCHAR(10) NOT NULL,
voice_url LONGVARCHAR,
voice_method VARCHAR(4),
sms_url LONGVARCHAR,
sms_method VARCHAR(4),
status_callback LONGVARCHAR,
status_callback_method VARCHAR(4),
uri LONGVARCHAR NOT NULL
);