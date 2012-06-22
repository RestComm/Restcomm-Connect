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
import org.mobicents.servlet.sip.restcomm.entities.Recording;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RecordingConverter extends AbstractConverter
    implements JsonSerializer<Recording> {
  public RecordingConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return Recording.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Recording recording = (Recording)object;
    writeSid(recording.getSid(), writer);
    writeDateCreated(recording.getDateCreated(), writer);
    writeDateUpdated(recording.getDateUpdated(), writer);
    writeAccountSid(recording.getAccountSid(), writer);
    writeCallSid(recording.getCallSid(), writer);
    writeDuration(recording.getDuration(), writer);
    writeApiVersion(recording.getApiVersion(), writer);
    writeUri(recording.getUri(), writer);
  }
  
  @Override public JsonElement serialize(final Recording recording, final Type type,
      final JsonSerializationContext context) {
  	final JsonObject object = new JsonObject();
  	writeSid(recording.getSid(), object);
    writeDateCreated(recording.getDateCreated(), object);
    writeDateUpdated(recording.getDateUpdated(), object);
    writeAccountSid(recording.getAccountSid(), object);
    writeCallSid(recording.getCallSid(), object);
    writeDuration(recording.getDuration(), object);
    writeApiVersion(recording.getApiVersion(), object);
    writeUri(recording.getUri(), object);
  	return object;
  }
}
