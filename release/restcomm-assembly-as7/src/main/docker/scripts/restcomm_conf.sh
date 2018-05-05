#!/bin/bash
#Maintainer Lefteris Banos -liblefty@telestax.com
#Maintainer Maria Farooq -maria.farooq@telestax.com

echo "Will configure advanced.conf"

source /etc/container_environment.sh

BASEDIR=/opt/Restcomm-JBoss-AS7

getOptions() {
    ( set -o posix ; set ) | grep "^$1" | awk -F "$1" '{print $2}'
}

configValues() {
    local options=$(getOptions $1)
    if [ -n "$options" ]; then
        while read -r option; do
            IFS='=' read ar1 ar2 <<<$option
            cli_var="CLI_$ar1"
            if env | grep -q ^$cli_var= ;then
                ar2=${!cli_var}
            fi
                if [ ! -z "$2" ]; then
                    echo "Setting $ar1 to $ar2"
                    sed -i "s|${ar1}=.*|${ar1}=${ar2}|" $BASEDIR/bin/restcomm/$2
                fi
                echo -e "$ar2" | xargs > /etc/container_environment/$ar1
        done <<< "$options"
    fi
}

#MAIN
# Declare associative array
typeset -A OPTS=( [RCBCONF_]=restcomm.conf [RCADVCONF_]=advanced.conf [EXTCONF_]="" [LBCONF_]=advanced.conf [RMSCONF_]=mediaserver.conf )

   for opt in "${!OPTS[@]}"; do
       printf 'conf value %s file: %q\n' "$opt" "${OPTS[$opt]}"
       configValues $opt ${OPTS[$opt]}
   done

#auto delete script after run once. No need more.
rm -- "$0"
