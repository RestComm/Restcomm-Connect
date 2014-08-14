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

import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.Sid;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */
public abstract class AbstractConverter implements Converter {
    protected final Configuration configuration;

    public AbstractConverter(final Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public abstract boolean canConvert(Class klass);

    @Override
    public abstract void marshal(final Object object, HierarchicalStreamWriter writer, MarshallingContext context);

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        return null;
    }

    protected void writeAccountSid(final Sid accountSid, final HierarchicalStreamWriter writer) {
        writer.startNode("AccountSid");
        writer.setValue(accountSid.toString());
        writer.endNode();
    }

    protected void writeAccountSid(final Sid accountSid, final JsonObject object) {
        object.addProperty("account_sid", accountSid.toString());
    }

    protected void writeApiVersion(final String apiVersion, final HierarchicalStreamWriter writer) {
        writer.startNode("ApiVersion");
        writer.setValue(apiVersion);
        writer.endNode();
    }

    protected void writeApiVersion(final String apiVersion, final JsonObject object) {
        object.addProperty("api_version", apiVersion);
    }

    protected void writeCallSid(final Sid callSid, final HierarchicalStreamWriter writer) {
        if (callSid != null) {
            writer.startNode("CallSid");
            writer.setValue(callSid.toString());
            writer.endNode();
        }
    }

    protected void writeCallSid(final Sid callSid, final JsonObject object) {
        if (callSid != null)
            object.addProperty("call_sid", callSid.toString());
    }

