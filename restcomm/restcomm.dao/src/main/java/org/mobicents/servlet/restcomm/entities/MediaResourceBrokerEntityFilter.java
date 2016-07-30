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
package org.mobicents.servlet.restcomm.entities;

import java.text.ParseException;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */

@Immutable
public class MediaResourceBrokerEntityFilter {

    private final Sid conferenceSid;
    private String masterMsId;
    private String masterMsBridgeEpId;
    private String masterMsCnfEpId;
    private String slaveMsId;
    private String slaveMsBridgeEpId;
    private String slaveMsCnfEpId;
    private boolean isBridgedTogether;

    public MediaResourceBrokerEntityFilter(Sid conferenceSid, String masterMsId, String masterMsBridgeEpId, String masterMsCnfEpId,
     String slaveMsId, String slaveMsBridgeEpId, String slaveMsCnfEpId, boolean isBridgedTogether) throws ParseException {
        this.conferenceSid = conferenceSid;
        this.masterMsId = masterMsId;
        this.masterMsBridgeEpId = masterMsBridgeEpId;
        this.masterMsCnfEpId = masterMsCnfEpId;
        this.slaveMsId = slaveMsId;
        this.slaveMsBridgeEpId = slaveMsBridgeEpId;
        this.slaveMsCnfEpId = slaveMsCnfEpId;
        this.isBridgedTogether = isBridgedTogether;
    }

    public Sid getConferenceSid() {
        return conferenceSid;
    }

    public String getMasterMsId() {
        return masterMsId;
    }

    public String getMasterMsBridgeEpId() {
        return masterMsBridgeEpId;
    }

    public String getMasterMsCnfEpId() {
        return masterMsCnfEpId;
    }

    public String getSlaveMsId() {
        return slaveMsId;
    }

    public String getSlaveMsBridgeEpId() {
        return slaveMsBridgeEpId;
    }

    public String getSlaveMsCnfEpId() {
        return slaveMsCnfEpId;
    }

    public boolean isBridgedTogether() {
        return isBridgedTogether;
    }
}
