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
package org.mobicents.servlet.sip.restcomm.http.converter;

import java.lang.reflect.Type;
import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Notification;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class NotificationConverter extends AbstractConverter
    implements JsonSerializer<Notification> {
  public NotificationConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return Notification.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Notification notification = (Notification)object;
    writeSid(notification.getSid(), writer);
    writeDateCreated(notification.getDateCreated(), writer);
    writeDateUpdated(notification.getDateUpdated(), writer);
    writeAccountSid(notification.getAccountSid(), writer);
    writeCallSid(notification.getCallSid(), writer);
    writeApiVersion(notification.getApiVersion(), writer);
    writeLog(notification.getLog(), writer);
    writeErrorCode(notification.getErrorCode(), writer);
    writeMoreInfo(notification.getMoreInfo(), writer);
    writeMessageText(notification.getMessageText(), writer);
    writeMessageDate(notification.getMessageDate(), writer);
    writeRequestUrl(notification.getRequestUrl(), writer);
    writeRequestMethod(notification.getRequestMethod(), writer);
    writeRequestVariables(notification.getRequestVariables(), writer);
    writeResponseHeaders(notification.getResponseHeaders(), writer);
    writeResponseBody(notification.getResponseBody(), writer);
    writeUri(notification.getUri(), writer);
  }
  
  @Override public JsonElement serialize(final Notification notification, final Type type,
      final JsonSerializationContext context) {
    final JsonObject object = new JsonObject();
    writeSid(notification.getSid(), object);
    writeDateCreated(notification.getDateCreated(), object);
    writeDateUpdated(notification.getDateUpdated(), object);
    writeAccountSid(notification.getAccountSid(), object);
    writeCallSid(notification.getCallSid(), object);
    writeApiVersion(notification.getApiVersion(), object);
    writeLog(notification.getLog(), object);
    writeErrorCode(notification.getErrorCode(), object);
    writeMoreInfo(notification.getMoreInfo(), object);
    writeMessageText(notification.getMessageText(), object);
    writeMessageDate(notification.getMessageDate(), object);
    writeRequestUrl(notification.getRequestUrl(), object);
    writeRequestMethod(notification.getRequestMethod(), object);
    writeRequestVariables(notification.getRequestVariables(), object);
    writeResponseHeaders(notification.getResponseHeaders(), object);
    writeResponseBody(notification.getResponseBody(), object);
    writeUri(notification.getUri(), object);
  	return object;
  }
  
  private void writeErrorCode(final int errorCode, final HierarchicalStreamWriter writer) {
    writer.startNode("ErrorCode");
    writer.setValue(Integer.toString(errorCode));
    writer.endNode();
  }
  
  private void writeErrorCode(final int errorCode, final JsonObject object) {
    object.addProperty("error_code", errorCode);
  }
  
  private void writeLog(final int log, final HierarchicalStreamWriter writer) {
    writer.startNode("Log");
    writer.setValue(Integer.toString(log));
    writer.endNode();
  }
  
  private void writeLog(final int log, final JsonObject object) {
    object.addProperty("log", log);
  }
  
  private void writeMessageDate(final DateTime messageDate, final HierarchicalStreamWriter writer) {
    writer.startNode("MessageDate");
    writer.setValue(messageDate.toString());
    writer.endNode();
  }
  
  private void writeMessageDate(final DateTime messageDate, final JsonObject object) {
    object.addProperty("message_date", messageDate.toString());
  }
  
  private void writeMessageText(final String messageText, final HierarchicalStreamWriter writer) {
    writer.startNode("MessageText");
    if(messageText != null) {
      writer.setValue(messageText);
    }
    writer.endNode();
  }
  
  private void writeMessageText(final String messageText, final JsonObject object) {
    if(messageText != null) {
      object.addProperty("message_text", messageText);
    } else {
      object.add("message_text", JsonNull.INSTANCE);
    }
  }
  
  private void writeMoreInfo(final URI moreInfo, final HierarchicalStreamWriter writer) {
    writer.startNode("MoreInfo");
    writer.setValue(moreInfo.toString());
    writer.endNode();
  }
  
  private void writeMoreInfo(final URI moreInfo, final JsonObject object) {
    object.addProperty("more_info", moreInfo.toString());
  }
  
  private void writeRequestUrl(final URI requestUrl, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestUrl");
    writer.setValue(requestUrl.toString());
    writer.endNode();
  }
  
  private void writeRequestUrl(final URI requestUrl, final JsonObject object) {
    object.addProperty("request_url", requestUrl.toString());
  }
  
  private void writeRequestMethod(final String requestMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestMethod");
    writer.setValue(requestMethod);
    writer.endNode();
  }
  
  private void writeRequestMethod(final String requestMethod, final JsonObject object) {
    object.addProperty("request_method", requestMethod);
  }
  
  private void writeRequestVariables(final String requestVariables, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestVariables");
    if(requestVariables != null) {
      writer.setValue(requestVariables);
    }
    writer.endNode();
  }
  
  private void writeRequestVariables(final String requestVariables, final JsonObject object) {
    if(requestVariables != null) {
      object.addProperty("request_variables", requestVariables);
    } else {
      object.add("request_variables", JsonNull.INSTANCE);
    }
  }
  
  private void writeResponseHeaders(final String responseHeaders, final HierarchicalStreamWriter writer) {
    writer.startNode("ResponseHeaders");
    if(responseHeaders != null) {
      writer.setValue(responseHeaders);
    }
    writer.endNode();
  }
  
  private void writeResponseHeaders(final String responseHeaders, final JsonObject object) {
    if(responseHeaders != null) {
      object.addProperty("response_headers", responseHeaders);
    } else {
      object.add("response_headers", JsonNull.INSTANCE);
    }
  }
  
  private void writeResponseBody(final String responseBody, final HierarchicalStreamWriter writer) {
    writer.startNode("ResponseBody");
    if(responseBody != null) {
      writer.setValue(responseBody);
    }
    writer.endNode();
  }
  
  private void writeResponseBody(final String responseBody, final JsonObject object) {
    if(responseBody != null) {
      object.addProperty("response_body", responseBody);
    } else {
      object.add("response_body", JsonNull.INSTANCE);
    }
  }
}
