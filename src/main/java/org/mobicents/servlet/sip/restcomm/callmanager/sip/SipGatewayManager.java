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
package org.mobicents.servlet.sip.restcomm.callmanager.sip;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.LifeCycle;

public final class SipGatewayManager implements Configurable, LifeCycle {
  private static final class SingletonHolder {
    private static final SipGatewayManager INSTANCE = new SipGatewayManager();
  }
  private static final String CONFIGURATION_PREFIX = "gateway-manager.gateway";
  
  private Configuration configuration;
  private SipGateway gateway;
  
  private SipGatewayManager() {
    super();
    this.gateway = null;
  }
  
  @Override public void configure(final Configuration configuration) {
	this.configuration = configuration;
  }
  
  public SipGateway getGateway() {
    return gateway;
  }
  
  public static SipGatewayManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  @Override public void start() throws RuntimeException {
	gateway = new SipGateway();
    gateway.setName(configuration.getString(CONFIGURATION_PREFIX + "[@name]"));
    gateway.setUser(configuration.getString(CONFIGURATION_PREFIX + ".user"));
    gateway.setPassword(configuration.getString(CONFIGURATION_PREFIX + ".password"));
    gateway.setRegister(configuration.getBoolean(CONFIGURATION_PREFIX + ".register"));
    gateway.setProxy(configuration.getString(CONFIGURATION_PREFIX + ".proxy"));
  }

  @Override public void shutdown() {
    // Nothing to do.
  }
}
