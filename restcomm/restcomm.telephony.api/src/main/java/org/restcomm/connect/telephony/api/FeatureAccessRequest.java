/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.connect.telephony.api;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.extension.api.IExtensionFeatureAccessRequest;

public class FeatureAccessRequest implements IExtensionFeatureAccessRequest {

    public enum Feature {
        OUTBOUND_VOICE("outbound-voice"), INBOUND_VOICE("inbound-voice"), OUTBOUND_SMS("outbound-sms"),
        INBOUND_SMS("inbound-sms"), ASR("asr"), OUTBOUND_USSD("outbound-ussd"), INBOUND_USSD("inbound_ussd");

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

    public Sid getAccountId () {
        return accountSid;
    }

    @Override
    public String getAccountSid () {
        return accountSid.toString();
    }

    @Override
    public boolean isAllowed () {
        return false;
    }

    @Override
    public void setAllowed (boolean allowed) {

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
