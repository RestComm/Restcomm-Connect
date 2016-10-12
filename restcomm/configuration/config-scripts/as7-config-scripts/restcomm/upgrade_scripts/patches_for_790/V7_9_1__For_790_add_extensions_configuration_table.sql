-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT IFNULL(column_name, '') INTO @colName791
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_extensions_configuration';

IF @colName791 IS NULL THEN
		CREATE TABLE restcomm_extensions_configuration (
      extension VARCHAR(255) NOT NULL,
      property VARCHAR(255),
      extra_parameter VARCHAR(255),
      value VARCHAR(255),
      date_created DATETIME NOT NULL,
      date_updated DATETIME
		);
END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
