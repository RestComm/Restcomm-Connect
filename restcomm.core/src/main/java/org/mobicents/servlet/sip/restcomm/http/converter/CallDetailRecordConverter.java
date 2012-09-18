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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class CallDetailRecordConverter extends AbstractConverter implements
    JsonSerializer<CallDetailRecord> {
  private final String apiVersion;
  private final String rootUri;
  
  public CallDetailRecordConverter(final Configuration configuration) {
    super(configuration);
    apiVersion = configuration.getString("api-version");
    rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return CallDetailRecord.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final CallDetailRecord cdr = (CallDetailRecord)object;
    writer.startNode("Call");
    writeSid(cdr.getSid(), writer);
    writeDateCreated(cdr.getDateCreated(), writer);
    writeDateUpdated(cdr.getDateUpdated(), writer);
    writeParentCallSid(cdr.getParentCallSid(), writer);
    writeAccountSid(cdr.getAccountSid(), writer);
    writeTo(cdr.getTo(), writer);
    writeFrom(cdr.getFrom(), writer);
    writePhoneNumberSid(cdr.getPhoneNumberSid(), writer);
    writeStatus(cdr.getStatus(), writer);
    writeStartTime(cdr.getStartTime(), writer);
    writeEndTime(cdr.getEndTime(), writer);
    writeDuration(cdr.getDuration(), writer);
    writePrice(cdr.getPrice(), writer);
    writeDirection(cdr.getDirection(), writer);
    writeAnsweredBy(cdr.getAnsweredBy(), writer);
    writeApiVersion(cdr.getApiVersion(), writer);
    writeForwardedFrom(cdr.getForwardedFrom(), writer);
    writeCallerName(cdr.getCallerName(), writer);
    writeUri(cdr.getUri(), writer);
    writeSubResources(cdr, writer);
    writer.endNode();
  }
  
  private String prefix(final CallDetailRecord cdr) {
    final StringBuilder buffer = new StringBuilder();
	buffer.append(rootUri).append(apiVersion).append("/Accounts/");
	buffer.append(cdr.getAccountSid().toString()).append("/Calls/");
	buffer.append(cdr.getSid());
	return buffer.toString();
  }

  @Override public JsonElement serialize(final CallDetailRecord cdr, Type type,
      final JsonSerializationContext context) {
	final JsonObject object = new JsonObject();
	writeSid(cdr.getSid(), object);
	writeDateCreated(cdr.getDateCreated(), object);
	writeDateUpdated(cdr.getDateUpdated(), object);
	writeParentCallSid(cdr.getParentCallSid(), object);
	writeAccountSid(cdr.getAccountSid(), object);
	writeTo(cdr.getTo(), object);
	writeFrom(cdr.getFrom(), object);
	writePhoneNumberSid(cdr.getPhoneNumberSid(), object);
	writeStatus(cdr.getStatus(), object);
	writeStartTime(cdr.getStartTime(), object);
	writeEndTime(cdr.getEndTime(), object);
	writeDuration(cdr.getDuration(), object);
	writePrice(cdr.getPrice(), object);
	writeDirection(cdr.getDirection(), object);
	writeAnsweredBy(cdr.getAnsweredBy(), object);
	writeApiVersion(cdr.getApiVersion(), object);
	writeForwardedFrom(cdr.getForwardedFrom(), object);
	writeCallerName(cdr.getCallerName(), object);
	writeUri(cdr.getUri(), object);
	writeSubResources(cdr, object);
	return object;
  }
  
  private void writeAnsweredBy(final String answeredBy, final HierarchicalStreamWriter writer) {
    writer.startNode("AnsweredBy");
    if(answeredBy != null) {
      writer.setValue(answeredBy);
    }
    writer.endNode();
  }
  
  private void writeAnsweredBy(final String answeredBy, final JsonObject object) {
    object.addProperty("answered_by", answeredBy);
  }
  
  private void writeCallerName(final String callerName, final HierarchicalStreamWriter writer) {
    writer.startNode("CallerName");
    if(callerName != null) {
      writer.setValue(callerName);
    }
    writer.endNode();
  }
  
  private void writeCallerName(final String callerName, final JsonObject object) {
    object.addProperty("caller_name", callerName);
  }
  
  private void writeDirection(final String direction, final HierarchicalStreamWriter writer) {
    writer.startNode("Direction");
    writer.setValue(direction);
    writer.endNode();
  }
  
  private void writeDirection(final String direction, final JsonObject object) {
    object.addProperty("direction", direction);
  }
  
  private void writeDuration(final Integer duration, final HierarchicalStreamWriter writer) {
    writer.startNode("Duration");
    if(duration != null) {
      writer.setValue(duration.toString());
    }
    writer.endNode();
  }
  
  private void writeDuration(final Integer duration, final JsonObject object) {
    object.addProperty("duration", duration);
  }
  
  private void writeForwardedFrom(final String forwardedFrom, final HierarchicalStreamWriter writer) {
    writer.startNode("ForwardedFrom");
    if(forwardedFrom != null) {
      writer.setValue(forwardedFrom);
    }
    writer.endNode();
  }
  
  private void writeForwardedFrom(final String forwardedFrom, final JsonObject object) {
    object.addProperty("forwarded_from", forwardedFrom);
  }
  
  private void writeParentCallSid(final Sid sid, final HierarchicalStreamWriter writer) {
    writer.startNode("ParentCallSid");
	if(sid != null) {
	  writer.setValue(sid.toString());
	}
	writer.endNode();
  }
  
  private void writeParentCallSid(final Sid sid, final JsonObject object) {
    object.addProperty("parent_call_sid", sid.toString());
  }
  
  private void writePhoneNumberSid(final Sid sid, final HierarchicalStreamWriter writer) {
    writer.startNode("PhoneNumberSid");
    if(sid != null) {
      writer.setValue(sid.toString());
    }
    writer.endNode();
  }
  
  private void writePhoneNumberSid(final Sid sid, final JsonObject object) {
    object.addProperty("phone_number_sid", sid.toString());
  }
  
  private void writeEndTime(final DateTime endTime, final HierarchicalStreamWriter writer) {
    writer.startNode("EndTime");
    if(endTime != null) {
      writer.setValue(endTime.toString());
    }
    writer.endNode();
  }
  
  private void writeEndTime(final DateTime endTime, final JsonObject object) {
    object.addProperty("end_time", endTime.toString());
  }
  
  private void writeStartTime(final DateTime startTime, final HierarchicalStreamWriter writer) {
    writer.startNode("StartTime");
    if(startTime != null) {
      writer.setValue(startTime.toString());
    }
    writer.endNode();
  }
  
  private void writeStartTime(final DateTime startTime, final JsonObject object) {
    object.addProperty("start_time", startTime.toString());
  }
  
  private void writeNotifications(final CallDetailRecord cdr, final HierarchicalStreamWriter writer) {
    writer.startNode("Notifications");
    writer.setValue(prefix(cdr) + "/Notifications");
    writer.endNode();
  }
  
  private void writeNotifications(final CallDetailRecord cdr, final JsonObject object) {
    object.addProperty("notifications", prefix(cdr) + "/Notifications");
  }
  
  private void writeRecordings(final CallDetailRecord cdr, final HierarchicalStreamWriter writer) {
    writer.startNode("Recordings");
    writer.setValue(prefix(cdr) + "/Recordings");
    writer.endNode();
  }
  
  private void writeRecordings(final CallDetailRecord cdr, final JsonObject object) {
    object.addProperty("recordings", prefix(cdr) + "/Recordings");
  }
  
  private void writeSubResources(final CallDetailRecord cdr, final HierarchicalStreamWriter writer) {
    writer.startNode("SubresourceUris");
    writeNotifications(cdr, writer);
    writeRecordings(cdr, writer);
    writer.endNode();
  }
  
  private void writeSubResources(final CallDetailRecord cdr, final JsonObject object) {
    final JsonObject other = new JsonObject();
    writeNotifications(cdr, other);
    writeRecordings(cdr, other);
    object.add("subresource_uris", other);
  }
}
