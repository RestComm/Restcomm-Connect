#!/bin/bash
export RESTCOMM_HOME=/Users/pach/work/repo/RestComm
export MAJOR_VERSION_NUMBER=7.3
export BUILD_NUMBER=1

export WORKSPACE=$RESTCOMM_HOME
mkdir $WORKSPACE/dependencies
export DEPENDENCIES_HOME=$WORKSPACE/dependencies



ant release -f ./release/build.xml -Dmobicents.restcomm.release.version=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Dmobicents.restcomm.branch.name=restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Dcheckout.mobicents-restcomm.dir=$RESTCOMM_HOME -Dworkspace.mobicents-restcomm.dir=$RESTCOMM_HOME/restcomm -Dcheckout.dir=$DEPENDENCIES_HOME
