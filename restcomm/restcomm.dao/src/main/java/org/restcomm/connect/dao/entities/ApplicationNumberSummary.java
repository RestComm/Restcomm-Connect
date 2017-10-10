/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.dao.entities;

/**
 * A summary entity for IncomingPhoneNumber to be nested inside the application
 */
public class ApplicationNumberSummary {
    String sid;
    String friendlyName;
    String phoneNumber;
    String voiceApplicationSid;
    String smsApplicationSid;
    String ussdApplicationSid;
    String referApplicationSid;

    public ApplicationNumberSummary(String sid, String friendlyName, String phoneNumber, String voiceApplicationSid, String smsApplicationSid, String ussdApplicationSid, String referApplicationSid) {
        this.sid = sid;
        this.friendlyName = friendlyName;
        this.phoneNumber = phoneNumber;
        this.voiceApplicationSid = voiceApplicationSid;
        this.smsApplicationSid = smsApplicationSid;
        this.ussdApplicationSid = ussdApplicationSid;
        this.referApplicationSid = referApplicationSid;
    }

    public String getSid() {
        return sid;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getVoiceApplicationSid() {
        return voiceApplicationSid;
    }

    public String getSmsApplicationSid() {
        return smsApplicationSid;
    }

    public String getUssdApplicationSid() {
        return ussdApplicationSid;
    }

    public String getReferApplicationSid() {
        return referApplicationSid;
    }
}
