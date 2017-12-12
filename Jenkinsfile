
def runTestsuite(exludedGroups = "org.restcomm.connect.commons.annotations.BrokenTests",groups = "", forkCount=1, profile="") {
    sh "mvn -f restcomm/restcomm.testsuite/pom.xml  install -DskipUTs=false  -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=1 -Dgroups=\"$groups\" -DexcludedGroups=\"$exludedGroups\""
}


def buildRC() {
     // Run the maven build with in-module unit testing and sonar
    try {
        if (env.BRANCH_NAME == 'master') {
            //do sonar just in master
            sh "mvn -f restcomm/pom.xml -pl \\!restcomm.testsuite -Dmaven.test.redirectTestOutputToFile=true -Dsonar.host.url=https://sonarqube.com -Dsonar.login=dd43f79a4bd32b1f2c484362e8a4de676a8388c4 -Dsonar.organization=jaimecasero-github -Dsonar.branch=master install sonar:sonar"
        } else {
            sh "mvn -f restcomm/pom.xml -pl \\!restcomm.testsuite -Dmaven.test.redirectTestOutputToFile=true install"
        }
    } catch(err) {
        publishRCResults()
        throw err
    }
}

def publishRCResults() {
    junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
    checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/checkstyle-result.xml', unHealthy: ''
    junit '**/target/surefire-reports/*.xml'
    step( [ $class: 'JacocoPublisher' ] )
    if (currentBuild.currentResult != 'SUCCESS' ) {
       slackSend "Build unstable - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
    }
    if (env.BRANCH_NAME ==~ /^PR-\d+$/) {
        //step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'FailingTestSuspectsRecipientProvider']])])
        /*if (currentBuild.currentResult != 'SUCCESS' ) { // Other values: SUCCESS, UNSTABLE, FAILURE
            setGitHubPullRequestStatus (context:'CI', message:'IT unstable', state:'FAILURE')
        } else {
           setGitHubPullRequestStatus (context:'CI', message:'IT passed', state:'SUCCESS')
        }*/
    }

}

node("cxs-ups-testsuites_large") {

   echo sh(returnStdout: true, script: 'env')

   stage ('Checkout') {
    checkout scm
   }

   stage ("Build") {
    buildRC()
   }

    stage("CITestsuiteSeq") {
        if (env.BRANCH_NAME == 'master') {
            runTestsuite("org.restcomm.connect.commons.annotations.ParallelClassTests or org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests")
        } else {
            //exclude alt and exp to make it lighter
            runTestsuite("org.restcomm.connect.commons.annotations.ParallelClassTests or org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests or org.restcomm.connect.commons.annotations.FeatureAltTests or org.restcomm.connect.commons.annotations.FeatureExpTests")
        }
    }


    stage("CITestsuiteParallel") {
        if (env.BRANCH_NAME == 'master') {
            runTestuite("org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests", "org.restcomm.connect.commons.annotations.ParallelClassTests", "16" , "parallel-testing")
        } else {
            //exclude alt and exp to make it lighter
            runTestuite("org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests or org.restcomm.connect.commons.annotations.FeatureAltTests or org.restcomm.connect.commons.annotations.FeatureExpTests", "org.restcomm.connect.commons.annotations.ParallelClassTests", "16" , "parallel-testing")
        }
    }


    stage("PublishResults") {
        publishRCResults()
    }
}
