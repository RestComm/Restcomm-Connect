#!/bin/bash
# Description: Script to create and publish an RestComm 6.1.0 AMI #
# Author     : Henrique Rosa (henrique.rosa@telestax.com)         #
# Notes      : This script was created on Cent OS 6.4             #

# VARIABLES
AMI_RESTCOMM_REL='TelScale-restcomm-6.1.0.GA'
AMI_DEPENDENCIES='/tmp/ec2-ami/required'

# Image variables
AMI_OS='centos'                                            # Operating System of the image
AMI_OS_REL='6.4'                                           # Version of the Operating System
AMI_ARCH='x86_64'                                          # Architecture of the image
AMI_SIZE=2048                                              # Size of the image (Mb)
AMI_AKI='aki-88aa75e1'                                     # Amazon Kernel of the image
AMI_NAME="$AMI_OS-$AMI_OS_REL-$AMI_ARCH-$AMI_RESTCOMM_REL" # Name of the image. Do not use whitespace.
AMI_FILENAME="$AMI_NAME.img"                               # Filename of the image
AMI_TYPE='m1.large'                                        # Instance Type

# Installation Variables
AMI_MOUNTPOINT='/mnt/ec2-ami'                              # Where to mount the image
AMI_INSTALL_DIR='/opt'
AMI_INSTALL_RESTCOMM="$AMI_INSTALL_DIR/$AMI_RESTCOMM_REL"
AMI_INSTALL_MMS="$AMI_INSTALL_DIR/$AMI_RESTCOMM_REL/telscale-media/telscale-media-server"

AMI_TMP_DIR="/tmp/ec2-ami/$AMI_NAME"                      # Where to store installation files
AMI_BUNDLE_DIR="$AMI_TMP_DIR/bundle"                      # Where to bundle the image
AMI_MANIFEST="$AMI_BUNDLE_DIR/$AMI_FILENAME.manifest.xml" # Image manifest
AMI_DOWNLOADS="$AMI_TMP_DIR/downloads"                    # Where to download files required for installation
AMI_DOWNLOADS_SQL="$AMI_DOWNLOADS/sql"                    # Where MariaDB scripts are located
AMI_USERS_DIR="$AMI_TMP_DIR/users"                        # Where to store user account files
AMI_YUM_DIR="$AMI_TMP_DIR/yum"                            # Temporary yum directory
AMI_YUM_REPO="$AMI_YUM_DIR/yum-xen.conf"                  # Yum repository used in installation

# EC2 Variables
AMI_BUCKET="telscale/ami/telscale-restcomm/$AMI_NAME"      # S3 bucket name
AMI_SECURITY_GROUP="$AMI_NAME-security"                    # Security group name for the AMI. Must be unique.

# Create temporary directories for installation
mkdir -p $AMI_TMP_DIR/{bundle,downloads/sql,users,yum}

####################
# CREATE THE IMAGE #
####################
# Create image with ext3 file system
dd if=/dev/zero of=$AMI_FILENAME bs=1M count=$AMI_SIZE
mke2fs -F -j $AMI_FILENAME

# Mount image in the selected directory
mkdir -p $AMI_MOUNTPOINT
mount -o loop $AMI_FILENAME $AMI_MOUNTPOINT

# Create directories in the root file system to hold system files and devices
mkdir -p $AMI_MOUNTPOINT/{dev,etc,proc,sys}
mkdir -p $AMI_MOUNTPOINT/var/{cache,log,lock,lib/rpm}

# Create devices
/sbin/MAKEDEV -d $AMI_MOUNTPOINT/dev -x console
/sbin/MAKEDEV -d $AMI_MOUNTPOINT/dev -x null
/sbin/MAKEDEV -d $AMI_MOUNTPOINT/dev -x zero
/sbin/MAKEDEV -d $AMI_MOUNTPOINT/dev -x urandom

# Mount dev, pts, shm, proc, and sys in the new root file system.
mount -o bind /dev $AMI_MOUNTPOINT/dev
mount -o bind /dev/pts $AMI_MOUNTPOINT/dev/pts
mount -o bind /dev/shm $AMI_MOUNTPOINT/dev/shm
mount -o bind /proc $AMI_MOUNTPOINT/proc
mount -o bind /sys $AMI_MOUNTPOINT/sys

#########
# DISKS #
#########
# Create the fstab file for the image.
# Notice the use of xen drivers
# xvdf and xvdg drives are typical of m1.large instances
cat > $AMI_MOUNTPOINT/etc/fstab << 'EOF'
/dev/xvde1  /           ext3         defaults          1    1
none        /dev/pts    devpts       gid=5,mode=620    0    0
none        /dev/shm    tmpfs        defaults          0    0
none        /proc       proc         defaults          0    0
none        /sys        sysfs        defaults          0    0
EOF

################
# INSTALLATION #
################
# Create a temporary file with yum repository
mkdir -p $AMI_YUM_DIR

cat > $AMI_YUM_REPO << 'EOF'
# CentOS-Base.repo
#
# The mirror system uses the connecting IP address of the client and the
# update status of each mirror to pick mirrors that are updated to and
# geographically close to the client.  You should use this for CentOS updates
# unless you are manually picking other mirrors.
#
# If the mirrorlist= does not work for you, as a fall back you can try the 
# remarked out baseurl= line instead.
#
#
[base]
name=CentOS-$releasever - Base
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=os
#baseurl=http://mirror.centos.org/centos/$releasever/os/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

#released updates 
[updates]
name=CentOS-$releasever - Updates
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=updates
#baseurl=http://mirror.centos.org/centos/$releasever/updates/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

#additional packages that may be useful
[extras]
name=CentOS-$releasever - Extras
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=extras
#baseurl=http://mirror.centos.org/centos/$releasever/extras/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

#additional packages that extend functionality of existing packages
[centosplus]
name=CentOS-$releasever - Plus
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=centosplus
#baseurl=http://mirror.centos.org/centos/$releasever/centosplus/$basearch/
gpgcheck=1
enabled=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6

#contrib - packages by Centos Users
[contrib]
name=CentOS-$releasever - Contrib
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=contrib
#baseurl=http://mirror.centos.org/centos/$releasever/contrib/$basearch/
gpgcheck=1
enabled=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
EOF

cat > $AMI_YUM_DIR/mariadb.conf << 'EOF'
[mariadb]
name = MariaDB
baseurl = http://yum.mariadb.org/5.5/centos6-amd64
gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
gpgcheck=1
EOF

