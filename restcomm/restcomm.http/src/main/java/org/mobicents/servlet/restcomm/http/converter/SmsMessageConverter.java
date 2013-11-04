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
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.SmsMessage;

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
public class SmsMessageConverter extends AbstractConverter
    implements JsonSerializer<SmsMessage> {
  public SmsMessageConverter(final Configuration configuration) {
    super(configuration);
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return SmsMessage.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final SmsMessage smsMessage = (SmsMessage)object;
    writer.startNode("SMSMessage");
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
    writePriceUnit(smsMessage.getPriceUnit(), writer);
    writeApiVersion(smsMessage.getApiVersion(), writer);
    writeUri(smsMessage.getUri(), writer);
    writer.endNode();
  }
  
  @Override public JsonElement serialize(final SmsMessage smsMessage, final Type type,
      final JsonSerializationContext context) {
  	final JsonObject object = new JsonObject();
  	writeSid(smsMessage.getSid(), object);
    writeDateCreated(smsMessage.getDateCreated(), object);
    writeDateUpdated(smsMessage.getDateUpdated(), object);
    writeDateSent(smsMessage.getDateSent(), object);
    writeAccountSid(smsMessage.getAccountSid(), object);
    writeFrom(smsMessage.getSender(), object);
    writeTo(smsMessage.getRecipient(), object);
    writeBody(smsMessage.getBody(), object);
    writeStatus(smsMessage.getStatus().toString(), object);
    writeDirection(smsMessage.getDirection().toString(), object);
    writePrice(smsMessage.getPrice(), object);
    writePriceUnit(smsMessage.getPriceUnit(), object);
    writeApiVersion(smsMessage.getApiVersion(), object);
    writeUri(smsMessage.getUri(), object);
  	return object;
  }
  
  private void writeBody(final String body, final HierarchicalStreamWriter writer) {
    writer.startNode("Body");
    if(body != null) {
      writer.setValue(body);
    }
    writer.endNode();
  }
  
  private void writeBody(final String body, final JsonObject object) {
    if(body != null) {
      object.addProperty("body", body);
    } else {
      object.add("body", JsonNull.INSTANCE);
    }
  }
  
  private void writeDateSent(final DateTime dateSent, final HierarchicalStreamWriter writer) {
    writer.startNode("DateSent");
    if(dateSent != null) {
      writer.setValue(dateSent.toString());
    }
    writer.endNode();
  }
  
  private void writeDateSent(final DateTime dateSent, final JsonObject object) {
    object.addProperty("date_sent", dateSent.toString());
  }
  
  private void writeDirection(final String direction, final HierarchicalStreamWriter writer) {
    writer.startNode("Direction");
    writer.setValue(direction);
    writer.endNode();
  }
  
  private void writeDirection(final String direction, final JsonObject object) {
    object.addProperty("direction", direction);
  }
}
