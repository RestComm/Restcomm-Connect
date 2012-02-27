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
package org.mobicents.servlet.sip.restcomm.callmanager.gateway;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.TimerListener;

import org.mobicents.servlet.sip.restcomm.Gateway;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class SipGatewayManagerTimerListener implements TimerListener {
  public SipGatewayManagerTimerListener() {
    super();
  }
  
  @Override public void timeout(final ServletTimer timer) {
	final String type = (String)timer.getInfo();
	if("REGISTER".equals(type)) {
	  final SipApplicationSession application = timer.getApplicationSession();
	  final SipGatewayManager manager = (SipGatewayManager)application.getAttribute(SipGatewayManager.class.getName());
	  final Gateway gateway = (Gateway)application.getAttribute(Gateway.class.getName());
	  manager.register(gateway, SipGatewayManager.defaultRegistrationTtl);
	}
  }
}
