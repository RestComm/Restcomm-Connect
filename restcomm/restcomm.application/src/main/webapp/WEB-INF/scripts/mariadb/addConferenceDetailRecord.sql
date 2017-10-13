DELIMITER //
DROP PROCEDURE IF EXISTS addConferenceDetailRecord;
CREATE PROCEDURE addConferenceDetailRecord(	IN in_sid VARCHAR(34),
									IN in_date_created DATETIME,
									IN in_date_updated DATETIME,
									IN in_account_sid VARCHAR(34),
									IN in_status VARCHAR(100),
									IN in_friendly_name VARCHAR(60),
									IN in_api_version VARCHAR(10),
									IN in_uri MEDIUMTEXT, 
									IN in_master_ms_id VARCHAR(34),
									IN master_present BOOLEAN )
BEGIN
	IF EXISTS(SELECT * FROM restcomm_conference_detail_records WHERE friendly_name=in_friendly_name AND account_sid=in_account_sid AND status LIKE 'RUNNING%')
	THEN
		SELECT * FROM restcomm_conference_detail_records WHERE friendly_name=in_friendly_name AND account_sid=in_account_sid AND status LIKE 'RUNNING%';
	ELSE 
		INSERT INTO restcomm_conference_detail_records (sid, date_created, date_updated, account_sid, status, friendly_name, api_version, uri, master_ms_id, master_present) VALUES (in_sid, in_date_created, in_date_updated, in_account_sid, in_status, in_friendly_name, in_api_version, in_uri, in_master_ms_id, master_present); 
		
	END IF;
		
END //
DELIMITER ;