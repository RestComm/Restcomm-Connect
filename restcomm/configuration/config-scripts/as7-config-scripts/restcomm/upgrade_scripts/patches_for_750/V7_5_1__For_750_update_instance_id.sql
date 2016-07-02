--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #923

--Modify table "restcomm_instance_id", add 'host' VARCHAR(255)
USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT DISTINCTROW IFNULL(column_name, '') INTO @colName751
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_instance_id'
 AND column_name = 'host';

IF @colName751 IS NULL THEN
  CREATE TABLE temp_table LIKE restcomm_instance_id;
  ALTER TABLE temp_table ADD host VARCHAR(255);
  INSERT INTO temp_table
    (
    instance_id,
    date_created,
    date_updated
    ) SELECT
    instance_id as instance_id,
    date_created as date_created,
    date_updated as date_updated
  FROM restcomm_instance_id;
  DROP TABLE restcomm_instance_id;
  ALTER TABLE temp_table RENAME restcomm_instance_id;
END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
