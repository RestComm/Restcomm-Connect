--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1104
--Date: Jan 21, 2016
--Author: George Vagenas

--Modify TABLE restcomm_conference_detail_records add columns master_ms_id VARCHAR(34),
master_conference_endpoint_id VARCHAR(20),
master_present BOOLEAN NOT NULL DEFAULT TRUE, 
master_ivr_endpoint_id VARCHAR(20),
master_ivr_endpoint_session_id VARCHAR(200),
master_bridge_endpoint_id VARCHAR(20),
master_bridge_endpoint_session_id VARCHAR(200),
master_bridge_conn_id VARCHAR(200),
master_ivr_conn_id VARCHAR(200)
);

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
   SELECT DISTINCTROW IFNULL(column_name, '') INTO @colName794
   FROM information_schema.columns
   WHERE table_schema='${RESTCOMM_DBNAME}'
   AND table_name = 'restcomm_conference_detail_records'
   AND column_name = 'master_ms_id';

  IF @colName794 IS NULL THEN
  CREATE TABLE temp_table LIKE restcomm_conference_detail_records;
  ALTER TABLE temp_table ADD master_ms_id VARCHAR(34);
  ALTER TABLE temp_table ADD master_conference_endpoint_id VARCHAR(20);
  ALTER TABLE temp_table ADD master_present BOOLEAN NOT NULL DEFAULT TRUE;
  ALTER TABLE temp_table ADD master_ivr_endpoint_id VARCHAR(20);
  ALTER TABLE temp_table ADD master_ivr_endpoint_session_id VARCHAR(200);
  ALTER TABLE temp_table ADD master_bridge_endpoint_id VARCHAR(20);
  ALTER TABLE temp_table ADD master_bridge_endpoint_session_id VARCHAR(200);
  ALTER TABLE temp_table ADD master_bridge_conn_id VARCHAR(200);
  ALTER TABLE temp_table ADD master_ivr_conn_id VARCHAR(200);

  INSERT INTO temp_table
  (
	sid,
	date_created,
	date_updated,
	account_sid,
	status,
	friendly_name,
	api_version,
	uri
  ) SELECT
	sid as sid,
	date_created as date_created,
	date_updated as date_updated,
	account_sid as account_sid,
	status as status,
	friendly_name as friendly_name,
	api_version as api_version,
	uri as uri
  FROM restcomm_conference_detail_records;
  DROP TABLE restcomm_conference_detail_records;
  ALTER TABLE temp_table RENAME restcomm_conference_detail_records;
  END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
