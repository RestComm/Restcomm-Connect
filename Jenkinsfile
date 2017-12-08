node("cxs-ups-testsuites_large") {
    def isPRMergeBuild() {
        return (env.BRANCH_NAME ==~ /^PR-\d+$/)
    }

   echo sh(returnStdout: true, script: 'env')

   stage ('Checkout') {
    checkout scm
   }

   stage ("Build") {
     // Run the maven build with in-module unit testing
     sh "mvn -f restcomm/pom.xml  -T 1.5C clean install -pl \\!restcomm.testsuite -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true"
     //keep this build for later use
     junit '**/target/surefire-reports/*.xml'
     step( [ $class: 'JacocoPublisher' ] )
     setGitHubPullRequestStatus ("${context}", 'Build completed', 'SUCCESS')
     //prevent to report this test results two times
     sh "mvn -f restcomm/pom.xml  clean"
   }

    stage("CITestsuiteSeq") {
        sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -DskipUTs=false  -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=1 -DexcludedGroups="org.restcomm.connect.commons.annotations.ParallelClassTests or org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests or org.restcomm.connect.commons.annotations.FeatureAltTests or org.restcomm.connect.commons.annotations.FeatureExpTests"'
        junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
        //prevent to report this test results two times
        sh "mvn -f restcomm/pom.xml  clean"
    }

    stage("CITestsuiteParallel") {
        sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -Pparallel-testing -DforkCount=16 -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=1 -Dgroups="org.restcomm.connect.commons.annotations.ParallelClassTests" -DexcludedGroups="org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests or org.restcomm.connect.commons.annotations.FeatureAltTests or org.restcomm.connect.commons.annotations.FeatureExpTests"'
        junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
        //prevent to report this test results two times
        sh "mvn -f restcomm/pom.xml  clean"
        if (isPRMergeBuild()) {
            step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'FailingTestSuspectsRecipientProvider']])])
            if (currentBuild.currentResult != 'SUCCESS' ) { // Other values: SUCCESS, UNSTABLE, FAILURE
                setGitHubPullRequestStatus ("${context}", 'Testsuite unstable', 'FAILURE')
            }            
        }
    }
}
