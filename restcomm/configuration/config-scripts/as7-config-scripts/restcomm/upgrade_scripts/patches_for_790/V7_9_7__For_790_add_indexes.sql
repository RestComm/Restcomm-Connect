-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1104
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
BEGIN
 IF((SELECT COUNT(*) AS index_exists FROM information_schema.statistics WHERE TABLE_SCHEMA = DATABASE() and table_name = 'restcomm_call_detail_records' AND index_name = 'idx_cdr_conference_sid') = 0)
 THEN
   CREATE INDEX idx_cdr_conference_sid ON restcomm_call_detail_records (conference_sid);
 END IF;
IF((SELECT COUNT(*) AS index_exists FROM information_schema.statistics WHERE TABLE_SCHEMA = DATABASE() and table_name = 'restcomm_conference_detail_records' AND index_name = 'idx_cdr_conference_status') = 0)
 THEN
   CREATE INDEX idx_cdr_conference_status ON restcomm_conference_detail_records (status);
 END IF;
END //
DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
