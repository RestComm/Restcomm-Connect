node("cxs-ups-testsuites") {
   echo sh(returnStdout: true, script: 'env')

   // Mark the code checkout 'stage'....
   stage ('Checkout') {
    checkout scm
   }

   // Mark the code build 'stage'....
   stage ("Build") {
     // Run the maven build with in-module unit testing
     sh "mvn -f restcomm/pom.xml  -T 1.5C clean install -pl \\!restcomm.testsuite -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true"
     //keep this build for later use
     //TODO stash  allowEmpty: true, includes: '$HOME/.m2/repository/org/restcomm/**', name: 'mavenArtifacts'
     junit '**/target/surefire-reports/*.xml'
     //prevent to report this test results two times
     sh "mvn -f restcomm/pom.xml  clean"
   }

    stage("CITestsuite") {
        parallel (
            "SequentialTests" : {
                //this runs in same node as previous, no need to recompile
                sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -DskipUTs=false  -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=0 -DexcludedGroups="org.restcomm.connect.commons.annotations.ParallelClassTests or org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests"'
                junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
            },
            "ParallelTests" : {
                node('cxs-ups-testsuites_large'){
                   //different node, rebuild
                   //TODO unstash 'mavenArtifacts'
                   checkout scm
                   sh 'mvn -f restcomm/pom.xml  -T 1.5C clean install -pl \\!restcomm.testsuite -DskipTests'
                   sh 'mvn -f restcomm/restcomm.testsuite/pom.xml  clean install -Pparallel-testing -DforkCount=16 -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=0 -Dgroups="org.restcomm.connect.commons.annotations.ParallelClassTests" -DexcludedGroups="org.restcomm.connect.commons.annotations.UnstableTests or org.restcomm.connect.commons.annotations.BrokenTests"'
                   junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
                }
            }
        )
    }
}
