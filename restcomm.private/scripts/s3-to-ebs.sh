#!/bin/bash
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

# Register AMI ready to be launched
IMAGE_ID=`ec2-register "$AMI_BUCKET/$AMI_FILENAME.manifest.xml" -O $AWS_ACCESS_KEY -W $AWS_SECRET_KEY -n $AMI_NAME -a $AMI_ARCH --kernel $AMI_AKI --description "$AMI_RESTCOMM_REL AMI" | grep 'IMAGE' | awk '{print $2}'`
echo "Registered S3-Backed AMI $IMAGE_ID!"

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
