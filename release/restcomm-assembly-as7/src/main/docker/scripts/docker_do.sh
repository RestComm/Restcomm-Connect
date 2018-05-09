#!/bin/bash

DATE=$(date +%F_%H_%M)
DIR_NAME=restcomm_$DATE

RESTCOMM_LOG_BASE=/var/log
RESTCOMM_TRACE=$RESTCOMM_LOG_BASE/$RESTCOMM_TRACE_LOG
LOGS_DIR_ZIP=$RESTCOMM_LOG_BASE/$DIR_NAME
LOGS_DIR_HOST=$LOGS_DIR_ZIP/host


SYSLOGS_DIR=/var/log

restcomm_version () {
 if [ -d "$LOGS_DIR_HOST" ]; then
     cp $RESTCOMM_LOG_BASE/version $LOGS_DIR_HOST/
  return 0
 fi
   exit 1
}

docker_logs () {
if [ -d "$LOGS_DIR_HOST" ]; then
    docker logs $1 2>&1 | head -n 1250 > $LOGS_DIR_HOST/DockerLogs
    return 0
 fi
   exit 1
}

system_usage_info () {
if [ -d "$LOGS_DIR_HOST" ]; then
   echo CPU\(s\): `top -b -n1 | grep "Cpu(s)" | awk '{print $2" : " $4}'` >  $LOGS_DIR_HOST/usage_stats_$DATE
   echo  >> $LOGS_DIR_HOST/usage_stats_$DATE
   top -b -n1 | grep Mem >> $LOGS_DIR_HOST/usage_stats_$DATE
   ps aux  > $LOGS_DIR_HOST/top_$DATE
  return 0
 fi
   exit 1
}

sys_date() {
if [ -d "$LOGS_DIR_HOST" ]; then
  echo `date` >  $LOGS_DIR_HOST/sys_date.txt
  return 0
 fi
   exit 1
}

netstat_stats () {

if [ -d "$LOGS_DIR_HOST" ]; then
  echo "----------------------- netstat -s ---------------------------" > $LOGS_DIR_HOST/netstat_stats_$DATE
  netstat -s >> $LOGS_DIR_HOST/netstat_stats_$DATE
  echo  >> $LOGS_DIR_HOST/netstat_stats_$DATE
  echo  >> $LOGS_DIR_HOST/netstat_stats_$DATE
  echo "----------------------- netstat -anp ---------------------------" >> $LOGS_DIR_HOST/netstat_stats_$DATE
  netstat -anp >> $LOGS_DIR_HOST/netstat_stats_$DATE
  return 0
 fi
   exit 1
}

