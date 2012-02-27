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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Gateway;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.GatewaysDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoGatewaysDao implements GatewaysDao {
  private static final Logger logger = Logger.getLogger(MongoGatewaysDao.class);
  private final DBCollection collection;
  
  public MongoGatewaysDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_gateways");
  }
  
  @Override public void addGateway(final Gateway gateway) {
    final WriteResult result = collection.insert(toDbObject(gateway));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public List<Gateway> getGateways() {
    final List<Gateway> gateways = new ArrayList<Gateway>();
    final DBCursor results = collection.find();
    while(results.hasNext()) {
      gateways.add(toGateway(results.next()));
    }
    return gateways;
  }

  @Override public void removeGateway(final String name) {
    final BasicDBObject query = new BasicDBObject();
    query.put("name", name);
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void updateGateway(final Gateway gateway) {
    final BasicDBObject query = new BasicDBObject();
    query.put("name", gateway.getName());
    final WriteResult result = collection.update(query, toDbObject(gateway));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private DBObject toDbObject(final Gateway gateway) {
    final BasicDBObject object = new BasicDBObject();
    object.put("name", gateway.getName());
    object.put("password", gateway.getPassword());
    object.put("proxy", gateway.getProxy());
    object.put("user", gateway.getUser());
    object.put("register", gateway.register());
    return object;
  }
  
  private Gateway toGateway(final DBObject object) {
    final String name = readString(object.get("name"));
    final String password = readString(object.get("password"));
    final String proxy = readString(object.get("proxy"));
    final String user = readString(object.get("user"));
    final Boolean register = readBoolean(object.get("register"));
    return new Gateway(name, password, proxy, register, user);
  }
}
