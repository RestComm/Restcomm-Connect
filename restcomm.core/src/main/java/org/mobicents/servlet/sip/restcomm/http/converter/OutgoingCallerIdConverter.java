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

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.OutgoingCallerId;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class OutgoingCallerIdConverter extends AbstractConverter
    implements JsonSerializer<OutgoingCallerId> {
  public OutgoingCallerIdConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return OutgoingCallerId.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final OutgoingCallerId outgoingCallerId = (OutgoingCallerId)object;
    writer.startNode("OutgoingCallerId");
    writeSid(outgoingCallerId.getSid(), writer);
    writeAccountSid(outgoingCallerId.getAccountSid(), writer);
    writeFriendlyName(outgoingCallerId.getFriendlyName(), writer);
    writePhoneNumber(outgoingCallerId.getPhoneNumber(), writer);
    writeDateCreated(outgoingCallerId.getDateCreated(), writer);
    writeDateUpdated(outgoingCallerId.getDateUpdated(), writer);
    writeUri(outgoingCallerId.getUri(), writer);
    writer.endNode();
  }
  
  @Override public JsonElement serialize(final OutgoingCallerId outgoingCallerId, final Type type,
      final JsonSerializationContext context) {
  	final JsonObject object = new JsonObject();
  	writeSid(outgoingCallerId.getSid(), object);
    writeAccountSid(outgoingCallerId.getAccountSid(), object);
    writeFriendlyName(outgoingCallerId.getFriendlyName(), object);
    writePhoneNumber(outgoingCallerId.getPhoneNumber(), object);
    writeDateCreated(outgoingCallerId.getDateCreated(), object);
    writeDateUpdated(outgoingCallerId.getDateUpdated(), object);
    writeUri(outgoingCallerId.getUri(), object);
  	return object;
  }
}
