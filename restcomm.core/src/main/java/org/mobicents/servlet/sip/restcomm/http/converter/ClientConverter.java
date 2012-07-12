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
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class ClientConverter extends AbstractConverter
    implements JsonSerializer<Client> {
  public ClientConverter(final Configuration configuration) {
    super(configuration);
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return Client.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Client client = (Client)object;
    writer.startNode("Client");
    writeSid(client.getSid(), writer);
    writeDateCreated(client.getDateCreated(), writer);
    writeDateUpdated(client.getDateUpdated(), writer);
    writeAccountSid(client.getAccountSid(), writer);
    writeApiVersion(client.getApiVersion(), writer);
    writeFriendlyName(client.getFriendlyName(), writer);
    writeLogin(client.getLogin(), writer);
    writePassword(client.getPassword(), writer);
    writeStatus(client.getStatus().toString(), writer);
    writeUri(client.getUri(), writer);
    writer.endNode();
  }
  
  @Override public JsonElement serialize(final Client client, final Type type,
      final JsonSerializationContext context) {
    final JsonObject object = new JsonObject();
	writeSid(client.getSid(), object);
	writeDateCreated(client.getDateCreated(), object);
	writeDateUpdated(client.getDateUpdated(), object);
	writeAccountSid(client.getAccountSid(), object);
	writeApiVersion(client.getApiVersion(), object);
	writeFriendlyName(client.getFriendlyName(), object);
	writeLogin(client.getLogin(), object);
	writePassword(client.getPassword(), object);
	writeStatus(client.getStatus().toString(), object);
	writeUri(client.getUri(), object);
    return object;
  }
  
  protected void writeLogin(final String login, final HierarchicalStreamWriter writer) {
    writer.startNode("Login");
    writer.setValue(login);
    writer.endNode();
  }
  
  protected void writeLogin(final String login, final JsonObject object) {
    object.addProperty("login", login);
  }
  
  protected void writePassword(final String password, final HierarchicalStreamWriter writer) {
    writer.startNode("Password");
    writer.setValue(password);
    writer.endNode();
  }
  
  protected void writePassword(final String password, final JsonObject object) {
    object.addProperty("password", password);
  }
}
