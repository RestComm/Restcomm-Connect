package org.mobicents.servlet.sip.restcomm.dao;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Account;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

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
    return bigDecimal.toString();
  }
  
  public static Date writeDateTime(final DateTime dateTime) {
    return dateTime.toDate();
  }
  
  public static String writeSid(final Sid sid) {
    return sid.toString();
  }
  
  public static String writeUri(final URI uri) {
    return uri.toString();
  }
}
