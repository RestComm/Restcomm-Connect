#!/bin/bash
##
## Description : Utility script to find network properties
## Author      : Henrique Rosa - henrique.rosa@telestax.com
##

# VARIABLES
IP_ADDRESS_PATTERN="[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"
INTERFACE="eth0"

## Description: Gets the private IP of the instance 
## Parameters : none
getPrivateIP() {
  /sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $2}' | awk '{print $1}';
}

## Description: Gets the public IP of the instance 
## Parameters : none
getPublicIP() {
        PIP_IP=`wget --tries=5 --timeout=5 -qO- http://ipecho.net/plain`
        if [[ -z "$PIP_IP" ]]; then
            PIP=`wget --tries=5 --timeout=5 -qO- http://observebox.com/ip`
        fi
        if [[ -z "$PIP" ]]; then
            echo "Failed to determine public IP"
            exit 1;
        else
            echo $PIP
        fi
}

## Description: Gets the broadcast address of the instance 
## Parameters : none
getBroadcastAddress() {
	/sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $3}' | awk '{print $1}';
}

## Description: Gets the Subnet Mask of the instance 
## Parameters : none
getSubnetMask() {
	/sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $4}' | awk '{print $1}';
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
PRIVATE_IP=`getPrivateIP`
PUBLIC_IP=`getPublicIP`
SUBNET_MASK=`getSubnetMask`
NETWORK=`getNetwork $PRIVATE_IP $SUBNET_MASK`
BROADCAST_ADDRESS=`getBroadcastAddress`
