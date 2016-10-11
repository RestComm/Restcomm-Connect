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
package org.restcomm.connect.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.commons.dao.Sid;

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
        if (incomingPhoneNumber.getVoiceApplicationSid() != null)
            writeVoiceApplicationName(incomingPhoneNumber.getVoiceApplicationName(), writer);
        writeDateCreated(incomingPhoneNumber.getDateCreated(), writer);
        writeDateUpdated(incomingPhoneNumber.getDateUpdated(), writer);
        writeSmsUrl(incomingPhoneNumber.getSmsUrl(), writer);
        writeSmsMethod(incomingPhoneNumber.getSmsMethod(), writer);
        writeSmsFallbackUrl(incomingPhoneNumber.getSmsFallbackUrl(), writer);
        writeSmsFallbackMethod(incomingPhoneNumber.getSmsFallbackMethod(), writer);
        writeSmsApplicationSid(incomingPhoneNumber.getSmsApplicationSid(), writer);
        if (incomingPhoneNumber.getSmsApplicationSid() != null)
            writeSmsApplicationName(incomingPhoneNumber.getSmsApplicationName(), writer);
        writeUssdUrl(incomingPhoneNumber.getUssdUrl(), writer);
        writeUssdMethod(incomingPhoneNumber.getUssdMethod(), writer);
        writeUssdFallbackUrl(incomingPhoneNumber.getUssdFallbackUrl(), writer);
        writeUssdFallbackMethod(incomingPhoneNumber.getUssdFallbackMethod(), writer);
        writeUssdApplicationSid(incomingPhoneNumber.getUssdApplicationSid(), writer);
        if (incomingPhoneNumber.getUssdApplicationSid() != null)
            writeUssdApplicationName(incomingPhoneNumber.getUssdApplicationName(), writer);
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
        if (incomingPhoneNumber.getVoiceApplicationSid() != null)
            writeVoiceApplicationName(incomingPhoneNumber.getVoiceApplicationName(), object);
        writeDateCreated(incomingPhoneNumber.getDateCreated(), object);
        writeDateUpdated(incomingPhoneNumber.getDateUpdated(), object);
        writeSmsUrl(incomingPhoneNumber.getSmsUrl(), object);
        writeSmsMethod(incomingPhoneNumber.getSmsMethod(), object);
        writeSmsFallbackUrl(incomingPhoneNumber.getSmsFallbackUrl(), object);
        writeSmsFallbackMethod(incomingPhoneNumber.getSmsFallbackMethod(), object);
        writeSmsApplicationSid(incomingPhoneNumber.getSmsApplicationSid(), object);
        if (incomingPhoneNumber.getSmsApplicationSid() != null)
            writeSmsApplicationName(incomingPhoneNumber.getSmsApplicationName(), object);
        writeUssdUrl(incomingPhoneNumber.getUssdUrl(), object);
        writeUssdMethod(incomingPhoneNumber.getUssdMethod(), object);
        writeUssdFallbackUrl(incomingPhoneNumber.getUssdFallbackUrl(), object);
        writeUssdFallbackMethod(incomingPhoneNumber.getUssdFallbackMethod(), object);
        writeUssdApplicationSid(incomingPhoneNumber.getUssdApplicationSid(), object);
        if (incomingPhoneNumber.getUssdApplicationSid() != null)
            writeUssdApplicationName(incomingPhoneNumber.getUssdApplicationName(), object);
        writeCapabilities(incomingPhoneNumber.isVoiceCapable(), incomingPhoneNumber.isSmsCapable(), incomingPhoneNumber.isMmsCapable(), incomingPhoneNumber.isFaxCapable(), object);
        writeApiVersion(incomingPhoneNumber.getApiVersion(), object);
        writeUri(incomingPhoneNumber.getUri(), object);
        return object;
    }

    private void writeSmsApplicationSid(final Sid smsApplicationSid, final HierarchicalStreamWriter writer) {
        if (smsApplicationSid != null) {
            writer.startNode("SmsApplicationSid");
            writer.setValue(smsApplicationSid.toString());
            writer.endNode();
        }
    }

    private void writeSmsApplicationSid(final Sid smsApplicationSid, final JsonObject object) {
        if (smsApplicationSid != null) {
            object.addProperty("sms_application_sid", smsApplicationSid.toString());
        } else {
            object.add("sms_application_sid", JsonNull.INSTANCE);
        }
    }

    private void writeUssdApplicationSid(final Sid ussdApplicationSid, final HierarchicalStreamWriter writer) {
        if (ussdApplicationSid != null) {
            writer.startNode("UssdApplicationSid");
            writer.setValue(ussdApplicationSid.toString());
            writer.endNode();
        }
    }

    private void writeUssdApplicationSid(final Sid ussdApplicationSid, final JsonObject object) {
        if (ussdApplicationSid != null) {
            object.addProperty("ussd_application_sid", ussdApplicationSid.toString());
        } else {
            object.add("ussd_application_sid", JsonNull.INSTANCE);
        }
    }

    private void writeVoiceApplicationName(final String voiceApplicationName, final JsonObject object) {
        if (voiceApplicationName != null)
            object.addProperty("voice_application_name", voiceApplicationName);
        else
            object.add("voice_application_name", JsonNull.INSTANCE);
    }

    private void writeVoiceApplicationName(final String voiceApplicationName, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceApplicationName");
        writer.setValue(voiceApplicationName);
        writer.endNode();
    }

    private void writeSmsApplicationName(final String smsApplicationName, final JsonObject object) {
        if (smsApplicationName != null)
            object.addProperty("sms_application_name", smsApplicationName);
        else
            object.add("sms_application_name", JsonNull.INSTANCE);
    }

    private void writeSmsApplicationName(final String smsApplicationName, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsApplicationName");
        writer.setValue(smsApplicationName);
        writer.endNode();
    }

    private void writeUssdApplicationName(final String ussdApplicationName, final JsonObject object) {
        if (ussdApplicationName != null)
            object.addProperty("ussd_application_name", ussdApplicationName);
        else
            object.add("ussd_application_name", JsonNull.INSTANCE);
    }

    private void writeUssdApplicationName(final String ussdApplicationName, final HierarchicalStreamWriter writer) {
        writer.startNode("UssdApplicationName");
        writer.setValue(ussdApplicationName);
        writer.endNode();
    }
}
