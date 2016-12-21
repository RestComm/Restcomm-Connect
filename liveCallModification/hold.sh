#!/bin/bash

############################################################
# Alice calls Bob. Given Alice call_sid, use this script to
# move both calls to a conference room with music and muted
# that means to hold the call.
############################################################

#Provide Restcomm ip address
restcomm_ip=
#Provide Username sid
userSid=
#Provide Auth Token
authToken=

#Call SID of the call to move. Use the call SID of the initial Call
sid=$1

echo "Moving call $1 to Conference"

curl -X POST http://$userSid:$authToken@$restcomm_ip:8080/restcomm/2012-04-24/Accounts/$userSid/Calls.json/$1 -d "Url=http://$restcomm_ip:8080//restcomm/demos/dial/conference/dial-conference.xml" -d "MoveConnectedCallLeg=true"
