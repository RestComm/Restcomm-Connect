-- SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #1104
-- #Author: Maria Farooq

-- #To run the script use mysql client:
-- #mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
 SELECT DISTINCTROW IFNULL(table_name, '') INTO @tblName792
 FROM information_schema.columns
 WHERE table_schema='${RESTCOMM_DBNAME}'
 AND table_name = 'restcomm_media_servers';

	IF @tblName792 IS NULL THEN
		CREATE TABLE restcomm_media_servers (
			ms_id INT PRIMARY KEY AUTO_INCREMENT, 
			local_ip VARCHAR(34) NOT NULL, 
			local_port INT NOT NULL,
			remote_ip VARCHAR(34) NOT NULL UNIQUE,
			remote_port INT NOT NULL, 
			compatibility VARCHAR(34) DEFAULT 'rms', 
			response_timeout VARCHAR(34),
			external_address VARCHAR(34)
		);
	END IF;
END //

DELIMITER ;
CALL updateProcedure();
drop procedure updateProcedure;
