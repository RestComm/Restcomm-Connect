#!/bin/bash
##
## Descript+ion: Restcomm performance test script to collect PerfRecorder metrics. From the work of jaime.casero@telestax.com
## Author     : George Vagenas
#

export PR_RESULTS_FOLDER=$RESULTS_FOLDER
if [ ! -d "$PR_RESULTS_FOLDER" ]; then
  mkdir $PR_RESULTS_FOLDER
fi

# Collect results and clean
cp $RESULTS_FOLDER/$TEST_NAME*.csv $TOOLS_DIR/target/data/periodic/sip/sipp.csv
cp $RESULTS_FOLDER/$TEST_NAME*_rtt.csv $TOOLS_DIR/target/data/periodic/sip/sipp_rtt.csv
cd $TOOLS_DIR
rm -f *.zip
./pc_stop_collect.sh -s 360
cp -f $TOOLS_DIR/perf*.zip $PR_RESULTS_FOLDER
### Check for performance regression
./pc_analyse.sh $TOOLS_DIR/perf*.zip 1 > $PR_RESULTS_FOLDER/PerfCorderAnalysis.xml 2> $PR_RESULTS_FOLDER/analysis.log
cat $PR_RESULTS_FOLDER/PerfCorderAnalysis.xml | ./pc_test.sh  $GOALS_FILE > $PR_RESULTS_FOLDER/TEST-PerfCorderAnalysisTest.xml 2> $PR_RESULTS_FOLDER/test.log
cd $CURRENT_FOLDER
