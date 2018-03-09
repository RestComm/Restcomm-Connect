#! /bin/bash

## Description: Stops Media Server running in a terminal session.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

stopMediaServer() {
    local ms_home=$RESTCOMM_HOME/mediaserver
    $ms_home/stop-mediaserver.sh
}

stopMediaServer
