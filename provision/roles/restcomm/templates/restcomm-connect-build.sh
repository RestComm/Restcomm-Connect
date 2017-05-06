#!/bin/bash
export RESTCOMM_HOME=/root/Restcomm-Connect
export MAJOR_VERSION_NUMBER=7.6
export BUILD_NUMBER=0

export WORKSPACE=$RESTCOMM_HOME
mkdir $WORKSPACE/dependencies
export DEPENDENCIES_HOME=$WORKSPACE/dependencies


ant release -f ./release/build.xml -Drestcomm.release.version=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Drestcomm.branch.name=restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Dcheckout.restcomm.dir=$RESTCOMM_HOME -Dworkspace.restcomm.dir=$RESTCOMM_HOME/restcomm -Dcheckout.dir=$DEPENDENCIES_HOME
