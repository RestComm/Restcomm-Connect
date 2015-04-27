#! /bin/bash
##
## Description: Starts RestComm with auto-configuration.
##
## Parameters : 1. Bind Address (default: 127.0.0.1)
##              2. Run Mode     [standalone|standalone-lb|domain|domain-lb] (default:standalone)
##
## Author     : Henrique Rosa
##

##
## FUNCTIONS
##
startRestcomm() {
	run_mode="$1"
	bind_address="$2"

	# Check if RestComm is already running
	if screen -list | grep -q 'restcomm'; then
		echo 'TelScale RestComm is already running on screen session "restcomm"!'
		exit 1;
	fi

	case $run_mode in
		'standalone'*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/standalone.sh
			screen -dmS 'restcomm' $RESTCOMM_HOME/bin/standalone.sh -b $bind_address
			echo 'TelScale RestComm started running on standalone mode. Screen session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			;;
		'domain'*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/domain.sh
			screen -dmS 'restcomm' $RESTCOMM_HOME/bin/domain.sh -b $bind_address
			echo 'TelScale RestComm started running on domain mode. Screen session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			;;
		*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/standalone.sh
			screen -dmS 'restcomm' $RESTCOMM_HOME/bin/standalone.sh -b $bind_address
			echo 'TelScale RestComm started running on standalone mode. Screen session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			;;
	esac


	if [[ "$run_mode" == *"-lb" ]]; then
		echo 'Starting SIP Load Balancer...'
		if screen -ls | grep -q 'balancer'; then
			echo 'SIP Load Balancer is already running on screen session "balancer"!'
		else
			screen -dmS 'balancer' java -DlogConfigFile=$LB_HOME/lb-log4j.xml \
			-jar $LB_HOME/sip-balancer-jar-with-dependencies.jar \
			-mobicents-balancer-config=$LB_HOME/lb-configuration.properties
			echo 'SIP Load Balancer started running on screen session "balancer"!'
			echo "Using IP Address: $BIND_ADDRESS"
		fi
	fi
}

startMediaServer() {
	echo "Starting Mobicents Media Server..."
	echo "Media Server will bind to the IP Address: $BIND_ADDRESS"
	if screen -ls | grep -q 'mms'; then
		echo '...Mobicents Media Server is already running on screen session "mms"!'
	else
		chmod +x $MMS_HOME/bin/run.sh
		screen -dmS 'mms'  $MMS_HOME/bin/run.sh
		echo '...Mobicents Media Server started running on screen "mms"!'
fi
}

##
## MAIN
##
# GNU screen needs to be installed
if [ -z "$(command -v screen)" ]; then
	echo "ERROR: GNU Screen is not installed! Install it and try again."
	echo "Centos/RHEL: yum install screen"
	echo "Debian/Ubuntu: apt-get install screen"
	exit 1
fi

# ipcalc needs to be installed
if [ -z "$(command -v ipcalc)" ]; then
	echo "ERROR: ipcalc is not installed! Install it and try again."
	echo "Centos/RHEL: yum install ipcalc"
	echo "Debian/Ubuntu: apt-get install ipcalc"
	exit 1
fi

# set environment variables for execution
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_HOME=$(cd $BASEDIR/../../ && pwd)
MMS_HOME=$RESTCOMM_HOME/mediaserver/server
LB_HOME=$RESTCOMM_HOME/tools/sip-balancer

echo BASEDIR: $BASEDIR
echo RESTCOMM_HOME: $RESTCOMM_HOME
source $BASEDIR/restcomm.conf
source $BASEDIR/proxy.conf

# input parameters and default values
RUN_MODE='standalone'
#NET_INTERFACE=''
#STATIC_ADDRESS=''
BIND_ADDRESS=''

while getopts "s:r:i:" optname
do
	case "$optname" in
		"s")
			STATIC_ADDRESS="$OPTARG"
			;;
		"r")
			RUN_MODE="$OPTARG"
			;;
		"i")
			NET_INTERFACE="$OPTARG"
			;;
		":")
			echo "No argument value for option $OPTARG"
			exit 1
			;;
		"?")
			echo "Unknown option $OPTARG"
			exit 1
			;;
		*)
			echo 'Unknown error while processing options'
			exit 1
			;;
	esac
done

# validate network interface and extract network properties
if [[ -z "$NET_INTERFACE" ]]; then
NET_INTERFACE='eth0'
echo "Looking for the appropriate interface"
	NET_INTERFACES=$(ifconfig | expand | cut -c1-8 | sort | uniq -u | awk -F: '{print $1;}')
	if [[ -z $(echo $NET_INTERFACES | sed -n "/$NET_INTERFACE/p") ]]; then
		echo "The network interface $NET_INTERFACE is not available or does not exist."
		echo "The list of available interfaces is: $NET_INTERFACES"
		exit 1
	fi
fi

# load network properties for chosen interface
if [[ -z "$PRIVATE_IP" || -z "$SUBNET_MASK" || -z "$NETWORK" || -z "$BROADCAST_ADDRESS" ]]; then
echo "Looking for the IP Address, subnet, network and broadcast_address"
	source $BASEDIR/utils/read-network-props.sh "$NET_INTERFACE"
fi
BIND_ADDRESS="$PRIVATE_IP"


if [[ -z "$STATIC_ADDRESS" ]]; then
	STATIC_ADDRESS=$BIND_ADDRESS
fi

if [[ -z "$PUBLIC_IP" ]]; then
	PUBLIC_IP=$STATIC_ADDRESS
fi

# configure restcomm installation
source $BASEDIR/autoconfigure.sh

# start restcomm in selected run mode
startRestcomm "$RUN_MODE" "$BIND_ADDRESS"
startMediaServer
exit 0
