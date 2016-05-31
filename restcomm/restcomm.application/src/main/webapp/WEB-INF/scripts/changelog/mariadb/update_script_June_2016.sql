#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
#Date: Jun ?, 2016
#Author: Maria Farooq

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

ALTER TABLE restcomm_call_detail_records ADD conference_sid VARCHAR(34);
ALTER TABLE restcomm_call_detail_records ADD muted BOOLEAN; 
ALTER TABLE restcomm_call_detail_records ADD start_conference_on_enter BOOLEAN;
ALTER TABLE restcomm_call_detail_records ADD end_conference_on_exit BOOLEAN;

CREATE TABLE restcomm_conference_detail_records (
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
status VARCHAR(100) NOT NULL,
friendly_name VARCHAR(60),
api_version VARCHAR(10) NOT NULL,
uri MEDIUMTEXT NOT NULL
);

INSERT INTO update_scripts VALUES ('update_script_June_2016 for issue #5', NOW());