# Install packages
yum -c $AMI_YUM_REPO --installroot=$AMI_MOUNTPOINT --releasever=$AMI_OS_REL -y groupinstall Base
yum -c $AMI_YUM_REPO --installroot=$AMI_MOUNTPOINT --releasever=$AMI_OS_REL -y install sudo
yum -c $AMI_YUM_REPO --installroot=$AMI_MOUNTPOINT --releasever=$AMI_OS_REL -y install *openssh* dhclient
yum -c $AMI_YUM_REPO --installroot=$AMI_MOUNTPOINT --releasever=$AMI_OS_REL -y install grub
yum -c $AMI_YUM_DIR/mariadb.conf --installroot=$AMI_MOUNTPOINT --releasever=$AMI_OS_REL -y install MariaDB-client MariaDB-server

# Install Oracle JDK 6
cp $AMI_DEPENDENCIES/jdk-6u45-linux-x64-rpm.bin $AMI_MOUNTPOINT/tmp
chroot $AMI_MOUNTPOINT chmod +x /tmp/jdk-6u45-linux-x64-rpm.bin
chroot $AMI_MOUNTPOINT /tmp/jdk-6u45-linux-x64-rpm.bin
rm $AMI_MOUNTPOINT/tmp/jdk-6u45-linux-x64-rpm.bin

# The following steps are necessary to unmount /proc device
# This service is started by Oracle JDK after RPM installation.
chroot $AMI_MOUNTPOINT /etc/init.d/jexec stop

# Extract RestComm to $AMI_MOUNTPOINT/opt
#wget --user telscale-file-server@telestax.com --password t31sca13fi13s ftp://ftp.box.com/TelScale%20restcomm/6.1.0.GA/CR2/TelScale-restcomm-6.1.0.CR2_for-AMI.zip -O $AMI_DOWNLOADS
#unzip $AMI_DOWNLOADS/TelScale-restcomm-6.1.0.CR2_for-AMI.zip -d $AMI_MOUNTPOINT$AMI_INSTALL_DIR
unzip $AMI_DEPENDENCIES/TelScale-restcomm-6.1.0.GA-1308060012.zip -d $AMI_MOUNTPOINT$AMI_INSTALL_DIR

# Extract deployed RestComm
mkdir $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war
unzip $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/telscale-restcomm.war -d $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war
rm $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/telscale-restcomm.war

# Replace licensing jar since AMI will not require a license.
rm -f $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/jbossweb.sar/sip-servlets-impl-6.1.3.GA-TelScale.jar
cp $AMI_DEPENDENCIES/sip-servlets-impl-6.1.3.GA-TelScale-no-license.jar $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/jbossweb.sar/sip-servlets-impl-6.1.3.GA-TelScale.jar

# MSS and Shiro Issue: jar needs to be removed
rm -f $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/snmp-adaptor.sar/xml-apis-1.3.04.jar
echo "Removed conflicting file $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/snmp-adaptor.sar/xml-apis-1.3.04.jar"
rm -f $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/activation-1.1.jar
echo "Removed conflicting file $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/activation-1.1.jar"
rm -f $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/mail-1.4.jar
echo "Removed conflicting file $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/mail-1.4.jar"
rm -f $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/servlet-api-6.0.35.jar
echo "Removed conflicting file $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib/servlet-api-6.0.35.jar"

# Download and Install Java Connector for MariaDB
wget https://downloads.mariadb.org/f/client-java-1.1.3/mariadb-java-client-1.1.3.jar/from/http:/mirror.stshosting.co.uk/mariadb -O $AMI_DOWNLOADS/mariadb-java-client-1.1.3.jar
cp $AMI_DOWNLOADS/mariadb-java-client-1.1.3.jar $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/lib
echo "Installed MariaDB Java Client 1.1.3"

# Configuration file for MariaDB 
cat > $AMI_MOUNTPOINT/etc/my.cnf.d/server.cnf << 'EOF'
#
# These groups are read by MariaDB server.
# Use it for options that only the server (but not clients) should see
#
# See the examples of server my.cnf files in /usr/share/mysql/
#

# this is read by the standalone daemon and embedded servers
[server]

# this is only for the mysqld standalone daemon
[mysqld]
user=mysql
port = 3306
data = /var/lib/mysql
socket = /var/lib/mysql/mysql.sock
log-error = /var/log/mysql.err
general-log-file = /var/log/mysql.log

# this is only for embedded server
[embedded]

# This group is only read by MariaDB-5.5 servers.
# If you use the same .cnf file for MariaDB of different versions,
# use this group for options that older servers don't understand
[mysqld-5.5]

# These two groups are only read by MariaDB servers, not by MySQL.
# If you use the same .cnf file for MySQL and MariaDB,
# you can put MariaDB-only options here
[mariadb]

[mariadb-5.5]

EOF

# Startup MariaDB on boot
chroot $AMI_MOUNTPOINT chkconfig --add mysql
chroot $AMI_MOUNTPOINT chkconfig --level 345 mysql on

# Change MariaDB root password
chroot $AMI_MOUNTPOINT service mysql start
chroot $AMI_MOUNTPOINT mysqladmin -u root password 't3l35taxr00t'

# Create RestComm Database
# TODO SQL scripts should be downloaded... Lets host them somewhere
cp $AMI_DEPENDENCIES/mariadb/init.sql $AMI_MOUNTPOINT/tmp
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t < $AMI_MOUNTPOINT/tmp/init.sql
rm $AMI_MOUNTPOINT/tmp/init.sql
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='show databases;'
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='show tables from restcomm;'

# Create user to be used by Restcomm for restcomm database
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='CREATE USER "telestax"@"%" IDENTIFIED BY "m0b1c3nt5";'
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='select User, Host from mysql.user;'
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='GRANT ALL PRIVILEGES ON restcomm.* To "telestax"@"%" IDENTIFIED BY "m0b1c3nt5";'
chroot $AMI_MOUNTPOINT mysql --password=t3l35taxr00t --execute='show grants for "telestax"@"%";'

# Stop MariaDB
chroot $AMI_MOUNTPOINT service mysql stop

# TODO SQL scripts should be downloaded... Lets host them somewhere
# Replace RestComm HSQL scripts with MySQL scripts
cat $AMI_DEPENDENCIES/mariadb/accounts.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/accounts.xml
cat $AMI_DEPENDENCIES/mariadb/announcements.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/announcements.xml
cat $AMI_DEPENDENCIES/mariadb/applications.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/applications.xml
cat $AMI_DEPENDENCIES/mariadb/available-phone-numbers.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/available-phone-numbers.xml
cat $AMI_DEPENDENCIES/mariadb/call-detail-records.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/call-detail-records.xml
cat $AMI_DEPENDENCIES/mariadb/clients.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/clients.xml
cat $AMI_DEPENDENCIES/mariadb/gateways.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/gateways.xml
cat $AMI_DEPENDENCIES/mariadb/http-cookies.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/http-cookies.xml
cat $AMI_DEPENDENCIES/mariadb/incoming-phone-numbers.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/incoming-phone-numbers.xml
cat $AMI_DEPENDENCIES/mariadb/notifications.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/notifications.xml
cat $AMI_DEPENDENCIES/mariadb/outgoing-caller-ids.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/outgoing-caller-ids.xml
cat $AMI_DEPENDENCIES/mariadb/recordings.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/recordings.xml
cat $AMI_DEPENDENCIES/mariadb/registrations.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/registrations.xml
cat $AMI_DEPENDENCIES/mariadb/sand-boxes.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/sand-boxes.xml
cat $AMI_DEPENDENCIES/mariadb/short-codes.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/short-codes.xml
cat $AMI_DEPENDENCIES/mariadb/sms-messages.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/sms-messages.xml
cat $AMI_DEPENDENCIES/mariadb/transcriptions.xml > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/sql/transcriptions.xml

