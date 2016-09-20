# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure('2') do |config|
  config.vm.define 'restcomm-connect' do |machine|
    machine.vm.box = "ubuntu/trusty64"

    machine.vm.network "private_network", type: "dhcp"
  end

  config.vm.provision :shell, :path => "provision/setup.sh"
end
