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

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.entities.SmsMessage;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class SmsMessageConverter extends AbstractConverter {
  public SmsMessageConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return SmsMessage.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final SmsMessage smsMessage = (SmsMessage)object;
    writeSid(smsMessage.getSid(), writer);
    writeDateCreated(smsMessage.getDateCreated(), writer);
    writeDateUpdated(smsMessage.getDateUpdated(), writer);
    writeDateSent(smsMessage.getDateSent(), writer);
    writeAccountSid(smsMessage.getAccountSid(), writer);
    writeFrom(smsMessage.getSender(), writer);
    writeTo(smsMessage.getRecipient(), writer);
    writeBody(smsMessage.getBody(), writer);
    writeStatus(smsMessage.getStatus().toString(), writer);
    writeDirection(smsMessage.getDirection().toString(), writer);
    writePrice(smsMessage.getPrice(), writer);
    writeApiVersion(smsMessage.getApiVersion(), writer);
    writeUri(smsMessage.getUri(), writer);
  }
  
  private void writeBody(final String body, final HierarchicalStreamWriter writer) {
    writer.startNode("Body");
    if(body != null) {
      writer.setValue(body);
    }
    writer.endNode();
  }
  
  private void writeDateSent(final DateTime dateSent, final HierarchicalStreamWriter writer) {
    writer.startNode("DateSent");
    writer.setValue(dateSent.toString());
    writer.endNode();
  }
  
  private void writeDirection(final String direction, final HierarchicalStreamWriter writer) {
    writer.startNode("Direction");
    writer.setValue(direction);
    writer.endNode();
  }
  
  private void writeFrom(final String from, final HierarchicalStreamWriter writer) {
    writer.startNode("From");
    writer.setValue(from);
    writer.endNode();
  }
  
  private void writeTo(final String to, final HierarchicalStreamWriter writer) {
    writer.startNode("To");
    writer.setValue(to);
    writer.endNode();
  }
}