# Create script to configure MMS, AS and RestComm
cat > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/autoconfigure.sh << 'EOF'
#! /bin/sh
# Description: Configures TelScale RestComm 6.1.0.GA    #
# Input Params:                                         #
#               1.The install dir of TelScale RestComm  #
# Dependencies:                                         #
#               1.ipcalc                                #
#                                                       #
# Authors: Thomas Quintana thomas.quintana@telestax.com #
#          Henrique Rosa   henrique.rosa@telestax.com   #
IP_ADDRESS_PATTERN="[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}.[0-9]\{1,3\}"
INTERFACE="eth0"

INSTALL_DIR=/opt/TelScale-restcomm-6.1.0.GA
MMS_DIR=$INSTALL_DIR/telscale-media/telscale-media-server
SERVER_DIR=$INSTALL_DIR/server/default
DEPLOY_DIR=$SERVER_DIR/deploy
WEBCONTAINER_DIR=$DEPLOY_DIR/jbossweb.sar
RESTCOMM_DIR=$DEPLOY_DIR/restcomm.war

# Configures Mobicents Media Server
# Params:
#        1. Private IP
#        2. Local Network
#        3. Local Subnet
configMediaServer() {
	FILE=$MMS_DIR/deploy/server-beans.xml
	FILE_BAK="$FILE.bak"

	mv $FILE $FILE_BAK
	sed -e "s|<property name=\"bindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"bindAddress\">$1<\/property>|" -e "s|<property name=\"localBindAddress\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localBindAddress\">$1<\/property>|" -e "s|<property name=\"localNetwork\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localNetwork\">$2<\/property>|" -e "s|<property name=\"localSubnet\">$IP_ADDRESS_PATTERN<\/property>|<property name=\"localSubnet\">$3<\/property>|" -e 's|<property name="useSbc">.*</property>|<property name="useSbc">true</property>|' -e 's|<property name="dtmfDetectorDbi">.*</property>|<property name="dtmfDetectorDbi">36</property>|' -e 's|<response-timeout>.*</response-timeout>|<response-timeout>5000</response-timeout>|' $FILE_BAK > $FILE;
    rm $FILE_BAK
    
    grep -q -e '<property name="lowestPort">[0-9]*</property>' $FILE || sed -i '/rtpTimeout/ a\
    <property name="lowestPort">64534</property>' $FILE;
    
    grep -q -e '<property name="highestPort">[0-9]*</property>' $FILE || sed -i '/rtpTimeout/ a\
    <property name="highestPort">65535</property>' $FILE;
    
    # Specify JAVA_OPTS for MMS
    FILE=$MMS_DIR/bin/run.sh
    
    sed -ie "s|JAVA_OPTS=\"-Dprogram.name=\$PROGNAME \$JAVA_OPTS\"|JAVA_OPTS=\"-Xmx2g -Dprogram.name=\$PROGNAME \$JAVA_OPTS\"|" $FILE;
    
    # Create folder for log4j
	mkdir -p $MMS_DIR/log
}

# Configures JBoss Web Container (TomCat)
# Params:
#        1. Private IP
#        2. Elastic IP
configApplicationServer() {
	FILE=$WEBCONTAINER_DIR/server.xml
	FILE_BAK="$FILE.bak"
	
	mv $FILE $FILE_BAK
	sed -e "s|ipAddress = \".*\"|ipAddress = \"$1\"|g" -e "s|staticServerAddress=\"$IP_ADDRESS_PATTERN\"|staticServerAddress=\"$2\"|g" -e 's|useStaticAddress="false"|useStaticAddress="true"|g' $FILE_BAK > $FILE;
	rm $FILE_BAK
	
	grep -q -e "staticServerAddress=\"$IP_ADDRESS_PATTERN\"" $FILE || sed -i "/Connector port=\"508[0-1]\"/ a\
	staticServerAddress=\"$2\"" $FILE;
	
	grep -q -e "useStaticAddress=\"[a-zA-Z]*\"" $FILE || sed -i "/Connector port=\"508[0-1]\"/ a\
	useStaticAddress=\"true\"" $FILE;
	
	grep -q -e 'staticServerPort="5080"' $FILE || sed -i '/Connector port="5080"/ a\
	staticServerPort="5080"' $FILE;
	
	grep -q -e 'staticServerPort="5081"' $FILE || sed -i '/Connector port="5081"/ a\
	staticServerPort="5081"' $FILE;
}

# Configures RestComm
# Params:
#        1. Private IP
#        2. Elastic IP
configRestComm() {
	FILE=$RESTCOMM_DIR/WEB-INF/conf/restcomm.xml
	FILE_BAK="$FILE.bak"
	
	OUTBOUND_IP='64.136.174.30'
	
	mv $FILE $FILE_BAK
	sed -e "s|<local-address>$IP_ADDRESS_PATTERN<\/local-address>|<local-address>$1<\/local-address>|" -e "s|<remote-address>$IP_ADDRESS_PATTERN<\/remote-address>|<remote-address>$1<\/remote-address>|" -e "s|<\!--.*<external-ip>.*<\/external-ip>.*-->|<external-ip>$2<\/external-ip>|" -e "s|<external-ip>.*<\/external-ip>|<external-ip>$2<\/external-ip>|" -e "s|<external-address>.*<\/external-address>|<external-address>$2<\/external-address>|" -e "s|<\!--.*<external-address>.*<\/external-address>.*-->|<external-address>$2<\/external-address>|" -e "s|<prompts-uri>http:\/\/$IP_ADDRESS_PATTERN:8080\/restcomm\/audio<\/prompts-uri>|<prompts-uri>http:\/\/$2:8080\/restcomm\/audio<\/prompts-uri>|" -e "s|<cache-uri>http:\/\/$IP_ADDRESS_PATTERN:8080\/restcomm\/cache<\/cache-uri>|<cache-uri>http:\/\/$1:8080\/restcomm\/cache<\/cache-uri>|" -e "s|<recordings-uri>http:\/\/$IP_ADDRESS_PATTERN:8080\/restcomm\/recordings<\/recordings-uri>|<recordings-uri>http:\/\/$2:8080\/restcomm\/recordings<\/recordings-uri>|" -e "s|<error-dictionary-uri>http:\/\/$IP_ADDRESS_PATTERN:8080\/restcomm\/errors<\/error-dictionary-uri>|<error-dictionary-uri>http:\/\/$2:8080\/restcomm\/errors<\/error-dictionary-uri>|" -e "s|<outbound-proxy-uri>.*<\/outbound-proxy-uri>|<outbound-proxy-uri>$OUTBOUND_IP<\/outbound-proxy-uri>|" -e "s|<outbound-endpoint>.*<\/outbound-endpoint>|<outbound-endpoint>$OUTBOUND_IP<\/outbound-endpoint>|" -e 's|<outbound-prefix>.*</outbound-prefix>|<outbound-prefix>#</outbound-prefix>|' $FILE_BAK > $FILE;
	rm $FILE_BAK

	# Update restcomm.xml with EC2 user data parameters
        source $INSTALL_DIR/autoconfigure-user-data.sh
	
	# Specify JAVA_OPTS for RestComm
	FILE=$INSTALL_DIR/bin/run.conf
	sed -ie 's|JAVA_OPTS="-Xms128m -Xmx512m -XX:MaxPermSize=256m -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"|JAVA_OPTS="-Xms128m -Xmx2g -XX:MaxPermSize=256m -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70"|' $FILE;
}

