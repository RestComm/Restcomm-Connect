#!/bin/bash

log() {
    logger -s -t $(basename $0) $1
}


read -p "Are you sure you want to proceed, all stopped containers will be deleted? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]; then
        # If yes & enter - proceed to delete stopped containers and unused images.
    log "Started"

    containers=$(docker ps -a -q -f "status=exited")
    if [ -n "${containers}" ]; then
        log "Cleanup stopped container id(s): ${containers}"
        docker rm $containers
    fi

    images=$(docker images -q --filter "dangling=true")
    if [ -n "${images}" ]; then
        log "Cleanup dangling image id(s): ${images}"
        docker rmi $images
    fi

    log "Done"
fi