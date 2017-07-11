package org.restcomm.connect.sms.smpp;

import org.restcomm.smpp.parameter.TlvSet;

import com.cloudhopper.commons.charset.Charset;

public class SmppOutboundMessageEntity {

    private final String smppTo;
    private final String smppFrom;
    private final String smppContent;
    private final Charset smppEncoding;
    private final TlvSet tlvSet;

    public SmppOutboundMessageEntity(String smppTo, String smppFrom, String smppContent, Charset smppEncoding) {
        this(smppTo, smppFrom, smppContent, smppEncoding, null);
    }

    public SmppOutboundMessageEntity(String smppTo, String smppFrom, String smppContent, Charset smppEncoding, TlvSet tlvSet) {
        this.smppTo = smppTo;
        this.smppFrom = smppFrom;
        this.smppContent = smppContent;
        this.smppEncoding = smppEncoding;
        this.tlvSet = tlvSet;
    }

    public final TlvSet getTlvSet() {
        return tlvSet;
    }

    public final String getSmppTo() {
        return smppTo;
    }

    public final String getSmppFrom() {
        return smppFrom;
    }

    public final String getSmppContent() {
        return smppContent;
    }

    public final Charset getSmppEncoding() {
        return smppEncoding;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("SMPPOutboundMessage[From=").append(smppFrom).append(",To").append(smppTo).append(",Content=")
                .append(smppContent).append(",Encoding=").append(smppEncoding);
        if (tlvSet != null) {
            builder.append(",TlvSet=").append(tlvSet.toString());
        }

        return super.toString();
    }
}
