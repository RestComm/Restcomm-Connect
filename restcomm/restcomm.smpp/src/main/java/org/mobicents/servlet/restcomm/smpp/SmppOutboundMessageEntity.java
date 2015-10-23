package org.mobicents.servlet.restcomm.smpp;


public class SmppOutboundMessageEntity {


    private final String smppTo;
    private final String smppFrom;
    private final String smppContent;


    public SmppOutboundMessageEntity(String smppTo, String smppFrom, String smppContent){

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
