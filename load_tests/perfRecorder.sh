#!/bin/bash
##
## Description: Restcomm performance test script to collect PerfRecorder metrics. From the work of jaime.casero@telestax.com
## Author     : George Vagenas
#

export PR_RESULTS_FOLDER=$RESULTS_DIR
if [ ! -d "$PR_RESULTS_FOLDER" ]; then
  mkdir $PR_RESULTS_FOLDER
fi

# Collect results and clean
echo "About to run PerfRecorder script"
echo "RESULTS_DIR: $RESULTS_DIR"
echo "TEST_NAME $TEST_NAME"
echo "TOOLS_DIR $TOOLS_DIR"
echo "GOALS_FILE $GOALS_FILE"

cp $RESULTS_DIR/$TEST_NAME*.csv $TOOLS_DIR/target/data/periodic/sip/sipp.csv
# NOTE: PerfCorder no longer uses RTT file
#cp $RESULTS_DIR/$TEST_NAME*_rtt.csv $TOOLS_DIR/target/data/periodic/sip/sipp_rtt.csv
cd $TOOLS_DIR
rm -f *.zip
./pc_stop_collect.sh -s 360
cp -f $TOOLS_DIR/perf*.zip $PR_RESULTS_FOLDER
### Check for performance regression
./pc_analyse.sh $TOOLS_DIR/perf*.zip 10 > $PR_RESULTS_FOLDER/PerfCorderAnalysis.xml 2> $PR_RESULTS_FOLDER/analysis.log
cat $PR_RESULTS_FOLDER/PerfCorderAnalysis.xml | ./pc_test.sh  $GOALS_FILE > $PR_RESULTS_FOLDER/TEST-PerfCorderAnalysisTest.xml 2> $PR_RESULTS_FOLDER/test.log
### Create HTML graphs
echo "Creating PerfCorder HTML ... "
cat $RESULTS_DIR/PerfCorderAnalysis.xml | $TOOLS_DIR/pc_html_gen.sh > $PR_RESULTS_FOLDER/PerfCorderAnalysis.html 2> $RESULTS_DIR/htmlgen.log
cd $CURRENT_FOLDER
