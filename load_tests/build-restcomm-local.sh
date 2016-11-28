#!/bin/bash

if [ $# -lt 3 ]; then
    echo "No arguments provided. Usage: "
    echo "build-restcomm-local.sh BRANCH_NAME BUILD_WORKSPACE MAJOR_VERSION_NUMBER"
    exit 1
fi

CURRENT_DIR=`pwd`
CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`

RESTCOMM_BRANCH=$1
WORKING_DIR=$2
MAJOR_VERSION_NUMBER=$3

export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
export RUN_TESTSUITE=false
export BUILD_NUMBER=$RESTCOMM_BRANCH-load-test
#export BUILD_WORKSPACE=$WORKING_DIR/workspace
export BUILD_WORKSPACE=$WORKING_DIR

echo "WORKING_DIR: $WORKING_DIR"
echo "BUILD_WORKSPACE: $BUILD_WORKSPACE"
echo "MAJOR VERSION NUMBER: $MAJOR_VERSION_NUMBER"
echo "RESTCOMM BRANCH: $RESTCOMM_BRANCH"
echo "RUN TESTSUITE: $RUN_TESTSUITE"
#export ANT_HOME=/opt/ant/apache-ant-1.8.3

# rm -rf $BUILD_WORKSPACE
# mkdir -p $BUILD_WORKSPACE

# cp -ar ../* $BUILD_WORKSPACE
cd $WORKING_DIR

rm Mobicents-Restcomm*.zip
rm dependencies -rf

export DEPENDENCIES_HOME=$BUILD_WORKSPACE/dependencies
mkdir -p $DEPENDENCIES_HOME
export RESTCOMM_HOME=$BUILD_WORKSPACE/restcomm
export BUILD_RELEASE=$BUILD_WORKSPACE/release

# git checkout -b restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER
# git rev-parse HEAD > git-info-restcomm.txt
echo $MAJOR_VERSION_NUMBER.$BUILD_NUMBER >> $BUILD_RELEASE/mss-version.txt


cd $RESTCOMM_HOME
echo "Workign directory: " `pwd`
mvn versions:set -DnewVersion=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER
#git commit -a -m "New release candidate $MAJOR_VERSION_NUMBER.$BUILD_NUMBER"

cd $BUILD_RELEASE
FILE=$RESTCOMM_HOME/configuration/mss-sip-stack.properties
sed -e "s|MAJOR_VERSION_NUMBER.BUILD_NUMBER|$MAJOR_VERSION_NUMBER.$BUILD_NUMBER|g" $FILE > $FILE.bak
mv $FILE.bak $FILE
ant release -f $BUILD_RELEASE/build.xml -Drestcomm.release.version=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Drestcomm.branch.name=restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Dcheckout.restcomm.dir=$RESTCOMM_HOME -Dworkspace.restcomm.dir=$RESTCOMM_HOME -Dcheckout.dir=$DEPENDENCIES_HOME
mv $BUILD_RELEASE/Restcomm-*.zip $BUILD_WORKSPACE/Restcomm-JBoss-AS7.zip

if [ "$RUN_TESTSUITE" = "true" ]
then
mvn -fn test -Dmaven.test.failure.ignore=true
else
echo "Will not run test suite because variable is $RUN_TESTSUITE"
fi

echo "$MAJOR_VERSION_NUMBER.$BUILD_NUMBER" > $BUILD_WORKSPACE/restcomm-version.txt
ls -la $BUILD_WORKSPACE/*.zip
md5sum $BUILD_WORKSPACE/*.zip
sha1sum $BUILD_WORKSPACE/*.zip
