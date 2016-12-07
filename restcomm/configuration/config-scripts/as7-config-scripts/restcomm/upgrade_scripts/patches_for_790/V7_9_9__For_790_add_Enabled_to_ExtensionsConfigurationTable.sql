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
  ALTER TABLE restcomm_extensions_configuration add enabled BOOLEAN NOT NULL DEFAULT FALSE, ALGORITHM = INPLACE, LOCK = NONE;
END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
