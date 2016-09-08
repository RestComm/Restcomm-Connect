#!/bin/bash
##
## Descript+ion: Script that collects all necessary system logs and data.
## Author     : Lefteris Banos
#
DATE=$(date +%F_%H_%M)
DIR_NAME=restcomm_$DATE
RESTCOMM_CORE_FILE=server.log
MEDIASERVER_FILE=server.log
SYSLOGS_DIR=/var/log
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
RESTCOMM_LOG_BASE=$(cd $BASEDIR/../../ && pwd)
RESTCOMM_CORE_LOG=$RESTCOMM_LOG_BASE/standalone/log
MMS_LOGS=$RESTCOMM_LOG_BASE/mediaserver/log
LOGS_DIR_ZIP=$BASEDIR/$DIR_NAME

restcomm_logs () {
    if [ -f $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE ]; then
        cp $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE $LOGS_DIR_ZIP/restcomm_server.log
    fi
}
restcomm_logs_bytime () {
    if [ -f $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE ]; then
        IN=$1
        IFS=","
        arr=($IN)
        unset IFS
        FROM=`grep -n "^${arr[0]}"  $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE |cut -f1 -d: | tail -1`
        TO=`grep -n "^${arr[1]}"  $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE |cut -f1 -d: | tail -1`
        awk 'NR=="'"$FROM"'", NR=="'"$TO"'"; NR=="'"$TO"'" {print; exit}' $RESTCOMM_CORE_LOG/$RESTCOMM_CORE_FILE > $LOGS_DIR_ZIP/RestCommlinesTime.log
    fi
}
mediaserver_logs () {
    if [ -f $MMS_LOGS/$MEDIASERVER_FILE ]; then
        cp $MMS_LOGS/$MEDIASERVER_FILE $LOGS_DIR_ZIP/mms_server.log
    fi
}
system_logs () {
    if [ -f $SYSLOGS_DIR/messages ]; then
        cp $SYSLOGS_DIR/messages $LOGS_DIR_ZIP/
    fi
    if [ -f $SYSLOGS_DIR/syslog ]; then
        cp $SYSLOGS_DIR/syslog $LOGS_DIR_ZIP/
    fi
}
JVM_perfo_stats () {
    if [ -n "$RESTCOMM_PID" ]; then
        jstack -l  $RESTCOMM_PID >  $LOGS_DIR_ZIP/restcomm_jstack_trace_$DATE
    fi
    if [ -n "$RMS_PID" ]; then
        jstack -l $RMS_PID >  $LOGS_DIR_ZIP/mms_jstack_trace_$DATE
    fi
}
system_usage_info () {
    echo "---top:" > $LOGS_DIR_ZIP/usage_stats_$DATE
    echo CPU\(s\): `top -b -n1 | grep "Cpu(s)" | awk '{print $2" : " $4}'` >>  $LOGS_DIR_ZIP/usage_stats_$DATE
    echo  >> $LOGS_DIR_ZIP/usage_stats_$DATE
    top -b -n1 | grep Mem >> $LOGS_DIR_ZIP/usage_stats_$DATE
    echo "---free:" >> $LOGS_DIR_ZIP/usage_stats_$DATE
    free >> $LOGS_DIR_ZIP/usage_stats_$DATE
    echo "---df:" >> $LOGS_DIR_ZIP/usage_stats_$DATE
    df -h >> $LOGS_DIR_ZIP/usage_stats_$DATE
    ps aux  > $LOGS_DIR_ZIP/top_$DATE
}
jvm_process_info () {
    if [ -n "$RESTCOMM_PID" ]; then
         echo "----------------------- restcomm ---------------------------" > $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "--------New Generation Heap-------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat -gcnew $RESTCOMM_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "--------New Generation Space Size-------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat -gcnewcapacity $RESTCOMM_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "--------Garbage-collected heap-------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat  -gc $RESTCOMM_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
    fi
    if [ -n "$RMS_PID" ]; then
         echo "----------------------- mediaserver ---------------------------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "-------- New Generation Heap -------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat -gcnew $RMS_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "--------New Generation Space Size-------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat -gcnewcapacity $RMS_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
         echo "--------Garbage-collected heap-------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
         jstat -gc $RMS_PID >> $LOGS_DIR_ZIP/jvm_process_$DATE
    fi
    echo "--------------------------------- ---------------------------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
    echo >> $LOGS_DIR_ZIP/jvm_process_$DATE
    echo >> $LOGS_DIR_ZIP/jvm_process_$DATE
    echo "----------------------- More INFO ---------------------------" >> $LOGS_DIR_ZIP/jvm_process_$DATE
    echo "  http://docs.oracle.com/javase/7/docs/technotes/tools/share/jstat.html " >> $LOGS_DIR_ZIP/jvm_process_$DATE
}
LWP_threads_logs () {
    pat=`ps -ef | grep java | grep -v grep | awk '{print $2}'`
    ps -eLo pid,lwp,nlwp,ruser,pcpu,stime,etime,args | grep -F "${pat}" > $LOGS_DIR_ZIP/lwpthread_$DATE.txt
    return 0
}
netstat_stats () {
    echo "----------------------- netstat -s ---------------------------" > $LOGS_DIR_ZIP/netstat_stats_$DATE
    netstat -s >> $LOGS_DIR_ZIP/netstat_stats_$DATE
    echo  >> $LOGS_DIR_ZIP/netstat_stats_$DATE
    echo  >> $LOGS_DIR_ZIP/netstat_stats_$DATE
    echo "----------------------- netstat -anp ---------------------------" >> $LOGS_DIR_ZIP/netstat_stats_$DATE
    netstat -anp >> $LOGS_DIR_ZIP/netstat_stats_$DATE
}
make_tar () {
    echo TAR_FILE : $LOGS_DIR_ZIP.tar.gz
    tar -zcf $LOGS_DIR_ZIP.tar.gz -C $LOGS_DIR_ZIP . 3>&1 1>&2 2>&3
    rm -rf $LOGS_DIR_ZIP
}
set_info() {
    echo "$1" >  $LOGS_DIR_ZIP/issue_info.txt
}
jvm_top() {
    ./jvmtop.sh --once > $LOGS_DIR_ZIP/jvm_top.txt
}
sys_date() {
    echo `date` >  $LOGS_DIR_ZIP/sys_date.txt
}
getPID(){
   RESTCOMM_PID=$(jps | grep jboss-modules.jar | cut -d " " -f 1)
   while read -r line
   do
    if  ps -ef | grep $line | grep -q  mediaserver
    then
          RMS_PID=$line
   fi
   done < <(jps | grep Main | cut -d " " -f 1)
  if [[ -z "$RESTCOMM_PID"  ]]; then
        echo "RestComm is not Running, will try to collect all available logs"
  fi
  if [[ -z "$RMS_PID" ]]; then
        echo "RMS is not Running, will try to collect all available logs"
  fi
}
usage () {
   cat << EOF
Usage: logs_collect.sh  <options>
options:
-m : optional message of the problem.
-t : Restcomm log file time extractor (e.g "06:20:0*,06:23:0*").
-h : prints this message
-z : Create .tar file
EOF
   exit 1
}
#MAIN
tflag=false
zflag=false
TEMP=`getopt --long -o ":t:m:hz" "$@"`
eval set -- "$TEMP"
while true ; do
    case "$1" in
        -m )
           mkdir -p $LOGS_DIR_ZIP
           set_info "$2"
           shift 2
        ;;
        -t )
           tflag=true
           var=$2
           shift 2
        ;;
         -z )
           zflag=true
           break
        ;;
        -h )
           usage
        ;;
        *)
            break
        ;;
    esac
done;
if [ ! -e $LOGS_DIR_ZIP ]; then
   echo "create DIR $LOGS_DIR_ZIP"
   mkdir -p $LOGS_DIR_ZIP
fi
if [ -d "$LOGS_DIR_ZIP" ]; then
    getPID
    restcomm_logs
    mediaserver_logs
    system_logs
    JVM_perfo_stats
    jvm_process_info
    LWP_threads_logs
    system_usage_info
    netstat_stats
    jvm_top
    sys_date
    if $tflag ; then
        restcomm_logs_bytime $var
    fi
    if $zflag ; then
        make_tar
    fi
else
     echo "Directory $LOGS_DIR_ZIP not created, going to exit."
      exit 1
fi
