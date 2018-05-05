#!/bin/bash

BASEDIR=/opt/Restcomm-JBoss-AS7

export RUN_DOCKER=true
exec $BASEDIR/bin/restcomm/start-restcomm.sh
