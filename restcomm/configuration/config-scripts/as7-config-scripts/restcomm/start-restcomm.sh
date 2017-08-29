#! /bin/bash
##
## Description: Starts RestComm with auto-configuration.
##
## Parameters : 1. Bind Address (default: 127.0.0.1)
##              2. Run Mode     [standalone|standalone-lb|domain|domain-lb] (default:standalone)
##
## Author     : Henrique Rosa
##
# set environment variables for execution
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_HOME=$(cd $BASEDIR/../../ && pwd)
MMS_HOME=$RESTCOMM_HOME/mediaserver
LB_HOME=$RESTCOMM_HOME/tools/sip-balancer

##
## FUNCTIONS
##
startRestcomm() {
	run_mode="$1"
	bind_address="$2"
	ExtraOpts="-Djboss.bind.address.management=127.0.0.1"

	# Check if RestComm is already running
	if tmux ls | grep -q 'restcomm'; then
		echo 'TelScale RestComm is already running on terminal session "restcomm"!'
		exit 1;
	fi

    if [ -n "$MGMT_PASS" ] && [ -n "$MGMT_USER" ]; then
	    echo "MGMT_PASS, MGMT_USER is set will be added to MGMNT configuration"
        grep -q "$MGMT_USER" $RESTCOMM_HOME/standalone/configuration/mgmt-users.properties || $RESTCOMM_HOME/bin/add-user.sh "$MGMT_USER" "$MGMT_PASS" -s
        #Management bind address
            ExtraOpts="-Djboss.bind.address.management=$bind_address"
    fi

	case $run_mode in
		'standalone'*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/standalone.sh
			echo 'TelScale RestComm started running on standalone mode. Terminal session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			if [[ "$RUN_DOCKER" == "true" || "$RUN_DOCKER" == "TRUE" ]]; then
				$RESTCOMM_HOME/bin/standalone.sh -b $bind_address "${ExtraOpts}"
			else
				tmux new -s restcomm -d "$RESTCOMM_HOME/bin/standalone.sh -b $bind_address ${ExtraOpts}"
			fi
			;;
		'domain'*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/domain.sh
			tmux new -s restcomm -d "$RESTCOMM_HOME/bin/domain.sh -b $bind_address ${ExtraOpts}"
			echo 'TelScale RestComm started running on domain mode. Screen session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			;;
		*)
			# start restcomm on standalone mode
			chmod +x $RESTCOMM_HOME/bin/standalone.sh
			tmux new -s restcomm -d "$RESTCOMM_HOME/bin/standalone.sh -b $bind_address ${ExtraOpts}"
			echo 'TelScale RestComm started running on standalone mode. Screen session: restcomm.'
			echo "Using IP Address: $BIND_ADDRESS"
			;;
	esac

}

verifyDependencies() {
    source $BASEDIR/verify-dependencies.sh
}

loadConfigurationParams() {
    source $BASEDIR/restcomm.conf
    source $BASEDIR/advanced.conf
}

##
## MAIN
##
verifyDependencies
loadConfigurationParams

echo BASEDIR: $BASEDIR
echo RESTCOMM_HOME: $RESTCOMM_HOME

# input parameters and default values
RUN_MODE='standalone'
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
BIND_NETWORK="$NETWORK"
BIND_SUBNET_MASK="$SUBNET_MASK"

if [[ -z "$STATIC_ADDRESS" ]]; then
	STATIC_ADDRESS=$BIND_ADDRESS
fi

if [[ -z "$MEDIASERVER_EXTERNAL_ADDRESS" ]]; then
   MEDIASERVER_EXTERNAL_ADDRESS="$STATIC_ADDRESS"
fi

if [[ -z "$PUBLIC_IP" ]]; then
	PUBLIC_IP=$STATIC_ADDRESS
fi

if [[ -z "$SMS_OUTBOUND_PROXY" ]]; then
	SMS_OUTBOUND_PROXY=$OUTBOUND_PROXY
fi

# configure restcomm installation

if [[ "$MANUAL_SETUP" == "false" || "$MANUAL_SETUP" == "FALSE" ]]; then
    source $BASEDIR/autoconfigure.sh
fi

if [[ "$MS_EXTERNAL" == "false" || "$MS_EXTERNAL" == "FALSE" ]]; then
    source $BASEDIR/start-mediaserver.sh
fi
# start restcomm in selected run mode
startRestcomm "$RUN_MODE" "$BIND_ADDRESS"
exit 0
