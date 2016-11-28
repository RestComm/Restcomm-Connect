#!/bin/bash
## Description: Change 'sed' to 'gsed' for MacOS users in order for the autoconfig scripts to work properly
## Prerequisites: Install gnu-sed using homebrew (brew install gnu-sed)
## Author: George Vagenas
gsed -i 's/sed/gsed/g' ./*.sh
gsed -i 's/sed/gsed/g' ./autoconfig.d/*.sh
