#!/bin/bash
##
## Description: Configures RestComm Nedia Server
## Author: Vladimir Morosev (vladimir.morosev@telestax.com)
##

BASEDIR=$RESTCOMM_HOME

# Copy Media Server configuration overrides to configuration folder
copyConfiguration(){
    cp -f $BASEDIR/bin/restcomm/media-extra.yml $BASEDIR/mediaserver/conf
}

# MAIN
echo 'Configuring Media Server...'
copyConfiguration