# Configures MyBatis
# Params: 
#         1. Private IP
configMyBatis() {
	FILE=$RESTCOMM_DIR/WEB-INF/conf/mybatis.xml
	FILE_BAK="$FILE.bak"
	
	mv $FILE $FILE_BAK
	sed -e 's|value=".*Driver"|value="org.mariadb.jdbc.Driver"|' -e "s|value=\"jdbc:.*\"|value=\"jdbc:mariadb://$1:3306/restcomm\"|" -e 's|property name="username" value=".*"|property name="username" value="telestax"|' -e 's|property name="password" value=".*"|property name="password" value="m0b1c3nt5"|' $FILE_BAK > $FILE;
	rm $FILE_BAK
}

# Configures Mobicents Properties file
configMobicentsProps() {
	FILE=$SERVER_DIR/conf/dars/mobicents-dar.properties
	FILE_BAK="$FILE.bak"
	
	mv $FILE $FILE_BAK
	sed -e 's|^ALL:.*|ALL: ("RestComm", "DAR\:From", "NEUTRAL", "", "NO_ROUTE", "0")|' $FILE_BAK > $FILE;
	rm $FILE_BAK
	
	grep -q -e '^ALL:' $FILE || echo -e '\nALL: ("RestComm", "DAR\:From", "NEUTRAL", "", "NO_ROUTE", "0")' >> $FILE;
}

getIpAddress() {
  /sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $2}' | awk '{print $1}';
}

getElasticIp() {
  wget -qO- http://instance-data/latest/meta-data/public-ipv4
}

getBroadcastAddress() {
  /sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $3}' | awk '{print $1}';
}

getSubnetMask()
{
  /sbin/ifconfig $INTERFACE | grep "inet addr" | awk -F: '{print $4}' | awk '{print $1}';
}

getNetwork() {
  ipcalc -n $1 $2 | grep -i "Network" | awk -F= '{print $2}';
}

IP_ADDRESS=`getIpAddress`
SUBNET_MASK=`getSubnetMask`
ELASTIC_IP=`getElasticIp`
NETWORK=`getNetwork $IP_ADDRESS $SUBNET_MASK`
BROADCAST_ADDRESS=`getBroadcastAddress`

echo "Private IP is: $IP_ADDRESS"
echo "Subnet Mask is: $SUBNET_MASK"
echo "Elastic IP is: $ELASTIC_IP"
echo "Network is: $NETWORK"
echo "Broadcast Address is: $BROADCAST_ADDRESS"
echo ''
configMediaServer $IP_ADDRESS $NETWORK $SUBNET_MASK
echo 'Configured Mobicents Media Server!'

configApplicationServer $IP_ADDRESS $ELASTIC_IP
echo 'Configured JBoss Web Container!'

configRestComm $IP_ADDRESS $ELASTIC_IP
echo 'Configured RestComm!'

configMyBatis $IP_ADDRESS
echo 'Configured MyBatis!'

configMobicentsProps
echo 'Configured Mobicents Properties!'
EOF

# Create script to configure MMS, AS and RestComm
cat > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/autoconfigure-user-data.sh << 'EOF'
#/bin/bash
# This file pulls the user configuration from Amazon and updates
# the TelScale RestComm configuration file.
# 
# When the user is prompted he/she should enter the user data
# in the Amazon console in the following format:
#
# vi_login=alice
# vi_password=bob
# vi_endpoint=123456
# interfax_user=alice
# ...
#
# Please see below for a list of variables. If you need more details about the
# meaning of these variables please find that information in the restcomm.xml
# file.

# VoIP Innovations variable declarations.
vi_login=""
vi_password=""
vi_endpoint=""
# Interfax variable declarations.
interfax_user=""
interfax_password=""
# ISpeech variable declarations.
ispeech_key=""
# Acapela variable declarations.
acapela_application=""
acapela_login=""
acapela_password=""

# Fetch the configuration data.
configuration=`curl http://169.254.169.254/latest/user-data`
# Iterate over the configuration data passed in by the user
# parsing the data as we go.
while read -r line; do
	# Split each line into key/value pairs.
	key=`echo $line | gawk -F'=' '{ print $1 }'`
	value=`echo $line | gawk -F'=' '{ print $2 }' | sed -e 's/^ *//g;s/ *$//g'`
	# Set the the configuration values.
	if [ "$key" = "vi_login" ]; then
		vi_login=$value;
	elif [ "$key" = "vi_password" ]; then
		vi_password=$value;
	elif [ "$key" = "vi_endpoint" ]; then
		vi_endpoint=$value;
	elif [ "$key" = "interfax_user" ]; then
		interfax_user=$value;
	elif [ "$key" = "interfax_password" ]; then
		interfax_password=$value;
	elif [ "$key" = "ispeech_key" ]; then
		ispeech_key=$value;
	elif [ "$key" = "acapela_application" ]; then
		acapela_application=$value;
	elif [ "$key" = "acapela_login" ]; then
		acapela_login=$value;
	elif [ "$key" = "acapela_password" ]; then
		acapela_password=$value;
	fi
done <<< "$configuration"

# The path to the RestComm configuration file inside the AMI.
restcomm_config_file=/opt/TelScale-restcomm-6.1.0.GA/server/default/deploy/restcomm.war/WEB-INF/conf/restcomm.xml

# Create a back up of the file.
cp -f $restcomm_config_file $restcomm_config_file.bak

