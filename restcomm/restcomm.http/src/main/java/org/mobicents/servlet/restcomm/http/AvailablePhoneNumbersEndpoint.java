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

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumber;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumberList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.http.converter.AvailablePhoneNumberConverter;
import org.mobicents.servlet.restcomm.http.converter.AvailablePhoneNumberListConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.loader.ObjectFactory;
import org.mobicents.servlet.restcomm.loader.ObjectInstantiationException;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public abstract class AvailablePhoneNumbersEndpoint extends AbstractEndpoint {
    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected Configuration phoneNumberProvisioningConfiguration;
    protected Configuration telestaxProxyConfiguration;
    protected Configuration activeConfiguration;
    protected PhoneNumberProvisioningManager phoneNumberProvisioningManager;
    private XStream xstream;
    protected Gson gson;

    public AvailablePhoneNumbersEndpoint() {
        super();
    }

    @PostConstruct
    public void init() throws ObjectInstantiationException {
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        super.init(configuration.subset("runtime-settings"));
        phoneNumberProvisioningConfiguration = configuration.subset("phone-number-provisioning");
        boolean telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            telestaxProxyConfiguration = configuration.subset("runtime-settings").subset("telestax-proxy");
        }
        final String phoneNumberProvisioningManagerClass = configuration.getString("phone-number-provisioning[@class]");
        phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) new ObjectFactory(getClass().getClassLoader())
                .getObjectInstance(phoneNumberProvisioningManagerClass);
        phoneNumberProvisioningManager.init(phoneNumberProvisioningConfiguration, telestaxProxyConfiguration);

        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(new AvailablePhoneNumberConverter(activeConfiguration));
        xstream.registerConverter(new AvailablePhoneNumberListConverter(activeConfiguration));
        xstream.registerConverter(new RestCommResponseConverter(activeConfiguration));
        final GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    protected Response getAvailablePhoneNumbers(final String accountSid, final String isoCountryCode, String areaCode,
            String filterPattern, boolean smsEnabled, boolean mmsEnabled, boolean voiceEnabled, boolean faxEnabled,
            int rangeSize, int rangeIndex, final MediaType responseType) {
        final List<PhoneNumber> phoneNumbers = phoneNumberProvisioningManager.searchForNumbers(isoCountryCode, areaCode,
                filterPattern, smsEnabled, mmsEnabled, voiceEnabled, faxEnabled, rangeSize, rangeIndex);
        List<AvailablePhoneNumber> availablePhoneNumbers = toAvailablePhoneNumbers(phoneNumbers);
        if (MediaType.APPLICATION_XML_TYPE == responseType) {
            return ok(xstream.toXML(new RestCommResponse(new AvailablePhoneNumberList(availablePhoneNumbers))),
                    MediaType.APPLICATION_XML).build();
        } else if (MediaType.APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(phoneNumbers), MediaType.APPLICATION_JSON).build();
        }
        return status(INTERNAL_SERVER_ERROR).build();
        // } else {
        // return status(BAD_REQUEST).build();
        // }
    }

    // TODO refactor this to see if we can have a single object instead of copying things
    private List<AvailablePhoneNumber> toAvailablePhoneNumbers(List<PhoneNumber> phoneNumbers) {
        final List<AvailablePhoneNumber> availablePhoneNumbers = new ArrayList<AvailablePhoneNumber>();
        for (PhoneNumber phoneNumber : phoneNumbers) {
            final AvailablePhoneNumber number = new AvailablePhoneNumber(phoneNumber.getFriendlyName(),
                    phoneNumber.getPhoneNumber(), phoneNumber.getLata(), phoneNumber.getRateCenter(),
                    phoneNumber.getLatitude(), phoneNumber.getLongitude(), phoneNumber.getRegion(),
                    phoneNumber.getPostalCode(), phoneNumber.getIsoCountry(), phoneNumber.isVoiceCapable(),
                    phoneNumber.isSmsCapable(), phoneNumber.isMmsCapable(), phoneNumber.isFaxCapable());
            availablePhoneNumbers.add(number);
        }
        return availablePhoneNumbers;
    }
}
