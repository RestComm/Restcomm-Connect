-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1104
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
/* Create index on restcomm_call_detail_records on conference_sid column */
CREATE INDEX idx_cdr_conference_sid ON restcomm_call_detail_records (conference_sid);

/* Create index on restcomm_conference_detail_records on status column */
CREATE INDEX idx_cdr_conference_status ON restcomm_conference_detail_records (status);