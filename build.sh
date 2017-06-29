#!/bin/bash
export MAVEN_OPTS="-Xms1024m -Xmx2048m -XX:MaxPermSize=1024m"
export ANT_HOME=/opt/ant/apache-ant-1.8.3

rm Restcomm*.zip -rf
rm dependencies -rf

echo "MAJOR VERSION NUMBER: $MAJOR_VERSION_NUMBER"
echo "RESTCOMM BRANCH: $RESTCOMM_BRANCH"
echo "RUN TESTSUITE: $RUN_TESTSUITE"

export WORKSPACE=$TRAVIS_BUILD_DIR
export BUILD_NUMBER=$TRAVIS_BUILD_NUMBER

export DEPENDENCIES_HOME=$WORKSPACE/dependencies
mkdir $DEPENDENCIES_HOME
export RESTCOMM_HOME=$WORKSPACE
export RELEASE=$RESTCOMM_HOME/release
cd $RESTCOMM_HOME/restcomm
git checkout -b restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER
git rev-parse HEAD > git-info-restcomm.txt
echo $MAJOR_VERSION_NUMBER.$BUILD_NUMBER >> mss-version.txt

mvn versions:set -DnewVersion=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -P docs
git commit -a -m "New release candidate $MAJOR_VERSION_NUMBER.$BUILD_NUMBER"

cd $RELEASE
FILE=$RESTCOMM_HOME/restcomm/configuration/mss-sip-stack.properties
sed -e "s|MAJOR_VERSION_NUMBER.BUILD_NUMBER|$MAJOR_VERSION_NUMBER.$BUILD_NUMBER|g" $FILE > $FILE.bak
mv $FILE.bak $FILE
ant release -f $RESTCOMM_HOME/release/build.xml -Drestcomm.release.version=$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Drestcomm.branch.name=restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER -Dcheckout.restcomm.dir=$RESTCOMM_HOME -Dworkspace.restcomm.dir=$RESTCOMM_HOME/restcomm -Dcheckout.dir=$DEPENDENCIES_HOME
mv $RELEASE/Restcomm-*.zip $WORKSPACE
ls -la $WORKSPACE/Restcomm-JBoss-AS7-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER.zip

echo "About to upload to Bintray"
curl -T $WORKSPACE/Restcomm-JBoss-AS7-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER.zip -ugvagenas:$BINTRAY_API_KEY -H "X-Bintray-Package:binaries" -H "X-Bintray-Version:8.2.0" https://api.bintray.com/content/gvagenas/Restcomm-Connect/bin/


cd $RESTCOMM_HOME/restcomm
#commenting the deploy command as it eats up storage on artifactory
#mvn deploy -Dmaven.test.skip=true
#mvn -Dgpg.passphrase="$GPG_PASSPHRASE" -f pom.xml clean install deploy -Pattach-sources,generate-javadoc,release-sign-artifacts,cloudbees-oss-release -s /private/mobicents/settings.xml -Dmaven.test.skip=true

#if [ "$RUN_TESTSUITE" = "true" ]
#then
#mvn -fn surefire-report:report -Dmaven.test.failure.ignore=true
#else
#echo "Will not run test suite because variable is $RUN_TESTSUITE"
#fi

#git push --repo https://telscalejenkins:m0b1c3nts@bitbucket.org/telestax/telscale-restcomm.git origin restcomm-release-$MAJOR_VERSION_NUMBER.$BUILD_NUMBER
echo "$MAJOR_VERSION_NUMBER.$BUILD_NUMBER" > $WORKSPACE/restcomm-version.txt
ls -la $WORKSPACE/*.zip
md5sum $WORKSPACE/*.zip
sha1sum $WORKSPACE/*.zip

git commit -a -m 'New release candidate'
git tag $MAJOR_VERSION_NUMBER.$BUILD_NUMBER
git push origin $MAJOR_VERSION_NUMBER.$BUILD_NUMBER

cd $RESTCOMM_HOME/restcomm
mvn -Dlicense.includedScopes=compile license:aggregate-add-third-party

# sonarqube integration
#cd $RESTCOMM_HOME/restcomm
#mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar -Dmaven.test.failure.ignore=true -Dsonar.host.url=https://nemo.sonarqube.org -Dsonar.login=Restcomm
