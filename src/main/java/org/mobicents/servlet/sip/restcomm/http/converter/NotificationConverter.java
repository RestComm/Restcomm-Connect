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

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class NotificationConverter extends AbstractConverter {
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

  @Override public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
    return null;
  }
  
  private void writeErrorCode(final int errorCode, final HierarchicalStreamWriter writer) {
    writer.startNode("ErrorCode");
    writer.setValue(Integer.toString(errorCode));
    writer.endNode();
  }
  
  private void writeLog(final int log, final HierarchicalStreamWriter writer) {
    writer.startNode("Log");
    writer.setValue(Integer.toString(log));
    writer.endNode();
  }
  
  private void writeMessageDate(final DateTime messageDate, final HierarchicalStreamWriter writer) {
    writer.startNode("MessageDate");
    writer.setValue(messageDate.toString());
    writer.endNode();
  }
  
  private void writeMessageText(final String messageText, final HierarchicalStreamWriter writer) {
    writer.startNode("MessageText");
    writer.setValue(messageText);
    writer.endNode();
  }
  
  private void writeMoreInfo(final URI moreInfo, final HierarchicalStreamWriter writer) {
    writer.startNode("MoreInfo");
    writer.setValue(moreInfo.toString());
    writer.endNode();
  }
  
  private void writeRequestUrl(final URI requestUrl, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestUrl");
    writer.setValue(requestUrl.toString());
    writer.endNode();
  }
  
  private void writeRequestMethod(final String requestMethod, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestMethod");
    writer.setValue(requestMethod);
    writer.endNode();
  }
  
  private void writeRequestVariables(final String requestVariables, final HierarchicalStreamWriter writer) {
    writer.startNode("RequestVariables");
    writer.setValue(requestVariables);
    writer.endNode();
  }
  
  private void writeResponseHeaders(final String responseHeaders, final HierarchicalStreamWriter writer) {
    writer.startNode("ResponseHeaders");
    writer.setValue(responseHeaders);
    writer.endNode();
  }
  
  private void writeResponseBody(final String responseBody, final HierarchicalStreamWriter writer) {
    writer.startNode("ResponseBody");
    writer.setValue(responseBody);
    writer.endNode();
  }
}
