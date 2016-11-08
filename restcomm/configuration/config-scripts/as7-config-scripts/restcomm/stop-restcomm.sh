#! /bin/bash
##
## Description: Stops RestComm and Media Server processes running on terminal sessions
## Authors    : Henrique Rosa (henrique.rosa@telestax.com)
##

BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_HOME=$(cd $BASEDIR/../../ && pwd)
MS_HOME=$RESTCOMM_HOME/mediaserver

stopMediaServer() {
    source $BASEDIR/stop-mediaserver.sh
}

stopRestComm() {
    echo 'Shutting down RestComm...'
    if tmux ls | grep -q 'restcomm'; then
        tmux kill-session -t restcomm
        echo '...stopped RestComm instance running on terminal session "restcomm"!'
    else
        echo '...restComm already stopped!'
    fi
}

stopMediaServer
stopRestComm