# Update the restcomm.xml configuration file.
sed -e "/<voip-innovations>/ {
	N
	N
	N
	s/<voip-innovations>.*<login>.*<\/login>.*<password>.*<\/password>.*<endpoint>.*<\/endpoint>/<voip-innovations>\n\t\t<login>$vi_login<\/login>\n\t\t<password>$vi_password<\/password>\n\t\t<endpoint>$vi_endpoint<\/endpoint>/
}" -e "/<fax-service class=\"org.mobicents.servlet.restcomm.fax.InterfaxService\">/ {
	N
	N
	s/<fax-service class=\"org.mobicents.servlet.restcomm.fax.InterfaxService\">.*<user>.*<\/user>.*<password>.*<\/password>/<fax-service class=\"org.mobicents.servlet.restcomm.fax.InterfaxService\">\n\t\t<user>$interfax_user<\/user>\n\t\t<password>$interfax_password<\/password>/
}" -e "/<speech-recognizer class=\"org.mobicents.servlet.restcomm.asr.ISpeechAsr\">/ {
	N
	s/<speech-recognizer class=\"org.mobicents.servlet.restcomm.asr.ISpeechAsr\">.*<api-key production=\".*\">.*<\/api-key>/<speech-recognizer class=\"org.mobicents.servlet.restcomm.asr.ISpeechAsr\">\n\t\t<api-key production=\"true\">$ispeech_key<\/api-key>/
}" -e "/<speech-synthesizer class=\"org.mobicents.servlet.restcomm.tts.AcapelaSpeechSynthesizer\">/ {
	N
	N
	N
	N
	s/<speech-synthesizer class=\"org.mobicents.servlet.restcomm.tts.AcapelaSpeechSynthesizer\">.*<service-root>http:\/\/vaas.acapela-group.com\/Services\/Synthesizer<\/service-root>.*<application>.*<\/application>.*<login>.*<\/login>.*<password>.*<\/password>/<speech-synthesizer class=\"org.mobicents.servlet.restcomm.tts.AcapelaSpeechSynthesizer\">\n\t\t<service-root>http:\/\/vaas.acapela-group.com\/Services\/Synthesizer<\/service-root>\n\t\t<application>$acapela_application<\/application>\n\t\t<login>$acapela_login<\/login>\n\t\t<password>$acapela_password<\/password>/
}" < $restcomm_config_file.bak > $restcomm_config_file

EOF

# Create script to configure MMS, AS and RestComm
cat > $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/start-restcomm.sh << 'EOF'
#! /bin/sh
# Description: Starts TelScale RestComm processes       #
# Input Params:                                         #
#               1.The install dir of TelScale RestComm  #
#               2.The local IP address                  #
# Authors: Thomas Quintana thomas.quintana@telestax.com #
#          Henrique Rosa   henrique.rosa@telestax.com   #
#          Ivelin Ivanov   ivelin.ivanov@telestax.com   #

echo Starting Restcomm from $JBOSS_HOME
source $JBOSS_HOME/autoconfigure.sh
$JBOSS_HOME/telscale-media/telscale-media-server/bin/run.sh &
$JBOSS_HOME/bin/run.sh -b $IP_ADDRESS &

EOF

# TODO: Restrict user access to Restcomm binary
chmod +x $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/autoconfigure.sh
chmod +x $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/autoconfigure-user-data.sh
chmod +x $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/start-restcomm.sh
chmod +x $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/bin/run.sh
chmod +x $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/bin/shutdown.sh
chmod 766 $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/bin/run.conf
chmod 777 $AMI_MOUNTPOINT$AMI_INSTALL_MMS/bin/run.sh

chmod 766 $AMI_MOUNTPOINT$AMI_INSTALL_MMS/deploy/server-beans.xml
find  $AMI_MOUNTPOINT$AMI_INSTALL_MMS/conf -type f -exec chmod 766 {} ';'
chmod 766 $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/conf/jboss-log4j.xml
chmod 766 $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/jbossweb.sar/server.xml
find  $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/WEB-INF/conf -type f -exec chmod 766 {} ';'
chmod 777 $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/demos
chmod +w $AMI_MOUNTPOINT$AMI_INSTALL_RESTCOMM/server/default/deploy/restcomm.war/demos/hello-world.xml

###############
# SHELL LOGIN #
###############
# Create file .bashrc in root home
cat > $AMI_MOUNTPOINT/root/.bashrc << 'EOF'
# .bashrc

# User specific aliases and functions
alias rm='rm -i'
alias cp='cp -i'
alias mv='mv -i'

# Source global definitions
if [ -f /etc/bashrc ]; then
        . /etc/bashrc
fi
EOF

# Create file .bash_profile in root home
cat > $AMI_MOUNTPOINT/root/.bash_profile << 'EOF'
# .bash_profile

# Get the aliases and functions
if [ -f ~/.bashrc ]; then
        . ~/.bashrc
fi

# User specific environment and startup programs
PATH=$PATH:$HOME/bin
export PATH
EOF

###########
# NETWORK #
###########
AMI_SYSCONFIG=$AMI_MOUNTPOINT/etc/sysconfig
AMI_IFCONFIG=$AMI_SYSCONFIG/network-scripts/ifcfg-eth0
AMI_NETWORK=$AMI_SYSCONFIG/network

# Configure Ethernet Interface
cat > $AMI_IFCONFIG << 'EOF'
DEVICE="eth0"
NM_CONTROLLED="yes"
ONBOOT=yes
TYPE=Ethernet
BOOTPROTO=dhcp
DEFROUTE=yes
PEERDNS=yes
PEERROUTES=yes
IPV4_FAILURE_FATAL=yes
IPV6INIT=no
EOF

# Configure network
cat > $AMI_NETWORK << 'EOF'
NETWORKING=yes
HOSTNAME=localhost.localdomain
EOF

# Guarantee networking starts on boot
chroot $AMI_MOUNTPOINT /sbin/chkconfig --level 2345 network on

############
# SE LINUX #
############
# Disable SELinux
cat > $AMI_MOUNTPOINT/etc/selinux/config << 'EOF'
# This file controls the state of SELinux on the system.
# SELINUX= can take one of these three values:
#       enforcing - SELinux security policy is enforced.
#       permissive - SELinux prints warnings instead of enforcing.
#       disabled - No SELinux policy is loaded.
SELINUX=disabled
# SELINUXTYPE= can take one of these two values:
#       targeted - Targeted processes are protected,
#       mls - Multi Level Security protection.
SELINUXTYPE=targeted
EOF

########
# BOOT #
########
AMI_GRUB=$AMI_MOUNTPOINT/boot/grub/grub.conf

# Create a grub configuration file for the image and boot settings so the Amazon Kernel Image (AKI) can boot into the new kernel
cat > $AMI_GRUB << 'EOF'
default=0
timeout=0
title CentOS 6.4 (TeleStax Custom AMI)
root (hd0)
kernel /boot/vmlinuz ro root=/dev/xvde1 rd_NO_PLYMOUTH
initrd /boot/initramfs
EOF

