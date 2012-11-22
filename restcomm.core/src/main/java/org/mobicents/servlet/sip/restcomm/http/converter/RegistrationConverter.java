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

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Registration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RegistrationConverter extends AbstractConverter implements JsonSerializer<Registration> {
  public RegistrationConverter(final Configuration configuration) {
    super(configuration);
  }

  @SuppressWarnings("rawtypes")
  @Override	public boolean canConvert(final Class klass) {
    return Registration.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Registration registration = (Registration)object;
    writer.startNode("Registration");
    writeSid(registration.getSid(), writer);
    writeDateCreated(registration.getDateCreated(), writer);
    writeDateUpdated(registration.getDateUpdated(), writer);
    writeDateExpires(registration.getDateExpires(), writer);
    writeAddressOfRecord(registration.getAddressOfRecord(), writer);
    writeDisplayName(registration.getDisplayName(), writer);
    writeUserName(registration.getUserName(), writer);
    writeTimeToLive(registration.getTimeToLive(), writer);
    writeLocation(registration.getLocation(), writer);
    writeUserAgent(registration.getUserAgent(), writer);
    writer.endNode();
  }

  @Override public JsonElement serialize(final Registration registration, final Type type, final JsonSerializationContext context) {
	final JsonObject object = new JsonObject();
	writeSid(registration.getSid(), object);
	writeDateCreated(registration.getDateCreated(), object);
	writeDateUpdated(registration.getDateUpdated(), object);
	writeDateExpires(registration.getDateExpires(), object);
	writeAddressOfRecord(registration.getAddressOfRecord(), object);
	writeDisplayName(registration.getDisplayName(), object);
	writeUserName(registration.getUserName(), object);
	writeTimeToLive(registration.getTimeToLive(), object);
	writeLocation(registration.getLocation(), object);
	writeUserAgent(registration.getUserAgent(), object);
    return object;
  }
  
  private void writeDateExpires(final DateTime dateExpires, final HierarchicalStreamWriter writer) {
    writer.startNode("DateExpires");
    writer.setValue(dateExpires.toString());
    writer.endNode();
  }
  
  private void writeDateExpires(final DateTime dateExpires, final JsonObject object) {
    object.addProperty("date_expires", dateExpires.toString());
  }
  
  private void writeAddressOfRecord(final String addressOfRecord, final HierarchicalStreamWriter writer) {
    writer.startNode("AddressOfRecord");
    writer.setValue(addressOfRecord);
    writer.endNode();
  }
  
  private void writeAddressOfRecord(final String addressOfRecord, final JsonObject object) {
    object.addProperty("address_of_record", addressOfRecord.toString());
  }
  
  private void writeDisplayName(final String displayName, final HierarchicalStreamWriter writer) {
    writer.startNode("DisplayName");
    writer.setValue(displayName);
    writer.endNode();
  }
  
  private void writeDisplayName(final String displayName, final JsonObject object) {
    object.addProperty("display_name", displayName.toString());
  }
  
  private void writeLocation(final String location, final HierarchicalStreamWriter writer) {
    writer.startNode("Location");
    writer.setValue(location);
    writer.endNode();
  }
  
  private void writeLocation(final String location, final JsonObject object) {
    object.addProperty("location", location.toString());
  }
  
  private void writeUserAgent(final String userAgent, final HierarchicalStreamWriter writer) {
    writer.startNode("UserAgent");
    writer.setValue(userAgent);
    writer.endNode();
  }
  
  private void writeUserAgent(final String userAgent, final JsonObject object) {
    object.addProperty("user_agent", userAgent.toString());
  }
}
