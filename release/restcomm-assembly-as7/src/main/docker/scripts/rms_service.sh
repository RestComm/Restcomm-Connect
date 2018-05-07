#! /bin/bash

export RUN_DOCKER=true

BASEDIR=/opt/Restcomm-JBoss-AS7
RMS_CONF=$BASEDIR/bin/restcomm
ms_conf=$RMS_CONF/mediaserver.conf
ms_home=$BASEDIR/mediaserver

exec  $ms_home/start-mediaserver.sh $ms_conf