unalias ls
ln -s /boot/grub/grub.conf $AMI_MOUNTPOINT/boot/grub/menu.lst
kern=`ls $AMI_MOUNTPOINT/boot/vmlin*|awk -F/ '{print $NF}'`
ird=`ls $AMI_MOUNTPOINT/boot/initramfs*.img|awk -F/ '{print $NF}'`
sed -ie "s/vmlinuz/$kern/" $AMI_GRUB
sed -ie "s/initramfs/$ird/" $AMI_GRUB

# Autoconfigure the instance on boot and start RestComm & MMS.
echo 'export JBOSS_HOME="/opt/TelScale-restcomm-6.1.0.GA"' >> $AMI_MOUNTPOINT/etc/rc.d/rc.local
echo '$JBOSS_HOME/start-restcomm.sh' >> $AMI_MOUNTPOINT/etc/rc.d/rc.local

#########
# USERS #
#########
AMI_SUDOERS=$AMI_MOUNTPOINT/etc/sudoers.d

# Create telestax and customer users.
chroot $AMI_MOUNTPOINT useradd -c 'TeleStax Administrator' telestax
chroot $AMI_MOUNTPOINT useradd -c 'TeleStax Customer' customer

cat > $AMI_SUDOERS/telestax << EOF
# Enumerate commands for restcomm
Cmnd_Alias CMD_RESTCOMM=/bin/kill, $AMI_INSTALL_RESTCOMM/autoconfigure.sh, $AMI_INSTALL_RESTCOMM/bin/run.sh, $AMI_INSTALL_RESTCOMM/bin/shutdown.sh, $AMI_INSTALL_MMS/bin/run.sh

# Grant privileges to users:
# Telestax has full admin privileges.
# Customer has limited access to RestComm only 
telestax ALL=(root) NOPASSWD:ALL
customer ALL=(root) NOPASSWD:CMD_RESTCOMM
EOF

chown root:root $AMI_SUDOERS/telestax
chmod 0440 $AMI_SUDOERS/telestax

################
# SSH SECURITY #
################
AMI_SSH_CONFIG=$AMI_MOUNTPOINT/etc/ssh/sshd_config

# Configure ssh key for telestax user
AMI_USER_TELESTAX_DIR=$AMI_USERS_DIR/telestax/

mkdir $AMI_MOUNTPOINT/home/telestax/.ssh
chmod 700 $AMI_MOUNTPOINT/home/telestax/.ssh
chroot $AMI_MOUNTPOINT chown telestax:telestax /home/telestax/.ssh

# Create ssh public key for telestax user
# Telestax user must authenticate with restcomm.pem
cat > $AMI_MOUNTPOINT/home/telestax/.ssh/authorized_keys << 'EOF'
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDLU0XCaCOaE5iRTHEVhCbevHF62NUIK/PhWSJLL0cyzJpKkYpxoKiEqJD1FNeVKiEwq769awD51pISbB9zAuqLoiEJA0slAmWTIuMTnBdIrKx0fCzENCdlZkZC2Gh8THwdU91dZ7AX7JaP/VS1UbWppZqq9wC7b4DSNcz/PlLBsuQgdNfED9xXoUIVlCPm3fGNGQo9I1Gi7Y63HTPTi7dSprEHluuxYozBhsXD4DLmfnawAvW3eqosWzi7AxBf6L/gNML1UFi9ibc0qBMYtOeK0T+OuZEnif8cO35jiPjAo+CTBA3/plWhPSuC5xRoQnY9mWJ55HPqc9Fnv+AbNTSl restcomm
EOF

chmod 600 $AMI_MOUNTPOINT/home/telestax/.ssh/authorized_keys
chroot $AMI_MOUNTPOINT chown telestax:telestax /home/telestax/.ssh/authorized_keys

# Configure ssh key for customer user
mkdir $AMI_MOUNTPOINT/home/customer/.ssh
chmod 700 $AMI_MOUNTPOINT/home/customer/.ssh
chroot $AMI_MOUNTPOINT chown customer:customer /home/customer/.ssh

# Customer ssh key will be kept blank
# The key must be provided by the customer himself and will be configured on boot time
touch $AMI_MOUNTPOINT/home/customer/.ssh/authorized_keys
chmod 600 $AMI_MOUNTPOINT/home/customer/.ssh/authorized_keys
chroot $AMI_MOUNTPOINT chown customer:customer /home/customer/.ssh/authorized_keys

# Update ssh configuration
mv $AMI_SSH_CONFIG "$AMI_SSH_CONFIG.bak"
sed -e "s/^\#UseDNS yes$/UseDNS yes/" -e "s/^\#PermitRootLogin yes$/PermitRootLogin no/" -e "s/^PasswordAuthentication yes$/PasswordAuthentication no/" "$AMI_SSH_CONFIG.bak" > $AMI_SSH_CONFIG
echo '# List of users allowed to login via SSH' >> $AMI_SSH_CONFIG
echo 'AllowUsers telestax customer' >> $AMI_SSH_CONFIG

# Create a script that captures the public key credentials for your customer login from instance metadata.
# In this example, public key 0 (in the OpenSSH key format) is fetched from instance metadata using HTTP and written to /customer/.ssh/authorized_keys in order to allow user to log in without a password using his private key.
cat > $AMI_MOUNTPOINT/etc/init.d/ec2-get-ssh << 'EOF'
#!/bin/bash
# chkconfig: 2345 95 20
# processname: ec2-get-ssh
# description: Capture AWS public key credentials for EC2 user

# Source function library
. /etc/rc.d/init.d/functions

# Source networking configuration
[ -r /etc/sysconfig/network ] && . /etc/sysconfig/network

# Replace the following environment variables for your system
export PATH=:/usr/local/bin:/usr/local/sbin:/usr/bin:/usr/sbin:/bin:/sbin
 
# Check that networking is configured
if [ "${NETWORKING}" = "no" ]; then
  echo "Networking is not configured."
  exit 1
fi
 
start() {
  if [ ! -d /home/customer/.ssh ]; then
    mkdir -p /home/customer/.ssh
    chmod 700 /home/customer/.ssh
  fi
  
  # Retrieve public key from metadata server using HTTP
  curl -f http://instance-data/latest/meta-data/public-keys/0/openssh-key > /tmp/my-public-key
  if [ $? -eq 0 ]; then
    echo "EC2: Retrieve public key from metadata server using HTTP." 
    cat /tmp/my-public-key >> /home/customer/.ssh/authorized_keys
    chmod 600 /home/customer/.ssh/authorized_keys
    rm /tmp/my-public-key
  fi
}
 
stop() {
  echo "Nothing to do here"
}
 
restart() {
  stop
  start
}
 
# See how we were called.
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    restart
    ;;
  *)
    echo $"Usage: $0 {start|stop|restart}"
    exit 1
