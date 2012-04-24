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
package org.mobicents.servlet.sip.restcomm.dao.mongodb;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Client;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ClientsDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class MongoClientsDao implements ClientsDao {
  private static final Logger logger = Logger.getLogger(MongoClientsDao.class);
  private final DBCollection collection;
  
  public MongoClientsDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_clients");
  }
  
  @Override public void addClient(final Client client) {
    final WriteResult result = collection.insert(toDbObject(client));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public Client getClient(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
	query.put("sid", sid.toString());
	return getClient(query);
  }

  @Override public Client getClient(final String login) {
    final BasicDBObject query = new BasicDBObject();
    query.put("login", login);
    return getClient(query);
  }
  
  private Client getClient(final DBObject query) {
    final DBObject result = collection.findOne(query);
    if(result != null) {
  	  return toClient(result);
  	} else {
  	  return null;
  	}
  }

  @Override public List<Client> getClients(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    final List<Client> clients = new ArrayList<Client>();
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      clients.add(toClient(results.next()));
    }
    return clients;
  }

  @Override public void removeClient(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    removeClients(query);
  }

  @Override public void removeClients(final Sid accountSid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("account_sid", accountSid.toString());
    removeClients(query);
  }
  
  private void removeClients(final DBObject query) {
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateClient(final Client client) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", client.getSid().toString());
    final WriteResult result = collection.update(query, toDbObject(client));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private Client toClient(final DBObject object) {
    final Sid sid = readSid(object.get("sid"));
    final DateTime dateCreated = readDateTime(object.get("date_created"));
    final DateTime dateUpdated = readDateTime(object.get("date_updated"));
    final Sid accountSid = readSid(object.get("account_sid"));
    final String apiVersion = readString(object.get("api_version"));
    final String friendlyName = readString(object.get("friendly_name"));
    final String login = readString(object.get("login"));
    final String password = readString(object.get("password"));
    final int status = readInteger(object.get("status"));
    final URI uri = readUri(object.get("uri"));
    return new Client(sid, dateCreated, dateUpdated, accountSid, apiVersion,
        friendlyName, login, password, status, uri);
  }
  
  private DBObject toDbObject(final Client client) {
    final BasicDBObject object = new BasicDBObject();
    object.put("sid", writeSid(client.getSid()));
    object.put("date_created", writeDateTime(client.getDateCreated()));
    object.put("date_updated", writeDateTime(client.getDateUpdated()));
    object.put("account_sid", writeSid(client.getAccountSid()));
    object.put("api_version", client.getApiVersion());
    object.put("friendly_name", client.getFriendlyName());
    object.put("login", client.getLogin());
    object.put("password", client.getPassword());
    object.put("status", client.getStatus());
    object.put("uri", writeUri(client.getUri()));
    return object;
  }
}
