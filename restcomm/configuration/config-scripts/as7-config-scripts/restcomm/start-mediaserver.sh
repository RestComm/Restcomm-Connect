#! /bin/bash

## Description: Starts Media Server with auto-configuration.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

local BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
local MS_CONF=$BASEDIR/mediaserver.conf
local MS_HOME=$RESTCOMM_HOME/mediaserver

$MS_HOME/start-mediaserver.sh $MS_CONF