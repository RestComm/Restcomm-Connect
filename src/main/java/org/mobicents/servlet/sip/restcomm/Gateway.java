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

  public Gateway(final String name, final String password, final String proxy,
      final Boolean register, final String user) {
    super();
    this.name = name;
    this.password = password;
    this.proxy = proxy;
    this.register = register;
    this.user = user;
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
  
  public Gateway setName(final String name) {
    return new Gateway(name, password, proxy, register, user);
  }
  
  public Gateway setPassword(final String password) {
    return new Gateway(name, password, proxy, register, user);
  }
  
  public Gateway setProxy(final String proxy) {
    return new Gateway(name, password, proxy, register, user);
  }
  
  public Gateway setRegister(final boolean register) {
    return new Gateway(name, password, proxy, register, user);
  }
  
  public Gateway setUser(final String user) {
    return new Gateway(name, password, proxy, register, user);
  }
}
