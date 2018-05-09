#! /bin/bash
## Description: Starts Media Server with auto-configuration.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)
## Parameters : 1. path to conf file (optional)

export MS_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMME_DIR=/opt/Restcomm-JBoss-AS7
source /etc/container_environment.sh

getPID(){
   while read -r line
   do
    if  ps -ef | grep $line | grep -q  mediaserver
    then
          RMS_PID=$line
   fi
   done < <(jps | grep Main | cut -d " " -f 1)
}

configNetwork()  {
# Configure RMS network using the read-network-props.sh from RC
if [  "${MS_EXTERNAL^^}" = "FALSE"  ]; then
    echo "Looking for the IP Address, subnet, network and broadcast_address for RMS"
	source $RESTCOMME_DIR/bin/restcomm/utils/read-network-props.sh "eth0"

    if [ ! -f /etc/container_environment/BIND_ADDRESS ]; then
         sed -i "s|BIND_ADDRESS=.*|BIND_ADDRESS=${PRIVATE_IP}|" $RESTCOMME_DIR/bin/restcomm/mediaserver.conf
    fi

    if [ ! -f /etc/container_environment/NETWORK ]; then
        sed -i "s|NETWORK=.*|NETWORK=${NETWORK}|" $RESTCOMME_DIR/bin/restcomm/mediaserver.conf
    fi

    if [ ! -f /etc/container_environment/SUBNET ]; then
         sed -i "s|SUBNET=.*|SUBNET=${SUBNET_MASK}|" $RESTCOMME_DIR/bin/restcomm/mediaserver.conf
    fi

     if [ ! -f /etc/container_environment/RMSCONF_MGCP_ADDRESS ]; then
        sed -i "s|MGCP_ADDRESS=.*|MGCP_ADDRESS=${PRIVATE_IP}|" $RESTCOMME_DIR/bin/restcomm/mediaserver.conf
    fi

fi

}

verifyDependencies() {
    source $MS_HOME/.autoconfig/verify-dependencies.sh
}

loadConfigurationParams() {
    local override_conf=$1

    # load default configuration files
    source $MS_HOME/mediaserver.conf
    source $MS_HOME/logger.conf
    source $MS_HOME/ssl.conf

    # load file to override configuration (if any)
    if [ -n "$override_conf" ]; then
        source $override_conf
    fi
}

configureMediaServer() {
    # Configure media server
    source $MS_HOME/.autoconfig/autoconfigure.sh
    # Set permissions of run script because it may have been overwritten by commands like sed
    chmod 755 $MS_HOME/bin/run.sh
}

startMediaServer() {
 echo 'Starting RestComm Media Server...'
    # Check if RestComm is already running
	if [[ "$RUN_DOCKER" == "true" || "$RUN_DOCKER" == "TRUE" ]]; then
        if [ -n "$RMS_PID" ]; then
            echo '... already running Aborted.'
		    exit 1;
	    else
            $MS_HOME/bin/run.sh
            echo '...RestComm Media Server started running!'
	    fi
    else
	     if tmux ls | grep -q 'mediaserver'; then
            echo '... already running a session named "mediaserver"! Aborted.'
            exit 1
        else
            tmux new -s mediaserver -d $MS_HOME/bin/run.sh
            echo '...RestComm Media Server started running on session named "mediaserver"!'
        fi
	fi
}

getPID
#verifyDependencies
configNetwork
loadConfigurationParams $1
configureMediaServer
startMediaServer
