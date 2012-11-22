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
package org.mobicents.servlet.sip.restcomm.entities;

import java.io.Serializable;
import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Gateway implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String friendlyName;
  private final String password;
  private final String proxy;
  private final Boolean register;
  private final String userName;
  private final int timeToLive;
  private final URI uri;

  public Gateway(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
      final String password, final String proxy, final Boolean register, final String userName, final int timeToLive,
      final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.friendlyName = friendlyName;
    this.password = password;
    this.proxy = proxy;
    this.register = register;
    this.userName = userName;
    this.timeToLive = timeToLive;
    this.uri = uri;
  }
  
  public Sid getSid() {
    return sid;
  }
  
  public DateTime getDateCreated() {
    return dateCreated;
  }
  
  public DateTime getDateUpdated() {
    return dateUpdated;
  }
  
  public String getFriendlyName() {
    return friendlyName;
  }
  
  public String getPassword() {
    return password;
  }
  
  public String getProxy() {
    return proxy;
  }
  
  public String getUserName() {
    return userName;
  }
  
  public boolean register() {
    return register;
  }
  
  public int getTimeToLive() {
    return timeToLive;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public Gateway setFriendlyName(final String friendlyName) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
	    timeToLive, uri);
  }
  
  public Gateway setPassword(final String password) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
        timeToLive, uri);
  }
  
  public Gateway setProxy(final String proxy) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
        timeToLive, uri);
  }
  
  public Gateway setRegister(final boolean register) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
        timeToLive, uri);
  }
  
  public Gateway setUser(final String userName) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
        timeToLive, uri);
  }
  
  public Gateway setTtl(final int timeToLive) {
    return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName,
        timeToLive, uri);
  }
  
  @Override public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("Name: ").append(friendlyName).append("\n");
    buffer.append("User: ").append(userName).append("\n");
    buffer.append("Password: ").append(password).append("\n");
    buffer.append("Proxy: ").append(proxy).append("\n");
    buffer.append("Register: ").append(register).append("\n");
    buffer.append("Time To Live: ").append(timeToLive);
    return buffer.toString();
  }
}
