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
package org.mobicents.servlet.restcomm.entities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Currency;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author pavel.slegr@telestax.com
 */
@Immutable public final class CallDetailRecord {
  private final Sid sid;
  private final Sid parentCallSid;
  private final DateTime dateCreated;
  private final DateTime dateUpdated;
  private final Sid accountSid;
  private final String to;
  private final String from;
  private final Sid phoneNumberSid;
  private final String status;
  private final DateTime startTime;
  private final DateTime endTime;
  private final Integer duration;
  private final BigDecimal price;
  private final Currency priceUnit;
  private final String direction;
  private final String answeredBy;
  private final String apiVersion;
  private final String forwardedFrom;
  private final String callerName;
  private final URI uri;

  private final String callPath;
  
  public CallDetailRecord(final Sid sid, final Sid parentCallSid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
      final String to, final String from , final Sid phoneNumberSid, final String status, final DateTime startTime, final DateTime endTime,
      final Integer duration, final BigDecimal price, final Currency priceUnit, final String direction, final String answeredBy, final String apiVersion,
      final String forwardedFrom, final String callerName, final URI uri, final String callPath) {
    super();
    this.sid = sid;
    this.parentCallSid = parentCallSid;
    this.dateCreated = dateCreated;
    this.dateUpdated = dateUpdated;
    this.accountSid = accountSid;
    this.to = to;
    this.from = from;
    this.phoneNumberSid = phoneNumberSid;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.duration = duration;
    this.price = price;
    this.priceUnit = priceUnit;
    this.direction = direction;
    this.answeredBy = answeredBy;
    this.apiVersion = apiVersion;
    this.forwardedFrom = forwardedFrom;
    this.callerName = callerName;
    this.uri = uri;
    this.callPath = callPath;
  }
  
  public static Builder builder() {
    return new Builder();
  }

  public Sid getSid() {
    return sid;
  }

  public Sid getParentCallSid() {
    return parentCallSid;
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

  public String getTo() {
    return to;
  }

  public String getFrom() {
    return from;
  }

  public Sid getPhoneNumberSid() {
    return phoneNumberSid;
  }

  public String getStatus() {
    return status;
  }

  public DateTime getStartTime() {
    return startTime;
  }

  public DateTime getEndTime() {
    return endTime;
  }

  public Integer getDuration() {
    return duration;
  }

  public BigDecimal getPrice() {
    return (price == null) ? new BigDecimal("0.0") : price;
  }
  
  public Currency getPriceUnit() {
	  return (priceUnit == null) ? Currency.getInstance("USD") : priceUnit;  }
  
  public String getDirection() {
    return direction;
  }

  public String getAnsweredBy() {
    return answeredBy;
  }
  
  public String getApiVersion() {
    return apiVersion;
  }

  public String getForwardedFrom() {
    return forwardedFrom;
  }

  public String getCallerName() {
    return callerName;
  }

  public URI getUri() {
    return uri;
  }
  
  public String getCallPath() {
	  return callPath;
  }
  
  public CallDetailRecord setStatus(final String status) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  public CallDetailRecord setStartTime(final DateTime startTime) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  public CallDetailRecord setEndTime(final DateTime endTime) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  public CallDetailRecord setDuration(final Integer duration) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  public CallDetailRecord setPrice(final BigDecimal price) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  public CallDetailRecord setAnsweredBy(final String answeredBy) {
    return new CallDetailRecord(sid, parentCallSid, dateCreated, DateTime.now(), accountSid, to, from , phoneNumberSid, status, startTime, endTime,
        duration, price, priceUnit, direction, answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
  }
  
  @NotThreadSafe public static final class Builder {
	private Sid sid;
    private Sid parentCallSid;
    private DateTime dateCreated;
    private DateTime dateUpdated;
    private Sid accountSid;
    private String to;
    private String from;
    private Sid phoneNumberSid;
    private String status;
    private DateTime startTime;
    private DateTime endTime;
    private Integer duration;
    private BigDecimal price;
    private Currency priceUnit;
    private String direction;
    private String answeredBy;
    private String apiVersion;
    private String forwardedFrom;
    private String callerName;
    private URI uri;
    
    private String callPath;

    private Builder() {
      super();
      sid = null;
      parentCallSid = null;
      dateCreated = null;
      dateUpdated = DateTime.now();
      accountSid = null;
      to = null;
      from = null;
      phoneNumberSid = null;
      status = null;
      startTime = null;
      endTime = null;
      duration = null;
      price = null;
      direction = null;
      answeredBy = null;
      apiVersion = null;
      forwardedFrom = null;
      callerName = null;
      uri = null;
      callPath = null;
    }
    
    public CallDetailRecord build() {
      return new CallDetailRecord(sid, parentCallSid, dateCreated, dateUpdated, accountSid,
          to, from, phoneNumberSid, status, startTime, endTime, duration, price, priceUnit, direction,
          answeredBy, apiVersion, forwardedFrom, callerName, uri, callPath);
    }

	public void setSid(final Sid sid) {
		this.sid = sid;
	}

	public void setParentCallSid(final Sid parentCallSid) {
		this.parentCallSid = parentCallSid;
	}

	public void setDateCreated(final DateTime dateCreated) {
		this.dateCreated = dateCreated;
	}

	public void setAccountSid(final Sid accountSid) {
		this.accountSid = accountSid;
	}

	public void setTo(final String to) {
		this.to = to;
	}

	public void setFrom(final String from) {
		this.from = from;
	}

	public void setPhoneNumberSid(final Sid phoneNumberSid) {
		this.phoneNumberSid = phoneNumberSid;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void setStartTime(final DateTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(final DateTime endTime) {
		this.endTime = endTime;
	}

	public void setDuration(final Integer duration) {
		this.duration = duration;
	}

	public void setPrice(final BigDecimal price) {
		this.price = price;
	}

	public void setPriceUnit(final Currency priceUnit) {
		this.priceUnit = priceUnit;
	}

	public void setDirection(final String direction) {
		this.direction = direction;
	}

	public void setAnsweredBy(final String answeredBy) {
		this.answeredBy = answeredBy;
	}

	public void setApiVersion(final String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public void setForwardedFrom(final String forwardedFrom) {
		this.forwardedFrom = forwardedFrom;
	}

	public void setCallerName(final String callerName) {
		this.callerName = callerName;
	}

	public void setUri(final URI uri) {
		this.uri = uri;
	}
	
	public void setCallPath(final String callPath) {
		this.callPath = callPath;
	}
  }
}
