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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.HttpCookiesDao;
import static org.mobicents.servlet.sip.restcomm.dao.DaoUtils.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class MongoHttpCookiesDao implements HttpCookiesDao {
  private static final Logger logger = Logger.getLogger(MongoHttpCookiesDao.class);
  private final DBCollection collection;
  
  public MongoHttpCookiesDao(final DB database) {
    super();
    collection = database.getCollection("restcomm_http_cookies");
  }

  @Override public void addCookie(Sid sid, Cookie cookie) {
    final WriteResult result = collection.insert(toDbObject(cookie));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public List<Cookie> getCookies(final Sid sid) {
    final List<Cookie> cookies = new ArrayList<Cookie>();
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final DBCursor results = collection.find(query);
    while(results.hasNext()) {
      cookies.add(toCookie(results.next()));
    }
    return cookies;
  }
  
  @Override public boolean hasCookie(final Sid sid, final Cookie cookie) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    query.put("name", cookie.getName());
    final long result = collection.count(query);
    return result == 1;
  }

  @Override public void removeCookies(Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }

  @Override public void removeExpiredCookies(final Sid sid) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    final BasicDBObject condition = new BasicDBObject();
    condition.put("$lte", new Date());
    query.put("expiration_date", condition);
    final WriteResult result = collection.remove(query);
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  @Override public void updateCookie(final Sid sid, final Cookie cookie) {
    final BasicDBObject query = new BasicDBObject();
    query.put("sid", sid.toString());
    query.put("name", cookie.getName());
    final WriteResult result = collection.update(query, toDbObject(cookie));
    if(!result.getLastError().ok()) {
      logger.error(result.getLastError().getErrorMessage());
    }
  }
  
  private Cookie toCookie(final DBObject object) {
    final String comment = readString(object.get("comment"));
    final String domain = readString(object.get("domain"));
    final Date expirationDate = (Date)object.get("expiration_date");
    final String name = readString(object.get("name"));
    final String path = readString(object.get("path"));
    final String value = readString(object.get("value"));
    final int version = readInteger(object.get("version"));
    final BasicClientCookie cookie = new BasicClientCookie(name, value);
    cookie.setComment(comment);
    cookie.setDomain(domain);
    cookie.setExpiryDate(expirationDate);
    cookie.setPath(path);
    cookie.setVersion(version);
    return cookie;
  }
  
  private DBObject toDbObject(final Cookie cookie) {
    final BasicDBObject object = new BasicDBObject();
    object.put("comment", cookie.getComment());
    object.put("domain", cookie.getDomain());
    object.put("expiration_date", cookie.getExpiryDate());
    object.put("name", cookie.getName());
    object.put("path", cookie.getPath());
    object.put("value", cookie.getValue());
    object.put("version", cookie.getVersion());
    return object;
  }
}
