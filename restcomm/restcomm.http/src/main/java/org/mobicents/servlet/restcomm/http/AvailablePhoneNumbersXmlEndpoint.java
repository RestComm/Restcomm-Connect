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
package org.mobicents.servlet.restcomm.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;
import javax.ws.rs.core.Response;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */
@Path("/Accounts/{accountSid}/AvailablePhoneNumbers/{IsoCountryCode}/Local")
@ThreadSafe
public final class AvailablePhoneNumbersXmlEndpoint extends AvailablePhoneNumbersEndpoint {
    public AvailablePhoneNumbersXmlEndpoint() {
        super();
    }

    @GET
    public Response getAvailablePhoneNumber(@PathParam("accountSid") final String accountSid, 
    		@PathParam("IsoCountryCode") final String isoCountryCode,
            @QueryParam("AreaCode") String areaCode,
            @QueryParam("Contains") String filterPattern,
            @QueryParam("SmsEnabled") String smsEnabled,
            @QueryParam("MmsEnabled") String mmsEnabled,
            @QueryParam("VoiceEnabled") String voiceEnabled,
            @QueryParam("FaxEnabled") String faxEnabled,
            @QueryParam("RangeSize") String rangeSize,
            @QueryParam("RangeIndex") String rangeIndex) {
        if (isoCountryCode != null && !isoCountryCode.isEmpty()) {
        	int rangeSizeInt = -1;
        	if(rangeSize != null && !rangeSize.isEmpty()) {
        		rangeSizeInt = Integer.parseInt(rangeSize);
        	}
        	int rangeIndexInt = -1;
        	if(rangeIndex != null && !rangeIndex.isEmpty()) {
        		rangeIndexInt = Integer.parseInt(rangeIndex);
        	}
        	boolean smsEnabledBool = false;
        	if(smsEnabled != null && !smsEnabled.isEmpty()) {
        		smsEnabledBool = Boolean.parseBoolean(smsEnabled);
        	}
        	boolean mmsEnabledBool = false;
        	if(mmsEnabled != null && !mmsEnabled.isEmpty()) {
        		mmsEnabledBool = Boolean.parseBoolean(mmsEnabled);
        	}
        	boolean voiceEnabledBool = false;
        	if(voiceEnabled != null && !voiceEnabled.isEmpty()) {
        		voiceEnabledBool = Boolean.parseBoolean(voiceEnabled);
        	}
        	boolean faxEnabledBool = false;
        	if(faxEnabled != null && !faxEnabled.isEmpty()) {
        		faxEnabledBool = Boolean.parseBoolean(faxEnabled);
        	}
            return getAvailablePhoneNumbers(
            		accountSid, isoCountryCode, areaCode, filterPattern, smsEnabledBool, mmsEnabledBool, voiceEnabledBool, faxEnabledBool, rangeSizeInt, rangeIndexInt, APPLICATION_XML_TYPE);
        } else {
            return status(BAD_REQUEST).build();
        }
    }
}
