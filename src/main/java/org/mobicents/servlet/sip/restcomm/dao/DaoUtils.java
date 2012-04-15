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
package org.mobicents.servlet.sip.restcomm.dao;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class DaoUtils {
  private DaoUtils() {
    super();
  }
  
  public static Account.Status readAccountStatus(final Object object) {
    if(object != null) {
      return Account.Status.getValueOf((String)object);
    } else {
      return null;
    }
  }
  
  public static Account.Type readAccountType(final Object object) {
    if(object != null) {
      return Account.Type.getValueOf((String)object);
    } else {
      return null;
    }
  }
  
  public static BigDecimal readBigDecimal(final Object object) {
    if(object != null) {
      return new BigDecimal((String)object);
    } else {
      return null;
    }
  }
  
  public static Boolean readBoolean(final Object object) {
    if(object != null) {
      return (Boolean)object;
    } else {
      return null;
    }
  }
  
  public static DateTime readDateTime(final Object object) {
    if(object != null) {
      return new DateTime((Date)object);
    } else {
      return null;
    }
  }
  
  public static Double readDouble(final Object object) {
    if(object != null) {
      return (Double)object;
    } else {
      return null;
    }
  }
  
  public static Integer readInteger(final Object object) {
    if(object != null) {
      return (Integer)object;
    } else {
      return null;
    }
  }
  
  public static Long readLong(final Object object) {
    if(object != null) {
      return (Long)object;
    } else {
      return null;
    }
  }
  
  public static Sid readSid(final Object object) {
    if(object != null) {
      return new Sid((String)object);
    } else {
      return null;
    }
  }
  
  public static String readString(final Object object) {
    if(object != null) {
      return (String)object;
    } else {
      return null;
    }
  }
  
  public static URI readUri(final Object object) {
    if(object != null) {
      return URI.create((String)object);
    } else {
      return null;
    }
  }
  
  public static String writeAccountStatus(final Account.Status status) {
    return status.toString();
  }
  
  public static String writeAccountType(final Account.Type type) {
    return type.toString();
  }
  
  public static String writeBigDecimal(final BigDecimal bigDecimal) {
    if(bigDecimal != null) {
      return bigDecimal.toString();
    } else {
      return null;
    }
  }
  
  public static Date writeDateTime(final DateTime dateTime) {
    if(dateTime != null) {
      return dateTime.toDate();
    } else {
      return null;
    }
  }
  
  public static String writeSid(final Sid sid) {
    return sid.toString();
  }
  
  public static String writeUri(final URI uri) {
    return uri.toString();
  }
}
