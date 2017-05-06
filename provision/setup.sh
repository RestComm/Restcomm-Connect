#!/bin/bash
sudo apt-get install software-properties-common
yes|sudo apt-add-repository ppa:ansible/ansible
sudo apt-get update
sudo apt-get install -y ansible
cd /vagrant/provision
ansible-playbook vagrant.yml
