-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT IFNULL(column_name, '') INTO @colName799
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_extensions_configuration'
 AND column_name = 'enabled';

IF @colName799 IS NULL THEN
CREATE TABLE temp_table LIKE restcomm_extensions_configuration;
ALTER TABLE temp_table ADD enabled BOOLEAN NOT NULL DEFAULT FALSE;
INSERT INTO temp_table
  (
sid,
extension,
configuration_data,
configuration_type,
date_created,
date_updated
  ) SELECT
  sid as sid,
  extension as extension,
  configuration_data as configuration_data,
  configuration_type as configuration_type,
  date_created as date_created,
  date_updated as date_updated
  FROM restcomm_extensions_configuration;
  DROP TABLE restcomm_extensions_configuration;
  ALTER TABLE temp_table RENAME restcomm_extensions_configuration;
END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