esac
 
exit $?
EOF

# Update the runlevel information for the new system service on the image
chmod +x $AMI_MOUNTPOINT/etc/init.d/ec2-get-ssh
chroot $AMI_MOUNTPOINT /sbin/chkconfig --level 34 ec2-get-ssh on

############
# FIREWALL #
############
AMI_FIREWALL=$AMI_MOUNTPOINT/etc/init.d/firewall

# Create firewall whitelist and blacklist
# NOTICE: you should edit these files according to your configuration!
touch $AMI_MOUNTPOINT/usr/local/etc/whitelist.txt
touch $AMI_MOUNTPOINT/usr/local/etc/blacklist.txt

# Create firewall configuration script
cat > $AMI_FIREWALL << 'EOF'
#!/bin/bash
# chkconfig: 345 30 99
# description: Starts and stops iptables based firewall

# List Locations
WHITELIST=/usr/local/etc/whitelist.txt
BLACKLIST=/usr/local/etc/blacklist.txt

# Specify ports you wish to use.
ALLOWED_TCP="22 5080 5081 8080"
ALLOWED_UDP="5080 64535:65535"

# Specify where IP Tables is located
IPTABLES=/sbin/iptables

# Find Private IP
PRIVATE_IP=`/sbin/ifconfig eth0 | grep "inet addr" | awk -F: '{print $2}' | awk '{print $1}'`

##
#DO NOT EDIT BELOW THIS LINE
##
RETVAL=0

# To start the firewall
start() {
  echo "Setting up firewall rules..."
  echo 'Allowing Localhost'
  #Allow localhost.
  $IPTABLES -A INPUT -t filter -s 127.0.0.1 -j ACCEPT
  
  # Allow Private IP
  $IPTABLES -A INPUT -t filter -s $PRIVATE_IP -j ACCEPT
  
  #
  # Whitelist
  #
  for x in `grep -v ^# $WHITELIST | awk '{print $1}'`; do
    echo "Permitting $x..."
    $IPTABLES -A INPUT -t filter -s $x -j ACCEPT
  done
  
  #
  # Blacklist
  #
  for x in `grep -v ^# $BLACKLIST | awk '{print $1}'`; do
    echo "Denying $x..."
    $IPTABLES -A INPUT -t filter -s $x -j DROP
  done
  
  #
  # Permitted Ports
  #
  for port in $ALLOWED_TCP; do
    echo "Accepting port TCP $port..."
    $IPTABLES -A INPUT -t filter -p tcp --dport $port -j ACCEPT
  done
  for port in $ALLOWED_UDP; do
    echo "Accepting port UDP $port..."
    $IPTABLES -A INPUT -t filter -p udp --dport $port -j ACCEPT
  done
  $IPTABLES -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
  $IPTABLES -A INPUT -j DROP
  RETVAL=0
}
# To stop the firewall
stop() {
  echo "Removing all iptables rules..."
  /sbin/iptables -F
  /sbin/iptables -X
  /sbin/iptables -Z
  RETVAL=0
}
case $1 in
  start)
  stop
  start
  ;;
stop)
  stop
  ;;
restart)
  stop
  start
  ;;
status)
  /sbin/iptables -L
  /sbin/iptables -t nat -L
  RETVAL=0
  ;;
*)
  echo "Usage: firewall {start|stop|restart|status}"
  RETVAL=1
esac
exit $RETVAL
EOF

# Grant permissions to firewall script
chmod 755 $AMI_FIREWALL

# Configure firewall
chroot $AMI_MOUNTPOINT /sbin/chkconfig --add firewall
chroot $AMI_MOUNTPOINT /sbin/chkconfig firewall on

##################
# IMAGE CLEANING #
##################
yum --installroot=$AMI_MOUNTPOINT -y clean packages

rm -rf $AMI_MOUNTPOINT/var/cache/yum
rm -rf $AMI_MOUNTPOINT/var/lib/yum
rm -rf $AMI_MOUNTPOINT/root/.bash_history

sync; sync; sync; sync

umount $AMI_MOUNTPOINT/dev/shm
umount $AMI_MOUNTPOINT/dev/pts
umount $AMI_MOUNTPOINT/dev
umount $AMI_MOUNTPOINT/sys
umount $AMI_MOUNTPOINT/proc/sys/fs/binfmt_misc # Because of java 6 JDK install
umount $AMI_MOUNTPOINT/proc
umount $AMI_MOUNTPOINT

###########################
# BUNDLE AND REGISTER AMI #
###########################
# Bundle image in /tmp directory
mkdir -p $AMI_BUNDLE_DIR
ec2-bundle-image -k $EC2_PRIVATE_KEY -u $AWS_ACCOUNT_NUMBER -c $EC2_BASE/certificates/cert-ec2.pem -i $AMI_FILENAME -r $AMI_ARCH -d $AMI_BUNDLE_DIR --kernel $AMI_AKI

# Upload bundle to S3 bucket
ec2-upload-bundle -a $AWS_ACCESS_KEY -s $AWS_SECRET_KEY -m $AMI_MANIFEST -b $AMI_BUCKET

# Register AMI ready to be launched
IMAGE_ID=`ec2-register "$AMI_BUCKET/$AMI_FILENAME.manifest.xml" -O $AWS_ACCESS_KEY -W $AWS_SECRET_KEY -n $AMI_NAME -a $AMI_ARCH --kernel $AMI_AKI --description "$AMI_RESTCOMM_REL AMI" | grep 'IMAGE' | awk '{print $2}'`
echo "Register S3-Backed AMI $IMAGE_ID!"

##################
# SECURITY GROUP #
##################
# Fill this variable if you want to use an existing Security Group
SECGROUP_ID='sg-02832e69'

# If no SG was identified, create a new one with default rules
if [ -z $SECGROUP_ID ]; then
	# Create the security group for the AMI
	SECGROUP_ID=`ec2-create-group $AMI_SECURITY_GROUP -d "Security Group for $AMI_RESTCOMM_REL" | grep 'GROUP' | awk '{print $2}'`
	echo "Created Security Group $SECGROUP_ID - $AMI_SECURITY_GROUP"

	# Authorize access via SSH
	ec2-authorize $AMI_SECURITY_GROUP --protocol tcp --port-range 22 --cidr 0.0.0.0/0

	# Authorize users to ping the instance
	ec2-authorize $AMI_SECURITY_GROUP --protocol icmp --icmp-type-code -1:-1 --cidr 0.0.0.0/0

	# Authorize to access the instance via HTTP
	ec2-authorize $AMI_SECURITY_GROUP --protocol tcp --port-range 80 --cidr 0.0.0.0/0

	# Authorize to access the Application Server Console via HTTP
	ec2-authorize $AMI_SECURITY_GROUP --protocol tcp --port-range 8080 --cidr 0.0.0.0/0

	# Authorize to access the instance via HTTPS
	ec2-authorize $AMI_SECURITY_GROUP --protocol tcp --port-range 443 --cidr 0.0.0.0/0

	# Authorize to access to port 5080 UDP
	ec2-authorize $AMI_SECURITY_GROUP --protocol udp --port-range 5080 --cidr 0.0.0.0/0

	# Unblock ports 64535-65535 to be used by MMS
	ec2-authorize $AMI_SECURITY_GROUP --protocol udp --port-range 64535-65535 --cidr 0.0.0.0/0
