#!/bin/bash

############################################################
# Use this script to unhold a call previously set on hold
############################################################

#Provide Restcomm ip address
restcomm_ip=
#Provide Username sid
userSid=
#Provide Auth Token
authToken=

#Call SID of the call to move. Use the call SID of the initial Call
sid=$1

curl -X POST http://$userSid:$authToken@$restcomm_ip:8080/restcomm/2012-04-24/Accounts/$userSid/Calls.json/$1 -d "Url=http://$restcomm_ip:8080/restcomm/demos/dial/conference/dial-conference-moderator.xml "
