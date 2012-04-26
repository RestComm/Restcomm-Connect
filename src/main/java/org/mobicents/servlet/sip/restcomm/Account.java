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

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Account {
  private final Sid sid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final String emailAddress;
  private final String friendlyName;
  private final Type type;
  private final Status status;
  private final String authToken;
  private final String role;
  private final URI uri;
  
  public Account(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String emailAddress,
      final String friendlyName, final Type type, final Status status, final String authToken, final String role,
      final URI uri) {
    super();
    this.sid = sid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.emailAddress = emailAddress;
    this.friendlyName = friendlyName;
    this.type = type;
    this.status = status;
    this.authToken = authToken;
    this.role = role;
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
  
  public String getEmailAddress() {
    return emailAddress;
  }
  
  public String getFriendlyName() {
    return friendlyName;
  }
  
  public Type getType() {
    return type;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public String getAuthToken() {
    return authToken;
  }
  
  public String getRole() {
    return role;
  }
  
  public URI getUri() {
    return uri;
  }
  
  public Account setEmailAddress(final String emailAddress) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public Account setFriendlyName(final String friendlyName) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public Account setType(final Type type) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public Account setStatus(final Status status) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public Account setAuthToken(final String authToken) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public Account setRole(final String role) {
    return new Account(sid, dateCreated, DateTime.now(), emailAddress, friendlyName, type, status, authToken, role, uri);
  }
  
  public enum Status {
    ACTIVE("active"),
    CLOSED("closed"),
    SUSPENDED("suspended");
    
    private final String text;
    
    private Status(final String text) {
      this.text = text;
    }
    
    public static Status getValueOf(final String text) {
      Status[] values = values();
      for(final Status value : values) {
        if(value.text.equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid account status.");
    }
    
    @Override public String toString() {
      return text;
    }
  };
  
  public enum Type {
    FULL("Full"),
    TRIAL("Trial");
    
    private final String text;
    
    private Type(final String text) {
      this.text = text;
    }
    
    public static Type getValueOf(final String text) {
      Type[] values = values();
      for(final Type value : values) {
        if(value.text.equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid account type.");
    }
    
    @Override public String toString() {
      return text;
    }
  };
}
