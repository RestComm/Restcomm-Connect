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

import java.util.List;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ApplicationsDao;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public class MongoApplicationDao implements ApplicationsDao {
  public MongoApplicationDao() {
    super();
  }

  @Override public void addApplication(final Application application) {
    
  }

  @Override public Application getApplication(final Sid sid) {
    return null;
  }

  @Override public List<Application> getApplications(final Sid accountSid) {
    return null;
  }

  @Override public void removeApplication(final Sid sid) {
    
  }

  @Override public void removeApplications(final Sid accountSid) {
    
  }

  @Override public void updateApplication(final Application application) {
    
  }
}
