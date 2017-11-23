node("cxs-ups-testsuites_large") {
   echo sh(returnStdout: true, script: 'env')

   stage ('Checkout') {
    checkout scm
   }

   stage ("Build") {
     // Run the maven build with in-module unit testing
     sh "mvn -f restcomm/pom.xml  -T 1.5C clean install -pl \\!restcomm.testsuite -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true"
     //keep this build for later use
     junit '**/target/surefire-reports/*.xml'
     //prevent to report this test results two times
     sh "mvn -f restcomm/pom.xml  clean"
   }

    stage("CITestsuiteSeq") {
        sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -DskipUTs=false  -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=0 -DexcludedGroups="org.restcomm.connect.commons.annotations.ParallelClassTests or org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests"'
        junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
        //prevent to report this test results two times
        sh "mvn -f restcomm/pom.xml  clean"
    }

    stage("CITestsuiteParallel") {
        sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -Pparallel-testing -DforkCount=16 -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=0 -Dgroups="org.restcomm.connect.commons.annotations.ParallelClassTests" -DexcludedGroups="org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests"'
        junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
        //prevent to report this test results two times
        sh "mvn -f restcomm/pom.xml  clean"
    }
}
