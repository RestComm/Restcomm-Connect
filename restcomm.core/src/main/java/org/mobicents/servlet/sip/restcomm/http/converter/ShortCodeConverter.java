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
import org.mobicents.servlet.sip.restcomm.entities.ShortCode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ShortCodeConverter extends AbstractConverter
    implements JsonSerializer<ShortCode> {
  public ShortCodeConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return ShortCode.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final ShortCode shortCode = (ShortCode)object;
    writeSid(shortCode.getSid(), writer);
    writeDateCreated(shortCode.getDateCreated(), writer);
    writeDateUpdated(shortCode.getDateUpdated(), writer);
    writeFriendlyName(shortCode.getFriendlyName(), writer);
    writeAccountSid(shortCode.getAccountSid(), writer);
    writeShortCode(Integer.toString(shortCode.getShortCode()), writer);
    writeApiVersion(shortCode.getApiVersion(), writer);
    writeSmsUrl(shortCode.getSmsUrl(), writer);
    writeSmsMethod(shortCode.getSmsMethod(), writer);
    writeSmsFallbackUrl(shortCode.getSmsFallbackUrl(), writer);
    writeSmsFallbackMethod(shortCode.getSmsFallbackMethod(), writer);
    writeUri(shortCode.getUri(), writer);
  }
  
  @Override public JsonElement serialize(final ShortCode shortCode, final Type type,
      final JsonSerializationContext context) {
  	final JsonObject object = new JsonObject();
  	writeSid(shortCode.getSid(), object);
    writeDateCreated(shortCode.getDateCreated(), object);
    writeDateUpdated(shortCode.getDateUpdated(), object);
    writeFriendlyName(shortCode.getFriendlyName(), object);
    writeAccountSid(shortCode.getAccountSid(), object);
    writeShortCode(Integer.toString(shortCode.getShortCode()), object);
    writeApiVersion(shortCode.getApiVersion(), object);
    writeSmsUrl(shortCode.getSmsUrl(), object);
    writeSmsMethod(shortCode.getSmsMethod(), object);
    writeSmsFallbackUrl(shortCode.getSmsFallbackUrl(), object);
    writeSmsFallbackMethod(shortCode.getSmsFallbackMethod(), object);
    writeUri(shortCode.getUri(), object);
  	return object;
  }
  
  private void writeShortCode(final String shortCode, final HierarchicalStreamWriter writer) {
    writer.startNode("ShortCode");
    writer.setValue(shortCode);
    writer.endNode();
  }
  
  private void writeShortCode(final String shortCode, final JsonObject object) {
    object.addProperty("short_code", shortCode);
  }
}
