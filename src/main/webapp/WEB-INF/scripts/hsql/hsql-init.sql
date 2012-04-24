CREATE TABLE "restcomm_accounts" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"type" VARCHAR(8) NOT NULL,
"status" VARCHAR(16) NOT NULL,
"auth_token" VARCHAR(32) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_sub_accounts" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"type" VARCHAR(8) NOT NULL,
"status" VARCHAR(16) NOT NULL,
"auth_token" VARCHAR(32) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_available_phone_numbers" (
"friendly_name" VARCHAR(64) NOT NULL,
"phone_number" VARCHAR(15) NOT NULL PRIMARY KEY,
"lata" SMALLINT,
"rate_center" VARCHAR(32),
"latitude" DOUBLE,
"longitude" DOUBLE,
"region" VARCHAR(2),
"postal_code" INT,
"iso_country" VARCHAR(2) NOT NULL
);

CREATE TABLE "restcomm_outgoing_caller_ids" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"phone_number" VARCHAR(15) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_incoming_phone_numbers" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"phone_number" VARCHAR(15) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"voice_caller_id_lookup" BOOLEAN NOT NULL,
"voice_url" LONGVARCHAR,
"voice_method" VARCHAR(4),
"voice_fallback_url" LONGVARCHAR,
"voice_fallback_method" VARCHAR(4),
"status_callback" LONGVARCHAR,
"status_callback_method" VARCHAR(4),
"voice_application_sid" VARCHAR(34),
"sms_url" LONGVARCHAR,
"sms_method" VARCHAR(4),
"sms_fallback_url" LONGVARCHAR,
"sms_fallback_method" VARCHAR(4),
"sms_application_sid" VARCHAR(34),
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_applications" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"voice_url" LONGVARCHAR,
"voice_method" VARCHAR(4),
"voice_fallback_url" LONGVARCHAR,
"voice_fallback_method" VARCHAR(4),
"status_callback" LONGVARCHAR,
"status_callback_method" VARCHAR(4),
"voice_caller_id_lookup" BOOLEAN NOT NULL,
"sms_url" LONGVARCHAR,
"sms_method" VARCHAR(4),
"sms_fallback_url" LONGVARCHAR,
"sms_fallback_method" VARCHAR(4),
"sms_status_callback" LONGVARCHAR,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_call_detail_records" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"parent_call_sid" VARCHAR(34),
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"sender" VARCHAR(15) NOT NULL,
"recipient" VARCHAR(15) NOT NULL,
"phone_number_sid" VARCHAR(34) NOT NULL,
"status" VARCHAR(11) NOT NULL,
"start_time" DATE,
"end_time" DATE,
"duration" INT,
"price" VARCHAR(8),
"answered_by" VARCHAR(7) NOT NULL,
"forwarded_from" VARCHAR(15),
"caller_name" VARCHAR(30) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_clients" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"login" VARCHAR(64) NOT NULL,
"password" VARCHAR(64) NOT NULL,
"status" INT NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_presence_records" (
"address_of_record" LONGVARCHAR NOT NULL,
"display_name" VARCHAR(255) NOT NULL,
"uri" LONGVARCHAR NOT NULL,
"user_agent" LONGVARCHAR NOT NULL,
"ttl" INT NOT NULL
);

CREATE TABLE "restcomm_short_codes" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"friendly_name" VARCHAR(64) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"short_code" INT NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"sms_url" LONGVARCHAR,
"sms_method" VARCHAR(4),
"sms_fallback_url" LONGVARCHAR,
"sms_fallback_method" VARCHAR(4),
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_sms_messages" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"date_sent" DATE,
"account_sid" VARCHAR(34) NOT NULL,
"sender" VARCHAR(15) NOT NULL,
"recipient" VARCHAR(15) NOT NULL,
"body" VARCHAR(160) NOT NULL,
"status" VARCHAR(7) NOT NULL,
"direction" VARCHAR(14) NOT NULL,
"price" VARCHAR(8) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_recordings" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"call_sid" VARCHAR(34) NOT NULL,
"duration" DOUBLE NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_transcriptions" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"status" VARCHAR(11) NOT NULL,
"recording_sid" VARCHAR(34) NOT NULL,
"duration" DOUBLE NOT NULL,
"transcription_text" LONGVARCHAR NOT NULL,
"price" VARCHAR(8) NOT NULL,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_notifications" (
"sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"account_sid" VARCHAR(34) NOT NULL,
"call_sid" VARCHAR(34) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"log" TINYINT NOT NULL,
"error_code" SMALLINT NOT NULL,
"more_info" LONGVARCHAR NOT NULL,
"message_text" LONGVARCHAR NOT NULL,
"message_date" DATE NOT NULL,
"request_url" LONGVARCHAR NOT NULL,
"request_method" VARCHAR(4) NOT NULL,
"request_variables" LONGVARCHAR NOT NULL,
"response_headers" LONGVARCHAR,
"response_body" LONGVARCHAR,
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_sand_boxes" (
"date_created" DATE NOT NULL,
"date_updated" DATE NOT NULL,
"pin" VARCHAR(8) NOT NULL,
"account_sid" VARCHAR(34) NOT NULL PRIMARY KEY,
"phone_number" VARCHAR(15) NOT NULL,
"application_sid" VARCHAR(34) NOT NULL,
"api_version" VARCHAR(10) NOT NULL,
"voice_url" LONGVARCHAR,
"voice_method" VARCHAR(4),
"sms_url" LONGVARCHAR,
"sms_method" VARCHAR(4),
"status_callback" LONGVARCHAR,
"status_callback_method" VARCHAR(4),
"uri" LONGVARCHAR NOT NULL
);

CREATE TABLE "restcomm_gateways" (
"name" VARCHAR(255) NOT NULL PRIMARY KEY,
"user" VARCHAR(255),
"password" VARCHAR(255),
"proxy" LONGVARCHAR NOT NULL,
"register" BOOLEAN NOT NULL
);
