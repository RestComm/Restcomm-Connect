#! /bin/bash

## Description: Executes all RestComm configuration scripts for a given version.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

autoconfigure() {
    local BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
    
    ## We want this file to be executed last since its contains xmlstarlet based config script
    ## https://telestax.atlassian.net/browse/RESTCOMM-1140
    local LAST_FILE_TO_BE_EXECUTED=$BASEDIR/autoconfig.d/config-restcomm.sh

    # load configuration values
    #source $BASEDIR/restcomm.conf
    echo ''
    echo 'RestComm automatic configuration started:'
    echo "LAST_FILE_TO_BE_EXECUTED is: $LAST_FILE_TO_BE_EXECUTED"
    for f in $BASEDIR/autoconfig.d/*.sh; do
        echo "Executing configuration file $f..."
        if [ "$f" != "$LAST_FILE_TO_BE_EXECUTED" ]; then
                source $f
                echo "Finished executing configuration file $f!"
                echo ''
        fi
    done

    source $LAST_FILE_TO_BE_EXECUTED
    echo "Finished executing configuration file $LAST_FILE_TO_BE_EXECUTED!"
    echo ''

    echo 'RestComm automatic configuration finished!'
    echo ''
}

autoconfigure