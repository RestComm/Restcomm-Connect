package org.mobicents.servlet.sip.restcomm;

import java.net.URI;
import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.http.RequestMethod;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface Notification {
  public String getSid();
  public DateTime getDateCreated();
  public DateTime getDateUpdated();
  public String getAccountSid();
  public String getCallSid();
  public String getApiVersion();
  public Integer getLog();
  public Integer getErrorCode();
  public URI getMoreInfo();
  public String getMessageText();
  public DateTime getMessageDate();
  public URI getRequestUrl();
  public RequestMethod getRequestMethod();
  public String getRequestVariables();
  public List<String> getRequestHeaders();
  public String getResponseBody();
  public URI getUri();
}
