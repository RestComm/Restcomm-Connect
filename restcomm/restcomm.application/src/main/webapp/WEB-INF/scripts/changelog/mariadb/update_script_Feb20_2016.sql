#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #885
#Date: Feb 20, 2016
#Author: George Vagenas

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

CREATE TABLE update_scripts (
script VARCHAR(255) NOT NULL,
date_executed DATETIME NOT NULL
);

INSERT INTO update_scripts VALUES ('update_script_Feb20_2016', NOW());