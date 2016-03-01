#! /bin/bash
##
## Description: Executes all RestComm configuration scripts for a given version.
## Author     : Henrique Rosa
##
BASEDIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

# load configuration values
#source $BASEDIR/restcomm.conf

echo ''
echo 'RestComm automatic configuration started:'
for f in $BASEDIR/autoconfig.d/*.sh; do
	echo "Executing configuration file $f..."
	source $f
	echo "Finished executing configuration file $f!"
	echo ''
done
echo 'RestComm automatic configuration finished!'
echo ''