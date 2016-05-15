--SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #900
--Date: Feb 26, 2016
--Author: George Vagenas

USE ${RESTCOMM_DBNAME};
DELIMITER //
CREATE PROCEDURE updateProcedure()
 BEGIN
   #--Modify table "restcomm_sms_messages", change 'body' length to 999
   ALTER TABLE restcomm_sms_messages MODIFY body VARCHAR(999);
END //

DELIMITER;
CALL updateProcedure();
drop procedure updateProcedure;
