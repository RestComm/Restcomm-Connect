PREPARATIONS:

1. Import health-entry.zip to RVD
2. Import health-queue.zip to RVD
3. Edit the address of the SIP URI in the Dial section of the health-queue application.
4. Bind the health-entry application to a RestComm number (+5555)

HOW TO EXECUTE TEST:

1. Open two terminal tabs.
2. In the first tab, open the health-ivr-server.sh script and edit the LOCAL_ADDRESS variable to match the private IP address of your instance.
3. Execute the health-ivr-server.sh script. SIPp will now listen for incoming calls.
4. In the second tab, open the health-ivr-client.sh script and edit the LOCAL_ADDRESS and RESTCOMM_ADDRESS variables according to your environment.
5. Execute the health-ivr-client.sh script.