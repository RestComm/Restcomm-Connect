package org.restcomm.connect.telephony.api;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.extension.api.IExtensionFeatureAccessRequest;

public class FeatureAccessRequest implements IExtensionFeatureAccessRequest {

    public enum Feature {
        OUTBOUND_VOICE("outbound-voice"), INBOUND_VOICE("inbound-voice"), OUTBOUND_SMS("outbound-sms"),
        INBOUND_SMS("inbound-sms"), ASR("asr"), OUTBOUND_USSD("outbound-ussd"), INBOUND_USSD("inbound_ussd"),
        SUBACCOUNTS("subaccounts");

        private final String text;

        private Feature(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private Feature feature;
    private Sid accountSid;
    private String destinationNumber;

    public FeatureAccessRequest () {}

    public FeatureAccessRequest (Feature feature, Sid accountSid) {
        this.feature = feature;
        this.accountSid = accountSid;
    }

    public FeatureAccessRequest (Feature feature, Sid accountSid, String destinationNumber) {
        this.feature = feature;
        this.accountSid = accountSid;
        this.destinationNumber = destinationNumber;
    }

    public Feature getFeature () {
        return feature;
    }

    public void setFeature (Feature feature) {
        this.feature = feature;
    }

    public Sid getAccountSid () {
        return accountSid;
    }

    public void setAccountSid (Sid accountSid) {
        this.accountSid = accountSid;
    }

    public String getDestinationNumber () {
        return destinationNumber;
    }

    public void setDestinationNumber (String destinationNumber) {
        this.destinationNumber = destinationNumber;
    }
}
