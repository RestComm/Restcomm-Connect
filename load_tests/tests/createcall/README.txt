Load test for Restcomm REST API Create call

The scripts here will do the following:
1. Will start a sipp thread that will listen for INVITE requests following the hello-play RCML application
2. Will use CURL to create and SEND REST API requests to Calls endpoint. Restcomm will create call to the sipp thread.