fi
#######################
# LAUNCH THE INSTANCE #
#######################
INSTANCE_ID=`ec2-run-instances $IMAGE_ID --instance-type $AMI_TYPE --group $SECGROUP_ID | grep -w 'INSTANCE' | awk '{print $2}'`
echo "Launched instance $INSTANCE_ID"

# Proceed only when instance is running
INSTANCE_STATE=''
until [ "$INSTANCE_STATE" = "running" ]; do
	echo 'Waiting for instance to startup...'
	sleep 30s
	INSTANCE_STATE=`ec2-describe-instance-status $INSTANCE_ID | grep -w 'INSTANCE' | awk '{print $4}'`
done
echo 'Instance is up and running!'

# Get useful info from running instance
PUBLIC_DNS=`ec2-describe-instances $INSTANCE_ID | grep -w 'INSTANCE' | awk '{print $4}'`
INSTANCE_REGION=`ec2-describe-instance-status $INSTANCE_ID | grep -w 'INSTANCE' | awk '{print $3}'`

####################################
# ATTACH EBS VOLUME TO S3 INSTANCE #
####################################
# Create a new EBS Volume
# Note that the availability zone must be the same of the running instance
VOLUME_ID=`ec2-create-volume --size 10 --availability-zone $INSTANCE_REGION | grep -w 'VOLUME' | awk '{print $2}'`
echo "Created EBS Volume $VOLUME_ID"

# Wait for volume to become available
VOLUME_STATE=''
until [ "$VOLUME_STATE" = "available" ]; do
	echo 'Waiting for volume to become available...'
	sleep 15s
	VOLUME_STATE=`ec2-describe-volumes $VOLUME_ID | grep -w 'VOLUME' | awk '{print $5}'`
done
echo 'Volume is available!'

# Attach the volume to the instance
ec2-attach-volume $VOLUME_ID --instance $INSTANCE_ID --device /dev/sdf

# Wait for the volume to attach to the instance
VOLUME_STATE=''
until [ "$VOLUME_STATE" = "attached" ]; do
	echo 'Waiting for volume to attach to instance...'
	sleep 15s
	VOLUME_STATE=`ec2-describe-volumes $VOLUME_ID | grep -w 'ATTACHMENT' | awk '{print $5}'`
done
echo 'Volume is attached!'

#####################################
# CONVERT S3 INSTANCE TO EBS-BACKED #
#####################################
# Create local script to move the contents of root partition to EBS volume
cat > $AMI_DOWNLOADS/s3-to-ebs.sh << 'EOF'
# Create an ext3 filesystem type on the partitionless EBS volume
/bin/egrep '[xvsh]d[a-z].*$' /proc/partitions
mkfs.ext3 /dev/xvdj

# Create a mount point directory and mount the EBS volume
mkdir -p /opt/ec2/mnt
mount -t ext3 /dev/xvdj /opt/ec2/mnt

# Remove any local instance storage entries from /etc/fstab
# Booting from an EBS volume does not use local instance storage by default
cat /etc/fstab | grep -v mnt > /tmp/fstab
mv /etc/fstab /etc/fstab.bak
mv /tmp/fstab /etc/fstab

# Sync the root and dev file systems to the EBS volume
rsync -avHx / /opt/ec2/mnt
rsync -avHx /dev /opt/ec2/mnt

# Label the disk
tune2fs -L '/' /dev/xvdj

# Flush all writes and unmount the volume
sync;sync;sync;sync
umount /opt/ec2/mnt
EOF

# Transfer executable file to instance so it can be run remotely with sudo
chmod +x $AMI_DOWNLOADS/s3-to-ebs.sh
chmod 400 $AMI_DEPENDENCIES/restcomm.pem
scp -i $AMI_DEPENDENCIES/restcomm.pem $AMI_DOWNLOADS/s3-to-ebs.sh telestax@$PUBLIC_DNS:/tmp
ssh -i $AMI_DEPENDENCIES/restcomm.pem -t -oStrictHostKeyChecking=no telestax@$PUBLIC_DNS sudo /tmp/s3-to-ebs.sh
echo "S3 to EBS conversion is finished!"

# Detach the volume
ec2-detach-volume $VOLUME_ID --instance $INSTANCE_ID
echo "Detached volume from instance."

# Wait for volume to become available
VOLUME_STATE=''
until [ "$VOLUME_STATE" = "available" ]; do
	echo 'Waiting for volume to become available...'
	sleep 15s
	VOLUME_STATE=`ec2-describe-volumes $VOLUME_ID | grep -w 'VOLUME' | awk '{print $5}'`
done
echo 'Volume is available again!'

# Terminate S3 instance
ec2-terminate-instances $INSTANCE_ID
echo "Terminated S3-Backed instance!"

# De-register S3-Backed AMI
ec2-deregister $IMAGE_ID
echo "S3-Backed AMI is no longer registered!"

###################
# CREATE SNAPSHOT #
###################
# Create a snapshot of the EBS Volume
SNAPSHOT_ID=`ec2-create-snapshot $VOLUME_ID --description "$AMI_RESTCOMM_REL AMI for $AMI_OS $AMI_OS_REL ($AMI_ARCH)" | grep -w 'SNAPSHOT' | awk '{print $2}'`
echo "Created Snapshot $SNAPSHOT_ID from EBS volume."

# Wait for Snapshot to complete
SNAPSHOT_STATE=''
until [ "$SNAPSHOT_STATE" = "completed" ]; do
	echo 'Waiting for snapshot to complete...'
	sleep 20s
	SNAPSHOT_STATE=`ec2-describe-snapshots $SNAPSHOT_ID | grep -w 'SNAPSHOT' | awk '{print $4}'`
done
echo 'Snapshot is completed!'

# Delete unused EBS volume
ec2-delete-volume $VOLUME_ID
echo "Deleted unnecessary EBS volume"

# Register EBS-backed AMI using snapshot
ec2-register --block-device-mapping "/dev/sda1=$SNAPSHOT_ID::true" --name "$AMI_NAME" --description "$AMI_RESTCOMM_REL EBS-Backed AMI for $AMI_OS $AMI_OS_REL ($AMI_ARCH)" --architecture "$AMI_ARCH" --kernel "$AMI_AKI"
echo "EBS-Back AMI was successfully registered!"
