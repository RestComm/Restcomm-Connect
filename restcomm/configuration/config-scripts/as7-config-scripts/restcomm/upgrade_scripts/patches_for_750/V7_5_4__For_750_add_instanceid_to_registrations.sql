--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1009
-- #Author: George Vagenas

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT DISTINCTROW IFNULL(column_name, '') INTO @colName
 FROM information_schema.columns
 WHERE table_name = 'restcomm_registrations'
 AND column_name = 'instanceid';

IF @colName IS NULL THEN
CREATE TABLE temp_table LIKE restcomm_registrations;
ALTER TABLE temp_table ADD instanceid VARCHAR(255);
INSERT INTO temp_table
  (
    sid,
    date_created,
    date_updated,
    date_expires,
    address_of_record,
    display_name,
    user_name,
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
  user_name as user_name,
  user_agent as user_agent,
  ttl as ttl,
  location as location,
  webrtc as webrtc
  FROM restcomm_registrations;
  DROP TABLE restcomm_registrations;
  ALTER TABLE temp_table RENAME restcomm_registrations;
END IF;
END //

DELIMITER;
CALL updateProcedure();
drop procedure updateProcedure;
