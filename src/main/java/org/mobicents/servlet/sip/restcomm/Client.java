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
package org.mobicents.servlet.sip.restcomm;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Client {
  public static final int NOT_PRESENT = 0;
  public static final int PRESENT = 1;
  
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final String apiVersion;
  private final String friendlyName;
  private final String login;
  private final String password;
  private final Integer status;
  private final URI uri;
  
  public Client(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final String apiVersion, final String friendlyName, final String login, final String password,
      final Integer status, final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.apiVersion = apiVersion;
    this.friendlyName = friendlyName;
    this.login = login;
    this.password = password;
    this.status = status;
    this.uri = uri;
  }
  
  public static Builder builder() {
    return new Builder();
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

  public Sid getAccountSid() {
	return accountSid;
  }

  public String getApiVersion() {
	return apiVersion;
  }
  
  public String getFriendlyName() {
    return friendlyName;
  }
  
  public String getLogin() {
    return login;
  }
  
  public String getPassword() {
    return password;
  }
  
  public Integer getStatus() {
    return status;
  }

  public URI getUri() {
	return uri;
  }
  
  public Client setFriendlyName(final String friendlyName) {
    return new Client(sid, dateCreated, DateTime.now(), accountSid, apiVersion, friendlyName, login, password, status, uri);
  }
  
  public Client setPassword(final String password) {
    return new Client(sid, dateCreated, DateTime.now(), accountSid, apiVersion, friendlyName, login, password, status, uri);
  }
  
  public Client setStatus(final int status) {
    return new Client(sid, dateCreated, DateTime.now(), accountSid, apiVersion, friendlyName, login, password, status, uri);
  }
  
  @NotThreadSafe public final static class Builder {
    private Sid sid;
    private Sid accountSid;
    private String apiVersion;
    private String friendlyName;
    private String login;
    private String password;
    private int status;
    private URI uri;
	  
    private Builder() {
      super();
    }
    
    public Client build() {
      final DateTime now = DateTime.now();
      return new Client(sid, now, now, accountSid, apiVersion, friendlyName, login, password, status, uri);
    }
    
    public void setSid(final Sid sid) {
      this.sid = sid;
    }
    
    public void setAccountSid(final Sid accountSid) {
      this.accountSid = accountSid;
    }
    
    public void setApiVersion(final String apiVersion) {
      this.apiVersion = apiVersion;
    }
    
    public void setFriendlyName(final String friendlyName) {
      this.friendlyName = friendlyName;
    }
    
    public void setLogin(final String login) {
      this.login = login;
    }
    
    public void setPassword(final String password) {
      this.password = password;
    }
    
    public void setStatus(final int status) {
      this.status = status;
    }
    
    public void setUri(final URI uri) {
      this.uri = uri;
    }
  }
}
