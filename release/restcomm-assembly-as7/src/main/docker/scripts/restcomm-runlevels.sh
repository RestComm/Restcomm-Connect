#!/bin/bash

mkdir -p /etc/runit/runsvdir/default
mkdir -p /etc/runit/runsvdir/second

cd /etc/runit/runsvdir
ln -s default /etc/runit/runsvdir/current
cp -pR /etc/service/* /etc/runit/runsvdir/current/
mv -f /etc/service /service.old && ln -s /etc/runit/runsvdir/current /etc/service

#auto delete script after run once. No need more.
rm -- "$0"