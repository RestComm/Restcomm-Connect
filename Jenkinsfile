node("cxs-ups-testsuites_large") {

   echo sh(returnStdout: true, script: 'env')

   stage ('Checkout') {
    checkout scm
   }

   stage ("Build") {
     
     // Run the maven build with in-module unit testing and sonar
     sh "mvn -f restcomm/pom.xml -pl \\!restcomm.testsuite -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dsonar.host.url=https://sonarqube.com -Dsonar.login=dd43f79a4bd32b1f2c484362e8a4de676a8388c4 -Dsonar.organization=jaimecasero-github -Dsonar.branch=master install sonar:sonar"

     checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/checkstyle-result.xml', unHealthy: ''
     //keep this build for later use
     junit '**/target/surefire-reports/*.xml'
     step( [ $class: 'JacocoPublisher' ] )
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
        if (env.BRANCH_NAME ==~ /^PR-\d+$/) {
            step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailextrecipients([[$class: 'FailingTestSuspectsRecipientProvider']])])
            if (currentBuild.currentResult != 'SUCCESS' ) { // Other values: SUCCESS, UNSTABLE, FAILURE
                setGitHubPullRequestStatus ('CI', 'IT unstable', 'FAILURE')
            } else {
               setGitHubPullRequestStatus ('CI', 'IT passed', 'SUCCESS')
            }
        }
        if (currentBuild.currentResult != 'SUCCESS' ) {
           slackSend "Build unstable - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
        }
    }
}
