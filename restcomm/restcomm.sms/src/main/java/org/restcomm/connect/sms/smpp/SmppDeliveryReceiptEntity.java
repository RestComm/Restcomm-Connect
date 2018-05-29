package org.restcomm.connect.sms.smpp;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.SmsMessage;

public class SmppDeliveryReceiptEntity {
    private Sid daoMessageSid;
    private String messageId;
    private SmsMessage.Status status;
    private DateTime dateSent;

    public SmppDeliveryReceiptEntity(Sid daoMessageSid, String messageId, SmsMessage.Status status, DateTime dateSent) {
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

    public SmsMessage.Status getStatus() {
        return status;
    }

    public void setStatus(SmsMessage.Status status) {
        this.status = status;
    }

    public DateTime getDateSent() {
        return this.dateSent;
    }

    @Override
    public String toString() {
        return "SmppDeliveryReceiptEntity [daoMessageSid=" + daoMessageSid + ", messageId=" + messageId + ", status="
                + status + ", dateSent=" + dateSent + "]";
    }

}
