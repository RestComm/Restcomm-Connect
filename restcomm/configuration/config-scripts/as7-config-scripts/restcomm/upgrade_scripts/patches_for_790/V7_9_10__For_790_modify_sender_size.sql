-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
  ALTER TABLE restcomm_sms_messages MODIFY sender varchar(255), ALGORITHM = INPLACE, LOCK = NONE;
  ALTER TABLE restcomm_call_detail_records MODIFY sender varchar(255), ALGORITHM = INPLACE, LOCK = NONE;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
