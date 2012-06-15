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

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Gateway implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String name;
  private final String password;
  private final String proxy;
  private final Boolean register;
  private final String user;
  private final int ttl;

  public Gateway(final String name, final String password, final String proxy,
      final Boolean register, final String user, final int ttl) {
    super();
    this.name = name;
    this.password = password;
    this.proxy = proxy;
    this.register = register;
    this.user = user;
    this.ttl = ttl;
  }
  
  public String getName() {
    return name;
  }
  
  public String getPassword() {
    return password;
  }
  
  public String getProxy() {
    return proxy;
  }
  
  public String getUser() {
    return user;
  }
  
  public boolean register() {
    return register;
  }
  
  public int getTtl() {
    return ttl;
  }
  
  public Gateway setPassword(final String password) {
    return new Gateway(name, password, proxy, register, user, ttl);
  }
  
  public Gateway setProxy(final String proxy) {
    return new Gateway(name, password, proxy, register, user, ttl);
  }
  
  public Gateway setRegister(final boolean register) {
    return new Gateway(name, password, proxy, register, user, ttl);
  }
  
  public Gateway setUser(final String user) {
    return new Gateway(name, password, proxy, register, user, ttl);
  }
  
  public Gateway setTtl(final int ttl) {
    return new Gateway(name, password, proxy, register, user, ttl);
  }
  
  @Override public String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("Name: ").append(name).append("\n");
    buffer.append("User: ").append(user).append("\n");
    buffer.append("Password: ").append(password).append("\n");
    buffer.append("Proxy: ").append(proxy).append("\n");
    buffer.append("Register: ").append(register).append("\n");
    buffer.append("Time To Live: ").append(ttl);
    return buffer.toString();
  }
}
