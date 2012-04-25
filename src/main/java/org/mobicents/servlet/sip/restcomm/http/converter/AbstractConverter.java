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

import java.math.BigDecimal;
import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class AbstractConverter implements Converter {
  
  @SuppressWarnings("rawtypes")
  @Override public abstract boolean canConvert(Class klass);

  @Override public abstract void marshal(final Object object, HierarchicalStreamWriter writer,
      MarshallingContext context);

  @Override public abstract Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context);
  
  protected void writeAccountSid(final Sid accountSid, final HierarchicalStreamWriter writer) {
    writer.startNode("AccountSid");
    writer.setValue(accountSid.toString());
    writer.endNode();
  }
  
  protected void writeApiVersion(final String apiVersion, final HierarchicalStreamWriter writer) {
    writer.startNode("ApiVersion");
    writer.setValue(apiVersion);
    writer.endNode();
  }
  
  protected void writeCallSid(final Sid callSid, final HierarchicalStreamWriter writer) {
    writer.startNode("CallSid");
    writer.setValue(callSid.toString());
    writer.endNode();
  }
  
  protected void writeDateCreated(final DateTime dateCreated, final HierarchicalStreamWriter writer) {
    writer.startNode("DateCreated");
    writer.setValue(dateCreated.toString());
    writer.endNode();
  }
  
  protected void writeDateUpdated(final DateTime dateUpdated, final HierarchicalStreamWriter writer) {
    writer.startNode("DateUpdated");
    writer.setValue(dateUpdated.toString());
    writer.endNode();
  }
  
  protected void writeDuration(final double duration, final HierarchicalStreamWriter writer) {
    writer.startNode("Duration");
    writer.setValue(Double.toString(duration));
    writer.endNode();
  }
  
  protected void writeFriendlyName(final String friendlyName, final HierarchicalStreamWriter writer) {
    writer.startNode("FriendlyName");
    writer.setValue(friendlyName);
    writer.endNode();
  }
  
  protected void writePhoneNumber(final String phoneNumber, final HierarchicalStreamWriter writer) {
    writer.startNode("PhoneNumber");
    writer.setValue(phoneNumber);
    writer.endNode();
  }
  
  protected void writePrice(final BigDecimal price, final HierarchicalStreamWriter writer) {
    writer.startNode("Price");
    writer.setValue(price.toString());
    writer.endNode();
  }
  
  protected void writeSid(final Sid sid, final HierarchicalStreamWriter writer) {
    writer.startNode("Sid");
    writer.setValue(sid.toString());
    writer.endNode();
  }
  
  protected void writeSmsFallbackUrl(final URI smsFallbackUrl, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsFallbackUrl");
    writer.setValue(smsFallbackUrl.toString());
    writer.endNode();
  }
  
  protected void writeSmsFallbackMethod(final String smsFallbackMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsFallbackMethod");
    writer.setValue(smsFallbackMethod);
    writer.endNode();
  }
  
  protected void writeSmsUrl(final URI smsUrl, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsUrl");
    writer.setValue(smsUrl.toString());
    writer.endNode();
  }
  
  protected void writeSmsMethod(final String smsMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsMethod");
    writer.setValue(smsMethod);
    writer.endNode();
  }
  
  protected void writeStatus(final String status, final HierarchicalStreamWriter writer) {
    writer.startNode("Status");
    writer.setValue(status);
    writer.endNode();
  }
  
  protected void writeStatusCallback(final URI statusCallback, final HierarchicalStreamWriter writer) {
    writer.startNode("StatusCallback");
    writer.setValue(statusCallback.toString());
    writer.endNode();
  }
  
  protected void writeStatusCallbackMethod(final String statusCallbackMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("StatusCallbackMethod");
    writer.setValue(statusCallbackMethod);
    writer.endNode();
  }
  
  protected void writeType(final String type, final HierarchicalStreamWriter writer) {
    writer.startNode("Type");
    writer.setValue(type);
    writer.endNode();
  }
  
  protected void writeUri(final URI uri, final HierarchicalStreamWriter writer) {
    writer.startNode("Uri");
    writer.setValue(uri.toString());
    writer.endNode();
  }
  
  protected void writeVoiceCallerIdLookup(final boolean voiceCallerIdLookup, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceCallerIdLookup");
    writer.setValue(Boolean.toString(voiceCallerIdLookup));
    writer.endNode();
  }
  
  protected void writeVoiceFallbackMethod(final String voiceFallbackMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceFallbackMethod");
    writer.setValue(voiceFallbackMethod);
    writer.endNode();
  }
  
  protected void writeVoiceFallbackUrl(final URI voiceFallbackUri, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceFallbackUrl");
    writer.setValue(voiceFallbackUri.toString());
    writer.endNode();
  }
  
  protected void writeVoiceMethod(final String voiceMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceMethod");
    writer.setValue(voiceMethod);
    writer.endNode();
  }
  
  protected void writeVoiceUrl(final URI voiceUrl, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceUrl");
    writer.setValue(voiceUrl.toString());
    writer.endNode();
  }
}
