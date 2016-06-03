--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #994
--Author: Orestis Tsakiridis

--Modify table "restcomm_accounts", add linked property
USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
   SELECT IFNULL(column_name, '') INTO @colName
   FROM information_schema.columns
   WHERE table_name = 'restcomm_accounts'
   AND column_name = 'linked';

  IF @colName IS NULL THEN
  CREATE TABLE temp_table LIKE restcomm_accounts;
  ALTER TABLE temp_table ADD linked BOOLEAN;
  INSERT INTO temp_table
  (
    sid,
    date_created,
    date_updated,
    email_address,
    friendly_name,
    account_sid,
    type,
    status,
    auth_token,
    role,
    uri,
    linked
  ) SELECT
    sid as sid,
    date_created as date_created,
    date_updated as date_updated,
    email_address as email_address,
    friendly_name as friendly_name,
    account_sid as account_sid,
    type as type,
    status as status,
    auth_token as auth_token,
    role as role,
    uri as uri,
    linked as linked
  FROM restcomm_accounts;
  DROP TABLE restcomm_accounts;
  ALTER TABLE temp_table RENAME restcomm_accounts;
  END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
