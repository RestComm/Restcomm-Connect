-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1104
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT DISTINCTROW IFNULL(table_name, '') INTO @tblName802
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_media_resource_broker_entity';

	IF @tblName802 IS NULL THEN
		CREATE TABLE restcomm_media_resource_broker_entity (
		conference_sid VARCHAR(34) NOT NULL, 
		slave_ms_id VARCHAR(34) NOT NULL, 
		slave_ms_bridge_ep_id VARCHAR(34),
		slave_ms_cnf_ep_id VARCHAR(34),
		is_bridged_together BOOLEAN NOT NULL DEFAULT FALSE, 
		PRIMARY KEY (conference_sid , slave_ms_id)
	);
	END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
