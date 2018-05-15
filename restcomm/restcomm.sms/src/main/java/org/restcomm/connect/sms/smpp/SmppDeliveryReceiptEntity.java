package org.restcomm.connect.sms.smpp;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;

public class SmppDeliveryReceiptEntity {
    private Sid daoMessageSid;
    private String messageId;
    private String status;
    private DateTime dateSent;

    public SmppDeliveryReceiptEntity(Sid daoMessageSid, String messageId, String status, DateTime dateSent) {
        this.daoMessageSid = daoMessageSid;
        this.messageId = messageId;
        this.status = status;
        this.dateSent = dateSent;
    }

    public Sid getDaoMessageSid() {
        return daoMessageSid;
    }

    public void setDaoMessageSid(Sid daoMessageSid) {
        this.daoMessageSid = daoMessageSid;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getDateSent() {
        return this.dateSent;
    }
}
