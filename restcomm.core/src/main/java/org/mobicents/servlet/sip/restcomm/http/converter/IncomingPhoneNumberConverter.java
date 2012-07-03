/*
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
package org.mobicents.servlet.sip.restcomm.http.converter;

import java.lang.reflect.Type;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.IncomingPhoneNumber;

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
@ThreadSafe public final class IncomingPhoneNumberConverter extends AbstractConverter
    implements JsonSerializer<IncomingPhoneNumber> {
  public IncomingPhoneNumberConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return IncomingPhoneNumber.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final IncomingPhoneNumber incomingPhoneNumber = (IncomingPhoneNumber)object;
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
    writeApiVersion(incomingPhoneNumber.getApiVersion(), writer);
    writeUri(incomingPhoneNumber.getUri(), writer);
    writer.endNode();
  }
  
  @Override public JsonElement serialize(final IncomingPhoneNumber incomingPhoneNumber,
      final Type type, final JsonSerializationContext context) {
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
    writeApiVersion(incomingPhoneNumber.getApiVersion(), object);
    writeUri(incomingPhoneNumber.getUri(), object);
    return object;
  }
  
  private void writeSmsApplicationSid(final Sid smsApplicationSid, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsApplicationSid");
    if(smsApplicationSid != null) {
      writer.setValue(smsApplicationSid.toString());
    }
    writer.endNode();
  }
  
  private void writeSmsApplicationSid(final Sid smsApplicationSid, final JsonObject object) {
    if(smsApplicationSid != null) {
      object.addProperty("sms_application_sid", smsApplicationSid.toString());
    } else {
      object.add("sms_application_sid", JsonNull.INSTANCE);
    }
  }
  
  private void writeVoiceApplicationSid(final Sid voiceApplicationSid, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceApplicationSid");
    if(voiceApplicationSid != null) {
      writer.setValue(voiceApplicationSid.toString());
    }
    writer.endNode();
  }
  
  private void writeVoiceApplicationSid(final Sid voiceApplicationSid, final JsonObject object) {
    if(voiceApplicationSid != null) {
      object.addProperty("voice_application_sid", voiceApplicationSid.toString());
    } else {
      object.add("voice_application_sid", JsonNull.INSTANCE);
    }
  }
}
