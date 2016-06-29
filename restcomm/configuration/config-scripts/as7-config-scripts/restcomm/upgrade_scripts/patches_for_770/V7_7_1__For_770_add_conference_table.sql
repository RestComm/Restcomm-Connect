--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #5
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT IFNULL(table_name, '') INTO @tblName
 FROM information_schema.columns
 WHERE table_name = 'restcomm_conference_detail_records';

	IF @tblName IS NULL THEN
		CREATE TABLE restcomm_conference_detail_records (
		sid VARCHAR(34) NOT NULL PRIMARY KEY,
		date_created DATETIME NOT NULL,
		date_updated DATETIME NOT NULL,
		account_sid VARCHAR(34) NOT NULL,
		status VARCHAR(100) NOT NULL,
		friendly_name VARCHAR(60),
		api_version VARCHAR(10) NOT NULL,
		uri MEDIUMTEXT NOT NULL
		);
	END IF;
END //

DELIMITER;
CALL updateProcedure();
drop procedure updateProcedure;
