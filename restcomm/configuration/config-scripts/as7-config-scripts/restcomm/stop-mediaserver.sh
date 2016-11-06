#! /bin/bash

## Description: Stops Media Server running in a terminal session.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

stopMediaServer() {
    local MS_HOME=$RESTCOMM_HOME/mediaserver
    $MS_HOME/stop-mediaserver.sh
}

stopMediaServer
