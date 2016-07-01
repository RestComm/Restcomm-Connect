#!/bin/bash
##
## Description: Manual DB upgrade script for 770
## Author     : Lefteris Banos
#


#Help function
function HELP {
   echo " USE: ./manual_upgrade.sh -D Database -H db_host -U user -P password"
  exit 1
}


#Check the number of arguments. If none are passed, print help and exit.
NUMARGS=$#
echo -e \\n"Number of arguments: $NUMARGS"
if [ $NUMARGS -eq 0 ]; then
  HELP
fi


while getopts ":D:H:U:P:" opt; do
  case $opt in
    D)
      echo "-D was triggered, Parameter: $OPTARG" >&2
      dbschema=$OPTARG
      ;;
    H)
      echo "-H was triggered, Parameter: $OPTARG" >&2
      dbhost=$OPTARG
      ;;
    U)
      echo "-U was triggered, Parameter: $OPTARG" >&2
      dbuser=$OPTARG
      ;;
    P)
      echo "-P was triggered, Parameter: *********" >&2
      dbpassword=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      HELP
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

declare -i k=1
m=`ls -l *.sql | wc -l`
for i in $(seq 1 $m);
do
   ls ./V7_7_${k}_*
   sed -e "s|USE \${RESTCOMM_DBNAME};|USE ${dbschema};|" ./V7_7_${k}_* | mysql -h $dbhost -P 3306 -u $dbuser -p$dbpassword  ${DBschema}
   k=k+1
done