tcpdump_logs () {
if [ -d "$LOGS_DIR_HOST" ]; then
  ls -t1 $RESTCOMM_TRACE/*.pcap* |  head -n 2 | xargs -i -exec cp -p {} $LOGS_DIR_ZIP/
  return 0
 fi
   exit 1
}

system_logs () {
if [ -d "$LOGS_DIR_HOST" ]; then
    if [ -f $SYSLOGS_DIR/messages ]; then
        cp $SYSLOGS_DIR/messages $LOGS_DIR_HOST/
    fi
    if [ -f $SYSLOGS_DIR/syslog ]; then
        cp $SYSLOGS_DIR/syslog $LOGS_DIR_HOST/
    fi
  return 0
 fi
   exit 1
}

collect_logs () {
echo -en "\e[92mpull collect logs container $1 , file: $LOGS_DIR_ZIP \e[0m\n"
time_logs=""
if $tflag ; then
 time_logs="-t $time_marg"
 echo $time_marg
fi

if $zflag ; then
 dtar="-z"
fi

mkdir -p $LOGS_DIR_HOST

tcpdump_logs
docker_logs $1
netstat_stats
system_usage_info
system_logs
restcomm_version
sys_date

docker exec $1  /bin/sh -c "/opt/Restcomm-JBoss-AS7/bin/restcomm/logs_collect.sh $time_logs $dtar"
}


stop_container () {
  echo -en "\e[92mstop container $1\e[0m\n"
  docker stop $1
}

start_container () {
  echo -en "\e[92mstart container $1\e[0m\n"
  docker start $1
}

run_container () {
   echo -en "\e[92mrun container\e[0m\n"
   echo "need to be done"
}

pull_container () {
 echo -en "\e[92mpull container $1\e[0m\n"
 docker pull $1
}

info_container () {
  docker ps;
}

collect_logs_time () {
echo -en "\e[92mpull collect logs by time for container $1\e[0m\n"
docker exec $1  /bin/sh -c "/opt/Restcomm-JBoss-AS7/bin/restcomm/logs_collect.sh $2"
}

docker_login() {
 docker login
}

delete_container () {
read -p "Are you sure about deleting container $1? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo -en "\e[92mdelete container $1\e[0m\n"
    docker rm $1
else
    echo "container no deleted"
    exit 1
fi
}

change_log() {
    echo -en "\e[92mchange log level for $2 to $3 for container $1\e[0m\n"
    docker exec $1  /bin/sh -c "/opt/Restcomm-JBoss-AS7/bin/restcomm/set-log-level.sh $2 $3"
}

usage () {
   cat << EOF
Usage: docker_do.sh  <options>

options:
-s : stop container (-c obligatory).
-S : start container (-c obligatory).
-r : run new container (-H obligatory). Optional (-n).
-H : Host name (only with -r)
-n : Container name (only with -r), if not used default name "telscale-restcomm"
-l : collect logs (-c obligatory).
-z : Create Tar file, and delete the original directory.
-t : collect logs by time (-l obligatory) e.g. "21:03:0*,22:00:0*".
-i : running containers.
-p : pull from hub (-L obligatory).
-L : login to docker hub.
-c : container to use.
-a : change log level (-c CONTAINER-a "servlet govnist" -k WARN or -c CONTAINER -a "servlet" -k INFO , -c CONTAINER -a list , etc)
-k : set log level (Can be used only with -a)
-d : delete container.
-h : prints this message
EOF
   exit 1
}

#MAIN
sflag=false
Sflag=false
rflag=false
lflag=false
cflag=false
pflag=false
iflag=false
Lflag=false
iflag=false
dflag=false
tflag=false
Hflag=false
nflag=false
zflag=false
aflag=false
kflag=false
login=false


#Global variables
 container_name="telscale-restcomm"

if [ $# -eq 0 ];
then
    usage
    exit 0
else

    while getopts ":rH:n:sSp:c:a:k:ilLdht:z" opt; do
      case $opt in
            s )
               sflag=true
            ;;
            S )
               Sflag=true
            ;;
        H )
           Hflag=true
               host=$OPTARG
            ;;
        n )
           nflag=true
              container_name=$OPTARG
            ;;
            r )
               rflag=true
            ;;
            l )
               lflag=true
            ;;
            c )
               cflag=true
               container=$OPTARG
            ;;
            p )
               pflag=true
               pull_repo=$OPTARG
            ;;
            t )
               tflag=true
               time_marg=$OPTARG
            ;;
            i )
               iflag=true
            ;;
            L )
               Lflag=true
            ;;

            d )
               dflag=true
            ;;
             z )
               zflag=true
            ;;
               a )
                aflag=true
                input=$OPTARG
            ;;
            k )
                kflag=true
                level=$OPTARG
            ;;
            h )
               usage
            ;;
            \?)
              echo "Invalid option: -$OPTARG" >&2
              exit 1
            ;;
             :)
              if ("$opt" = "p"); then
                 pull_repo="restcomm/restcomm-cloud:master"
                 read -p "Default hub repo $pull_repo  will be used OK?" -n 1 -r
                 echo    # move to a new line
                    if [[ $REPLY =~ ^[Yy]$ ]]
                    then
                            pflag=true
                            continue
                    else
                            echo
                    fi
              fi
              echo "Option -$OPTARG requires an argument." >&2
              exit 1
            ;;
        esac
    done;


    if [[ ( "$sflag" = "true" || "$Sflag" = "true" || "$lflag" = "true" || "$dflag" = "true" || "$aflag" = "true"  )  &&  ( "$cflag" = "false" ) ]]; then
        echo " For flags (-s, -S, -l, -d, -a)  a container must  be specified  (-c) " >&2
        exit 1
    fi

    if [[ "$aflag" = "true" && "$input" != "list"  &&  "$kflag" = "false" ]]; then
        echo " For flag -a  flag -k  must  be specified " >&2
        exit 1
    fi

    if [[ "$tflag" = "true"  &&  "$lflag" = "false" ]]; then
        echo " For flag -t  flag -l  must  be specified " >&2
        exit 1
    fi

    if [[ "$rflag" = "true"  &&  "$Hflag" = "false" ]]; then
        echo " To run a container Hostname is necessary (flag -H) " >&2
        exit 1
    fi

    if $lflag ; then
     collect_logs $container
    fi


    if $sflag ; then
     stop_container $container
    fi

    if $Lflag ; then
        login=true
        docker_login
    fi

    if $pflag ; then
       if ! $login ; then
        echo -en  "\e[31mMake sure you are connected to docker hub (-L)\e[0m\n"
       fi
        pull_container $pull_repo
    fi

    if $dflag ; then
        delete_container $container
    fi

    if $Sflag ; then
        start_container $container
    fi

    if $rflag ; then
        run_container $host
    fi

    if $iflag ; then
        info_container
    fi

    if $aflag ; then
        change_log $container \""$input"\" $level
    fi
fi