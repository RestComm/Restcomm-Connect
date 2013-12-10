## README - RestComm Configuration Scripts
## author - Henrique Rosa

This collection of scripts can be used to quickly configure your RestComm instance with
ease.

As a requirement, you simply need to declare an environment variable named RESTCOMM_HOME 
containing the install path of your RestComm instance.

Attention, these scripts were tested on CentOS 6.4 and should be used to configure JBoss 
AS 7 on standalone mode. Feel free to change these scripts in case you want to run 
RestComm on a different environment.

---------------
Utils Directory
---------------

This directory contains utility scripts that may be dependencies of the configuration 
scripts.

## read-network-props.sh

This script reads network properties on a specific interface.
The network interface is defined by the variable INTERFACE which assume default value 
"eth0". You should edit this variable according to your environment.

When you source this script, you will have available the following variables:
 - PRIVATE_IP
 - PUBLIC_IP
 - SUBNET_MASK
 - NETWORK
 - BROADCAST_ADDRESS
 
NOTE: This script is usually a dependency of all other scripts! Make sure it runs well
agains your system, before invoking other scripts!

## collect-data.sh

This script collects logs and relevant data from your system.
It shall be used when an error occurs in production environment or when you want to
monitor your RestComm instance in a test environment.

The following data will be gathered:
 - RestComm Logs
 - Media Server Logs
 - Process Status
 - Network Statistics
 - Memory Usage
 - System Messages
 - jStack for RestComm and Media Server
 - jMap for RestComm and Media Server (optional - run script with "-m flag")

----------------
Config Directory
----------------

This directory contains scripts whose function is to update RestComm configuration.
Usually these scripts depend on other scripts contained on Utils directory.

## create-mariadb-datasource

This script downloads and install MariaDB driver an a JBoss Module.
It also creates a MariaDB Datasource on standalone-sip.xml configuration file, named
MariaDS. The datasource is disabled by default.

You can enable and configure the MariaDB datasource using the config-mariadb.sh 
configuration script.

## config-mariadb

This script enables and configures the MariaDB datasource created by 
create-mariadb-datasource.sh script. It will disable all remaining datasources.

Notice that you should modify this script in order to define the username and password
used on your database connection. The default values are user-name=sa, password=sa.

You should only use this script if you intend to use MariaDB and your database engine.

## config-media-server

This script configures the TelScale Media Server instance that ships with RestComm binary.
Depends on read-network-utils script.

## config-sip-connectors

This scripts configures the SIP connectors defined on standalone-sip.xml configuration
file.
Depends on read-network-utils script.

## config-restcomm

This script configures RestComm as well as the interfaces with 3rd party plugins.
Depends on read-network-utils script.

Before you execute this script you should edit the following variables:
 - OUTBOUND_IP: ip address of the outbound proxy
 - VI_LOGIN: username for Voip Innovations login
 - VI_PASSWORD: password for Voip Innovations login
 - VI_ENDPOINT: endpoint for Voip Innovations
 - INTERFAX_USER: username for Interfax
 - INTERFAX_PASSWORD: password for Interfax
 - ISPEECH_KEY: key for iSpeech license
 - ACAPELA_APPLICATION: application id for Acapela account
 - ACAPELA_LOGIN: username of Acapela account
 - ACAPELA_PASSWORD: password of the Acapela account
 - VOICERSS_KEY: key for VoiceRSS license


