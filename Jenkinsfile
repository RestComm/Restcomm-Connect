node("cxs-ups-testsuites_large") {
   // Mark the code checkout 'stage'....
   stage 'Checkout'

   // Checkout code from repository
   checkout scm

   // Mark the code build 'stage'....
   stage 'Build'
   // Run the maven build
   sh "mvn -f restcomm/pom.xml  clean install -Pparallel-testing -DforkCount=15 -DskipUTs=false  -Dmaven.test.failure.ignore=true -Dmaven.test.redirectTestOutputToFile=true -Dfailsafe.rerunFailingTestsCount=0"
}
