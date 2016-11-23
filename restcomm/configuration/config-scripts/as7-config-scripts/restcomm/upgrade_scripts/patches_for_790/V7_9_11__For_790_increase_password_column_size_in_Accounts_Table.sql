-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
-- #Author: Orestis Tsakiridis

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT DISTINCTROW IFNULL(character_maximum_length, '') INTO @colSize7911
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_accounts'
 AND column_name = 'password';

IF @colSize7911 = 32 THEN
ALTER TABLE restcomm_accounts CHANGE column password password varchar(60);
END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
