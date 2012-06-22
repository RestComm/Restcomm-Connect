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
import java.net.URI;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Application;

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
@ThreadSafe public final class ApplicationConverter extends AbstractConverter
    implements JsonSerializer<Application> {
  public ApplicationConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return Application.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Application application = (Application)object;
    writer.startNode("Application");
    writeSid(application.getSid(), writer);
    writeDateCreated(application.getDateCreated(), writer);
    writeDateUpdated(application.getDateUpdated(), writer);
    writeFriendlyName(application.getFriendlyName(), writer);
    writeAccountSid(application.getAccountSid(), writer);
    writeApiVersion(application.getApiVersion(), writer);
    writeVoiceUrl(application.getVoiceUrl(), writer);
    writeVoiceMethod(application.getVoiceMethod(), writer);
    writeVoiceFallbackUrl(application.getVoiceFallbackUrl(), writer);
    writeVoiceFallbackMethod(application.getVoiceFallbackMethod(), writer);
    writeStatusCallback(application.getStatusCallback(), writer);
    writeStatusCallbackMethod(application.getStatusCallbackMethod(), writer);
    writeVoiceCallerIdLookup(application.hasVoiceCallerIdLookup(), writer);
    writeSmsUrl(application.getSmsUrl(), writer);
    writeSmsMethod(application.getSmsMethod(), writer);
    writeSmsFallbackUrl(application.getSmsFallbackUrl(), writer);
    writeSmsFallbackMethod(application.getSmsFallbackMethod(), writer);
    writeSmsStatusCallback(application.getSmsStatusCallback(), writer);
    writeUri(application.getUri(), writer);
    writer.endNode();
  }
  
  @Override public JsonElement serialize(final Application application, final Type type,
      final JsonSerializationContext context) {
    final JsonObject object = new JsonObject();
    writeSid(application.getSid(), object);
    writeDateCreated(application.getDateCreated(), object);
    writeDateUpdated(application.getDateUpdated(), object);
    writeFriendlyName(application.getFriendlyName(), object);
    writeAccountSid(application.getAccountSid(), object);
    writeApiVersion(application.getApiVersion(), object);
    writeVoiceUrl(application.getVoiceUrl(), object);
    writeVoiceMethod(application.getVoiceMethod(), object);
    writeVoiceFallbackUrl(application.getVoiceFallbackUrl(), object);
    writeVoiceFallbackMethod(application.getVoiceFallbackMethod(), object);
    writeStatusCallback(application.getStatusCallback(), object);
    writeStatusCallbackMethod(application.getStatusCallbackMethod(), object);
    writeVoiceCallerIdLookup(application.hasVoiceCallerIdLookup(), object);
    writeSmsUrl(application.getSmsUrl(), object);
    writeSmsMethod(application.getSmsMethod(), object);
    writeSmsFallbackUrl(application.getSmsFallbackUrl(), object);
    writeSmsFallbackMethod(application.getSmsFallbackMethod(), object);
    writeSmsStatusCallback(application.getSmsStatusCallback(), object);
    writeUri(application.getUri(), object);
    return object;
  }
  
  private void writeSmsStatusCallback(final URI smsStatusCallback, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsStatusCallback");
    if(smsStatusCallback != null) {
      writer.setValue(smsStatusCallback.toString());
    }
    writer.endNode();
  }
  
  private void writeSmsStatusCallback(final URI smsStatusCallback, final JsonObject object) {
    if(smsStatusCallback != null) {
      object.addProperty("sms_status_callback", smsStatusCallback.toString());
    } else {
      object.add("sms_status_callback", JsonNull.INSTANCE);
    }
  }
}
