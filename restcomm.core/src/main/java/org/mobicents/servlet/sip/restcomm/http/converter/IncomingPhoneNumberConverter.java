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

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.IncomingPhoneNumber;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class IncomingPhoneNumberConverter extends AbstractConverter {
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
  }
  
  private void writeSmsApplicationSid(final Sid smsApplicationSid, final HierarchicalStreamWriter writer) {
    writer.startNode("SmsApplicationSid");
    if(smsApplicationSid != null) {
      writer.setValue(smsApplicationSid.toString());
    }
    writer.endNode();
  }
  
  private void writeVoiceApplicationSid(final Sid voiceApplicationSid, final HierarchicalStreamWriter writer) {
    writer.startNode("VoiceApplicationSid");
    if(voiceApplicationSid != null) {
      writer.setValue(voiceApplicationSid.toString());
    }
    writer.endNode();
  }
}
