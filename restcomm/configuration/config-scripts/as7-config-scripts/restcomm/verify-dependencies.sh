#!/bin/bash
## Description: Verifies if all dependencies are installed.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

verifyJava() {
	if [ -n "$(which java)" ]; then
		
		if [ $(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f2) -ne "7" ]; then
             echo "Only Java 1.7 required."
             exit 1
		fi
	
	else
        echo "Java dependency is missing."
        echo "CentOS/RHEL: java-1.7.0-openjdk-devel.x86_64"
        echo "Debian/Ubuntu:"
        echo "    add-apt-repository ppa:openjdk-r/ppa"
        echo "    apt-get update"
        echo "    apt-get install openjdk-7-jdk"
        echo "macOS: brew cask install java7"
        exit 1
	fi
}

verifyTmux() {
    if [ -z "$(which tmux)" ]; then
        echo "TMux dependency is missing."
        echo "CentOS/RHEL: yum install tmux"
        echo "Debian/Ubuntu: apt-get install tmux"
        echo "macOS: brew install tmux"
        exit 1
    fi
}

verifyXmlstarlet() {
    if [ -z "$(which xmlstarlet)" ]; then
        echo "XML Starlet dependency is missing."
        echo "CentOS/RHEL: yum install xmlstarlet"
        echo "Debian/Ubuntu: apt-get install xmlstarlet"
        echo "macOS: brew install xmlstarlet"
        exit 1
    fi
}

verifyIpcalc() {
    if [ -z "$(which ipcalc)" ]; then
        echo "IP Calc dependency is missing."
        echo "CentOS/RHEL: yum install ipcalc"
        echo "Debian/Ubuntu: apt-get install ipcalc"
        echo "macOS: brew install ipcalc"
        exit 1
    fi
}

verifyJava
verifyTmux
verifyXmlstarlet
verifyIpcalc
