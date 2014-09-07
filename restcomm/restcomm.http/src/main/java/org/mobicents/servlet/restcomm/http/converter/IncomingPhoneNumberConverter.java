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
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class IncomingPhoneNumberConverter extends AbstractConverter implements JsonSerializer<IncomingPhoneNumber> {
    public IncomingPhoneNumberConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return IncomingPhoneNumber.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final IncomingPhoneNumber incomingPhoneNumber = (IncomingPhoneNumber) object;
        writer.startNode("IncomingPhoneNumber");
        writeSid(incomingPhoneNumber.getSid(), writer);
        writeAccountSid(incomingPhoneNumber.getAccountSid(), writer);
        writeFriendlyName(incomingPhoneNumber.getFriendlyName(), writer);
        writePhoneNumber(incomingPhoneNumber.getPhoneNumber(), writer);
        writeVoiceUrl(incomingPhoneNumber.getVoiceUrl(), writer);
        writeVoiceMethod(incomingPhoneNumber.getVoiceMethod(), writer);
        writeVoiceFallbackUrl(incomingPhoneNumber.getVoiceFallbackUrl(), writer);
        writeVoiceFallbackMethod(incomingPhoneNumber.getVoiceFallbackMethod(), writer);
        writeStatusCallback(incomingPhoneNumber.getStatusCallback(), writer);
        writeStatusCallbackMethod(incomingPhoneNumber.getStatusCallbackMethod(), writer);
        writeVoiceCallerIdLookup(incomingPhoneNumber.hasVoiceCallerIdLookup(), writer);
        writeVoiceApplicationSid(incomingPhoneNumber.getVoiceApplicationSid(), writer);
        writeDateCreated(incomingPhoneNumber.getDateCreated(), writer);
        writeDateUpdated(incomingPhoneNumber.getDateUpdated(), writer);
        writeSmsUrl(incomingPhoneNumber.getSmsUrl(), writer);
        writeSmsMethod(incomingPhoneNumber.getSmsMethod(), writer);
        writeSmsFallbackUrl(incomingPhoneNumber.getSmsFallbackUrl(), writer);
        writeSmsFallbackMethod(incomingPhoneNumber.getSmsFallbackMethod(), writer);
        writeSmsApplicationSid(incomingPhoneNumber.getSmsApplicationSid(), writer);
        writeCapabilities(incomingPhoneNumber.isVoiceCapable(), incomingPhoneNumber.isSmsCapable(), incomingPhoneNumber.isMmsCapable(), incomingPhoneNumber.isFaxCapable(), writer);
        writeApiVersion(incomingPhoneNumber.getApiVersion(), writer);
        writeUri(incomingPhoneNumber.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final IncomingPhoneNumber incomingPhoneNumber, final Type type,
            final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(incomingPhoneNumber.getSid(), object);
        writeAccountSid(incomingPhoneNumber.getAccountSid(), object);
        writeFriendlyName(incomingPhoneNumber.getFriendlyName(), object);
        writePhoneNumber(incomingPhoneNumber.getPhoneNumber(), object);
        writeVoiceUrl(incomingPhoneNumber.getVoiceUrl(), object);
        writeVoiceMethod(incomingPhoneNumber.getVoiceMethod(), object);
        writeVoiceFallbackUrl(incomingPhoneNumber.getVoiceFallbackUrl(), object);
        writeVoiceFallbackMethod(incomingPhoneNumber.getVoiceFallbackMethod(), object);
        writeStatusCallback(incomingPhoneNumber.getStatusCallback(), object);
        writeStatusCallbackMethod(incomingPhoneNumber.getStatusCallbackMethod(), object);
        writeVoiceCallerIdLookup(incomingPhoneNumber.hasVoiceCallerIdLookup(), object);
        writeVoiceApplicationSid(incomingPhoneNumber.getVoiceApplicationSid(), object);
        writeDateCreated(incomingPhoneNumber.getDateCreated(), object);
        writeDateUpdated(incomingPhoneNumber.getDateUpdated(), object);
        writeSmsUrl(incomingPhoneNumber.getSmsUrl(), object);
        writeSmsMethod(incomingPhoneNumber.getSmsMethod(), object);
        writeSmsFallbackUrl(incomingPhoneNumber.getSmsFallbackUrl(), object);
        writeSmsFallbackMethod(incomingPhoneNumber.getSmsFallbackMethod(), object);
        writeSmsApplicationSid(incomingPhoneNumber.getSmsApplicationSid(), object);
        writeCapabilities(incomingPhoneNumber.isVoiceCapable(), incomingPhoneNumber.isSmsCapable(), incomingPhoneNumber.isMmsCapable(), incomingPhoneNumber.isFaxCapable(), object);
        writeApiVersion(incomingPhoneNumber.getApiVersion(), object);
        writeUri(incomingPhoneNumber.getUri(), object);
        return object;
    }

    private void writeSmsApplicationSid(final Sid smsApplicationSid, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsApplicationSid");
        if (smsApplicationSid != null) {
            writer.setValue(smsApplicationSid.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    private void writeSmsApplicationSid(final Sid smsApplicationSid, final JsonObject object) {
        if (smsApplicationSid != null) {
            object.addProperty("sms_application_sid", smsApplicationSid.toString());
        } else {
            object.add("sms_application_sid", JsonNull.INSTANCE);
        }
    }
}
