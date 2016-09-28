package org.mobicents.servlet.restcomm.sms.smpp;

import com.cloudhopper.commons.charset.Charset;

public class SmppInboundMessageEntity {


    private final String smppTo;
    private final String smppFrom;
    private final String smppContent;
    private final Charset smppEncoding;



    public SmppInboundMessageEntity(String smppTo, String smppFrom, String smppContent, Charset smppEncoding){

        this.smppTo = smppTo;
        this.smppFrom = smppFrom;
        this.smppContent = smppContent;
        this.smppEncoding = smppEncoding;

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
    public final Charset getSmppEncoding(){
        return smppEncoding;
    }

}
