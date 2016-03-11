package org.mobicents.servlet.restcomm.sms.smpp;


public class SmppInboundMessageEntity {


    private final String smppTo;
    private final String smppFrom;
    private final String smppContent;



    public SmppInboundMessageEntity(String smppTo, String smppFrom, String smppContent){

        this.smppTo = smppTo;
        this.smppFrom = smppFrom;
        this.smppContent = smppContent;


    }



    public final String getSmppTo(){
        return smppTo;
    }

    public final String getSmppFrom(){
        return smppFrom;
    }
    public final String getSmppContent(){
        return smppContent;
    }

}
