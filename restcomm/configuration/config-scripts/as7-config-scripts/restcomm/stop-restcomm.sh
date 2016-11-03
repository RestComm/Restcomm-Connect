#! /bin/bash
##
## Description: Stops RestComm and Media Server processes running on terminal sessions
## Authors    : Henrique Rosa (henrique.rosa@telestax.com)
##

# Stop Media Server
local basedir=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)
$basedir/stop-mediaserver.sh

# Stop RestComm
echo 'Shutting down RestComm...'
if tmux ls | grep -q 'restcomm'; then
	tmux kill-session -t 'restcomm'
	echo '...stopped RestComm instance running on terminal session "restcomm"!'
else
	echo '...restComm already stopped!'
fi