    protected void writeDateCreated(final DateTime dateCreated, final HierarchicalStreamWriter writer) {
        writer.startNode("DateCreated");
        writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateCreated.toDate()));
        writer.endNode();
    }

    protected void writeDateCreated(final DateTime dateCreated, final JsonObject object) {
        object.addProperty("date_created", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateCreated.toDate()));
    }

    protected void writeDateUpdated(final DateTime dateUpdated, final HierarchicalStreamWriter writer) {
        writer.startNode("DateUpdated");
        writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateUpdated.toDate()));
        writer.endNode();
    }

    protected void writeDateUpdated(final DateTime dateUpdated, final JsonObject object) {
        object.addProperty("date_updated", new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateUpdated.toDate()));
    }

    protected void writeDuration(final double duration, final HierarchicalStreamWriter writer) {
        writer.startNode("Duration");
        writer.setValue(Double.toString(duration));
        writer.endNode();
    }

    protected void writeDuration(final double duration, final JsonObject object) {
        object.addProperty("duration", Double.toString(duration));
    }

    protected void writeFriendlyName(final String friendlyName, final HierarchicalStreamWriter writer) {
        writer.startNode("FriendlyName");
        writer.setValue(friendlyName);
        writer.endNode();
    }

    protected void writeFriendlyName(final String friendlyName, final JsonObject object) {
        object.addProperty("friendly_name", friendlyName);
    }

    protected void writeFrom(final String from, final HierarchicalStreamWriter writer) {
        writer.startNode("From");
        writer.setValue(from);
        writer.endNode();
    }

    protected void writeFrom(final String from, final JsonObject object) {
        object.addProperty("from", from);
    }

    protected void writePhoneNumber(final String phoneNumber, final HierarchicalStreamWriter writer) {
        writer.startNode("PhoneNumber");
        writer.setValue(phoneNumber);
        writer.endNode();
    }

    protected void writePhoneNumber(final String phoneNumber, final JsonObject object) {
        object.addProperty("phone_number", phoneNumber);
    }

    protected void writePrice(final BigDecimal price, final HierarchicalStreamWriter writer) {
        writer.startNode("Price");
        writer.setValue(price.toString());
        writer.endNode();
    }

    protected void writePrice(final BigDecimal price, final JsonObject object) {
        object.addProperty("price", price.toString());
    }

    protected void writePriceUnit(final Currency priceUnit, final HierarchicalStreamWriter writer) {
        writer.startNode("PriceUnit");
        if (priceUnit != null) {
            writer.setValue(priceUnit.toString());
        }
        writer.endNode();
    }

    protected void writePriceUnit(final Currency priceUnit, final JsonObject object) {
        if (priceUnit != null) {
            object.addProperty("price_unit", priceUnit.toString());
        } else {
            object.add("price_unit", JsonNull.INSTANCE);
        }
    }

    protected void writeSid(final Sid sid, final HierarchicalStreamWriter writer) {
        writer.startNode("Sid");
        writer.setValue(sid.toString());
        writer.endNode();
    }

    protected void writeSid(final Sid sid, final JsonObject object) {
        object.addProperty("sid", sid.toString());
    }

    protected void writeSmsFallbackUrl(final URI smsFallbackUrl, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsFallbackUrl");
        if (smsFallbackUrl != null) {
            writer.setValue(smsFallbackUrl.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeSmsFallbackUrl(final URI smsFallbackUrl, final JsonObject object) {
        if (smsFallbackUrl != null) {
            object.addProperty("sms_fallback_url", smsFallbackUrl.toString());
        } else {
            object.add("sms_fallback_url", JsonNull.INSTANCE);
        }
    }

    protected void writeSmsFallbackMethod(final String smsFallbackMethod, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsFallbackMethod");
        if (smsFallbackMethod != null) {
            writer.setValue(smsFallbackMethod);
        }
        writer.endNode();
    }

    protected void writeSmsFallbackMethod(final String smsFallbackMethod, final JsonObject object) {
        if (smsFallbackMethod != null) {
            object.addProperty("sms_fallback_method", smsFallbackMethod);
        } else {
            object.add("sms_fallback_method", JsonNull.INSTANCE);
        }
    }

    protected void writeSmsUrl(final URI smsUrl, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsUrl");
        if (smsUrl != null) {
            writer.setValue(smsUrl.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeSmsUrl(final URI smsUrl, final JsonObject object) {
        if (smsUrl != null) {
            object.addProperty("sms_url", smsUrl.toString());
        } else {
            object.add("sms_url", JsonNull.INSTANCE);
        }
    }

    protected void writeSmsMethod(final String smsMethod, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsMethod");
        if (smsMethod != null) {
            writer.setValue(smsMethod);
        }
        writer.endNode();
    }

    protected void writeSmsMethod(final String smsMethod, final JsonObject object) {
        if (smsMethod != null) {
            object.addProperty("sms_method", smsMethod);
        } else {
            object.add("sms_method", JsonNull.INSTANCE);
        }
    }

    protected void writeStatus(final String status, final HierarchicalStreamWriter writer) {
        writer.startNode("Status");
        writer.setValue(status);
        writer.endNode();
    }

    protected void writeStatus(final String status, final JsonObject object) {
        object.addProperty("status", status);
    }

    protected void writeStatusCallback(final URI statusCallback, final HierarchicalStreamWriter writer) {
        writer.startNode("StatusCallback");
        if (statusCallback != null) {
            writer.setValue(statusCallback.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeStatusCallback(final URI statusCallback, final JsonObject object) {
        if (statusCallback != null) {
            object.addProperty("status_callback", statusCallback.toString());
        } else {
            object.add("status_callback", JsonNull.INSTANCE);
        }
    }

    protected void writeStatusCallbackMethod(final String statusCallbackMethod, final HierarchicalStreamWriter writer) {
        writer.startNode("StatusCallbackMethod");
        if (statusCallbackMethod != null) {
            writer.setValue(statusCallbackMethod);
        }
        writer.endNode();
    }

    protected void writeStatusCallbackMethod(final String statusCallbackMethod, final JsonObject object) {
        if (statusCallbackMethod != null) {
            object.addProperty("status_callback_method", statusCallbackMethod);
        } else {
            object.add("status_callback_method", JsonNull.INSTANCE);
        }
    }

    protected void writeTo(final String to, final HierarchicalStreamWriter writer) {
        writer.startNode("To");
        writer.setValue(to);
        writer.endNode();
    }

    protected void writeTo(final String to, final JsonObject object) {
        object.addProperty("to", to);
    }

    protected void writeTimeToLive(final int timeToLive, final HierarchicalStreamWriter writer) {
        writer.startNode("TimeToLive");
        writer.setValue(Integer.toString(timeToLive));
        writer.endNode();
    }

    protected void writeTimeToLive(final int timeToLive, final JsonObject object) {
        object.addProperty("time_to_live", timeToLive);
    }

    protected void writeType(final String type, final HierarchicalStreamWriter writer) {
        writer.startNode("Type");
        writer.setValue(type);
        writer.endNode();
    }

    protected void writeType(final String type, final JsonObject object) {
        object.addProperty("type", type);
    }

    protected void writeUri(final URI uri, final HierarchicalStreamWriter writer) {
        writer.startNode("Uri");
        writer.setValue(uri.toString());
        writer.endNode();
    }

    protected void writeUri(final URI uri, final JsonObject object) {
        object.addProperty("uri", uri.toString() + ".json");
    }

    protected void writeUserName(final String userName, final HierarchicalStreamWriter writer) {
        writer.startNode("UserName");
        writer.setValue(userName);
        writer.endNode();
    }

    protected void writeUserName(final String userName, final JsonObject object) {
        object.addProperty("user_name", userName);
    }

    protected void writeVoiceApplicationSid(final Sid voiceApplicationSid, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceApplicationSid");
        if (voiceApplicationSid != null) {
            writer.setValue(voiceApplicationSid.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeVoiceApplicationSid(final Sid voiceApplicationSid, final JsonObject object) {
        if (voiceApplicationSid != null) {
            object.addProperty("voice_application_sid", voiceApplicationSid.toString());
        } else {
            object.add("voice_application_sid", JsonNull.INSTANCE);
        }
    }

    protected void writeVoiceCallerIdLookup(final boolean voiceCallerIdLookup, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceCallerIdLookup");
        writer.setValue(Boolean.toString(voiceCallerIdLookup));
        writer.endNode();
    }

    protected void writeVoiceCallerIdLookup(final boolean voiceCallerIdLookup, final JsonObject object) {
        object.addProperty("voice_caller_id_lookup", voiceCallerIdLookup);
    }

    protected void writeVoiceFallbackMethod(final String voiceFallbackMethod, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceFallbackMethod");
        if (voiceFallbackMethod != null) {
            writer.setValue(voiceFallbackMethod);
        }
        writer.endNode();
    }

    protected void writeVoiceFallbackMethod(final String voiceFallbackMethod, final JsonObject object) {
        if (voiceFallbackMethod != null) {
            object.addProperty("voice_fallback_method", voiceFallbackMethod);
        } else {
            object.add("voice_fallback_method", JsonNull.INSTANCE);
        }
    }

    protected void writeVoiceFallbackUrl(final URI voiceFallbackUri, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceFallbackUrl");
        if (voiceFallbackUri != null) {
            writer.setValue(voiceFallbackUri.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeVoiceFallbackUrl(final URI voiceFallbackUri, final JsonObject object) {
        if (voiceFallbackUri != null) {
            object.addProperty("voice_fallback_url", voiceFallbackUri.toString());
        } else {
            object.add("voice_fallback_url", JsonNull.INSTANCE);
        }
    }

    protected void writeVoiceMethod(final String voiceMethod, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceMethod");
        if (voiceMethod != null) {
            writer.setValue(voiceMethod);
        }
        writer.endNode();
    }

    protected void writeVoiceMethod(final String voiceMethod, final JsonObject object) {
        if (voiceMethod != null) {
            object.addProperty("voice_method", voiceMethod);
        } else {
            object.add("voice_method", JsonNull.INSTANCE);
        }
    }

    protected void writeVoiceUrl(final URI voiceUrl, final HierarchicalStreamWriter writer) {
        writer.startNode("VoiceUrl");
        if (voiceUrl != null) {
            writer.setValue(voiceUrl.toString());
        } else {
            writer.setValue(null);
        }
        writer.endNode();
    }

    protected void writeVoiceUrl(final URI voiceUrl, final JsonObject object) {
        if (voiceUrl != null) {
            object.addProperty("voice_url", voiceUrl.toString());
        } else {
            object.add("voice_url", JsonNull.INSTANCE);
        }
    }

    protected void writeCapabilities(final Boolean voiceCapable, final Boolean smsCapable, final Boolean mmsCapable,
            final Boolean faxCapable, final HierarchicalStreamWriter writer) {
        writer.startNode("Capabilities");
        writeVoiceCapability(voiceCapable, writer);
        writeSmsCapability(smsCapable, writer);
        writeMmsCapability(mmsCapable, writer);
        writeFaxCapability(faxCapable, writer);
        writer.endNode();
    }

    protected void writeCapabilities(final Boolean voiceCapable, final Boolean smsCapable, final Boolean mmsCapable,
            final Boolean faxCapable, final JsonObject object) {
        JsonObject capabilities = new JsonObject();
        writeVoiceCapability(voiceCapable, capabilities);
        writeSmsCapability(smsCapable, capabilities);
        writeMmsCapability(mmsCapable, capabilities);
        writeFaxCapability(faxCapable, capabilities);
        object.add("capabilities", capabilities);
    }

    protected void writeVoiceCapability(final Boolean voiceCapable, final HierarchicalStreamWriter writer) {
        writer.startNode("Voice");
        if (voiceCapable == null) {
            writer.setValue("false");
        } else {
            writer.setValue(voiceCapable.toString());
        }
        writer.endNode();
    }

    protected void writeVoiceCapability(final Boolean voiceCapable, final JsonObject object) {
        if (voiceCapable != null) {
            object.addProperty("voice_capable", voiceCapable);
        } else {
            object.addProperty("voice_capable", "false");
        }
    }

    protected void writeSmsCapability(final Boolean smsCapable, final HierarchicalStreamWriter writer) {
        writer.startNode("Sms");
        if (smsCapable == null) {
            writer.setValue("false");
        } else {
            writer.setValue(smsCapable.toString());
        }
        writer.endNode();
    }

    protected void writeSmsCapability(final Boolean smsCapable, final JsonObject object) {
        if (smsCapable != null) {
            object.addProperty("sms_capable", smsCapable);
        } else {
            object.addProperty("sms_capable", "false");
        }
    }

    protected void writeMmsCapability(final Boolean mmsCapable, final HierarchicalStreamWriter writer) {
        writer.startNode("Mms");
        if (mmsCapable == null) {
            writer.setValue("false");
        } else {
            writer.setValue(mmsCapable.toString());
        }
        writer.endNode();
    }

    protected void writeMmsCapability(final Boolean mmsCapable, final JsonObject object) {
        if (mmsCapable != null) {
            object.addProperty("mms_capable", mmsCapable);
        } else {
            object.addProperty("mms_capable", "false");
        }
    }

    protected void writeFaxCapability(final Boolean faxCapable, final HierarchicalStreamWriter writer) {
        writer.startNode("Fax");
        if (faxCapable == null) {
            writer.setValue("false");
        } else {
            writer.setValue(faxCapable.toString());
        }
        writer.endNode();
    }

    protected void writeFaxCapability(final Boolean faxCapable, final JsonObject object) {
        if (faxCapable != null) {
            object.addProperty("fax_capable", faxCapable);
        } else {
            object.addProperty("fax_capable", "false");
        }
    }
}
