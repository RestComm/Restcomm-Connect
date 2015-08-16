#!/bin/bash
##
## Description : Utility script to find network properties
## Author      : Henrique Rosa - henrique.rosa@telestax.com
##

# VARIABLES
IP_ADDRESS_PATTERN="[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"
INTERFACE="$1"

## Description: Gets the private IP of the instance 
## Parameters : none
getPrivateIP() {
  echo "$INET_DATA" | grep -o "addr:$IP_ADDRESS_PATTERN" | awk -F: '{print $2}'
}

## Description: Gets the broadcast address of the instance 
## Parameters : none
getBroadcastAddress() {
  echo "$INET_DATA" | grep "Bcast:$IP_ADDRESS_PATTERN" | awk '{print $3}' | awk -F: '{print $2}'
}

## Description: Gets the Subnet Mask of the instance 
## Parameters : none
getSubnetMask() {
  /sbin/ifconfig $INTERFACE | grep "Mask:$IP_ADDRESS_PATTERN" | awk '{print $4}' | awk -F: '{print $2}'
}

## Description: Gets the Network of the instance 
## Parameters : 1.Private IP
## 				2.Subnet Mask
getNetwork() {
        #debian/ubuntu
        NW=`ipcalc -n $1 $2 | grep -i "Network" | awk '{print $2}' | awk -F/ '{print $1}';`
        if [[ -z "$NW" ]]; then
            #rhel/centos/amazon
            NW=`ipcalc -n $1 $2 | grep -i "Network" | awk -F= '{print $2}';`
        fi
        echo $NW
}

# MAIN
INET_DATA=$(/sbin/ifconfig $INTERFACE | grep "inet ")

PRIVATE_IP=$(getPrivateIP)
SUBNET_MASK=$(getSubnetMask)
NETWORK=$(getNetwork $PRIVATE_IP $SUBNET_MASK)
BROADCAST_ADDRESS=$(getBroadcastAddress)
