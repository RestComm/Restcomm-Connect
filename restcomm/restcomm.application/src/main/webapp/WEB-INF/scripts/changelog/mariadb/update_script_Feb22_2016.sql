#SQL Script for MySQL/MariaDB to update DB with the schema changes for issue #829
#Date: Jan 2
#Author: Guilherme Humberto Jansen

#To run the script use mysql client:
#mysql -u yourusername -p yourpassword yourdatabase < sql_update_script.sql

#USE restcomm;

UPDATE restcomm_incoming_phone_numbers SET voice_url = NULL, voice_application_sid = 'APb70c33bf0b6748f09eaec97030af36f3' WHERE sid = 'PNc2b81d68a221482ea387b6b4e2cbd9d7';
UPDATE restcomm_incoming_phone_numbers SET voice_url = NULL, voice_application_sid = 'AP73926e7113fa4d95981aa96b76eca854' WHERE sid = 'PN46678e5b01d44973bf184f6527bc33f7';
UPDATE restcomm_incoming_phone_numbers SET voice_url = NULL, voice_application_sid = 'AP81cf45088cba4abcac1261385916d582' WHERE sid = 'PNb43ed9e641364277b6432547ff1109e9';
UPDATE restcomm_applications SET rcml_url = '/restcomm-rvd/services/apps/AP73926e7113fa4d95981aa96b76eca854/controller' WHERE sid = 'AP73926e7113fa4d95981aa96b76eca854';
UPDATE restcomm_applications SET rcml_url = '/restcomm-rvd/services/apps/AP81cf45088cba4abcac1261385916d582/controller' WHERE sid = 'AP81cf45088cba4abcac1261385916d582';
UPDATE restcomm_applications SET rcml_url = '/restcomm-rvd/services/apps/APb70c33bf0b6748f09eaec97030af36f3/controller' WHERE sid = 'APb70c33bf0b6748f09eaec97030af36f3';