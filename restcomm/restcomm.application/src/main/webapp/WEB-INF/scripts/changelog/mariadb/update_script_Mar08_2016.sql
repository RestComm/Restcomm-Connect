#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue GeoLocation API draft
#Date: Jan 20
#Author: Fernando Mendioroz

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

USE restcomm;

SELECT IFNULL(table_name, '') INTO @tableName
FROM information_schema.columns 
WHERE table_name = 'restcomm_geolocation'

IF @tableName = '' THEN 
    -- ALTER COMMAND GOES HERE --
#Drop and create again the "restcomm_applications" table
CREATE TABLE restcomm_geolocation(
sid VARCHAR(34) NOT NULL PRIMARY KEY,
date_created DATETIME NOT NULL,
date_updated DATETIME NOT NULL,
date_executed DATETIME NOT NULL,
account_sid VARCHAR(34) NOT NULL,
source VARCHAR(30),
device_identifier VARCHAR(30) NOT NULL,
geolocation_type VARCHAR(15) NOT NULL,
response_status VARCHAR(30),
cell_id VARCHAR(10),
location_area_code VARCHAR(10),
mobile_country_code INTEGER, 
mobile_network_code VARCHAR(3), 
network_entity_address BIGINT,
age_of_location_info INTEGER,
device_latitude VARCHAR(15), 
device_longitude VARCHAR(15), 
accuracy BIGINT, 
physical_address VARCHAR(50), 
internet_address VARCHAR(50), 
formatted_address VARCHAR(200),
location_timestamp DATETIME,
event_geofence_latitude VARCHAR(15), 
event_geofence_longitude VARCHAR(15), 
radius BIGINT, 
geolocation_positioning_type VARCHAR(15), 
last_geolocation_response VARCHAR(10), 
cause VARCHAR(150), 
api_version VARCHAR(10) NOT NULL, 
uri LONGVARCHAR NOT NULL);
END IF;