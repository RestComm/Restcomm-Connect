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
package org.mobicents.servlet.sip.restcomm.callmanager.presence;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.TimerListener;

import org.mobicents.servlet.sip.restcomm.entities.PresenceRecord;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public class PresenceManagerTimerListener implements TimerListener {
  public PresenceManagerTimerListener() {
    super();
  }
  
  @Override public void timeout(final ServletTimer timer) {
    final String type = (String)timer.getInfo();
    final SipApplicationSession application = timer.getApplicationSession();
	final PresenceManager manager = (PresenceManager)application.getAttribute(PresenceManager.class.getName());
	final PresenceRecord record = (PresenceRecord)application.getAttribute(PresenceRecord.class.getName());
	if("CLEANUP".equals(type)) {
	  manager.cleanup(record);
	  application.invalidate();
	} else if("OPTIONS_PING".equals(type)) {
	  manager.ping(record);
	}
  }
}
