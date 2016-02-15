#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #748
#Date: Jan 11
#Author: Guilherme Humberto Jansen

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE restcomm;

SELECT IFNULL(table_name, '') INTO @tabName
FROM information_schema.tables 
WHERE table_name = 'restcomm_organizations'

IF @tabName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Drop and create again the "restcomm_organizations" table
DROP TABLE restcomm_organizations;
CREATE TABLE restcomm_organizations (
sid VARCHAR(34) NOT NULL PRIMARY KEY, 
date_created DATETIME NOT NULL, 
date_updated DATETIME NOT NULL, 
friendly_name VARCHAR(64) NOT NULL, 
namespace VARCHAR(30) NOT NULL, 
account_sid VARCHAR(34) NOT NULL, 
api_version VARCHAR(10) NOT NULL,
uri MEDIUMTEXT NOT NULL
);
END IF;

SELECT IFNULL(column_name, '') INTO @colName
FROM information_schema.columns 
WHERE table_name = 'restcomm_accounts'
AND column_name = 'organization_sid';

IF @colName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Modify table "restcomm_accounts", move column cost to the end of the table schema
CREATE TABLE temp_table LIKE restcomm_accounts;
ALTER TABLE temp_table DROP organization_sid;
ALTER TABLE temp_table ADD organization_sid VARCHAR(34);
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
  organization_sid
)
SELECT
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
  organization_sid as organization_sid
FROM restcomm_accounts;
DROP TABLE restcomm_accounts;
ALTER TABLE temp_table RENAME restcomm_accounts;
END IF;