/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import javax.servlet.sip.URI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class CallinfoConverter extends AbstractConverter implements JsonSerializer<CallInfo>{
    private final String apiVersion;
    private final String rootUri;

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return CallInfo.class.equals(klass);
    }

    /**
     * @param configuration
     */
    public CallinfoConverter(Configuration configuration) {
        super(configuration);
        apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @Override
    public JsonElement serialize(CallInfo callInfo, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(callInfo.sid(), object);
        writeState(callInfo.state().name(), object);
        if (callInfo.type() != null)
            writeType(callInfo.type().name(), object);
        writeDirection(callInfo.direction(), object);
        writeDateCreated(callInfo.dateCreated(), object);
        writeForwardedFrom(callInfo.forwardedFrom(), object);
        writeCallerName(callInfo.fromName(), object);
        writeFrom(callInfo.from(), object);
        writeTo(callInfo.to(), object);
        writeInviteUri(callInfo.invite().getRequestURI(), object);
        if (callInfo.lastResponse() != null)
            writeLastResponseUri(callInfo.lastResponse().getStatus(), object);
        return object;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final CallInfo callInfo = (CallInfo) object;
        writer.startNode("CallInfo");
        writeSid(callInfo.sid(), writer);
        writeState(callInfo.state().name(), writer);
        if (callInfo.type() != null)
            writeType(callInfo.type().name(), writer);
        writeDirection(callInfo.direction(), writer);
        writeDateCreated(callInfo.dateCreated(), writer);
        writeForwardedFrom(callInfo.forwardedFrom(), writer);
        writeCallerName(callInfo.fromName(), writer);
        writeFrom(callInfo.from(), writer);
        writeTo(callInfo.to(), writer);
        writeInviteUri(callInfo.invite().getRequestURI(), writer);
        if (callInfo.lastResponse() != null)
            writeLastResponseUri(callInfo.lastResponse().getStatus(), writer);
        writer.endNode();
    }

    private void writeState(final String state, final HierarchicalStreamWriter writer) {
        writer.startNode("State");
        writer.setValue(state);
        writer.endNode();
    }

    private void writeState(final String state, final JsonObject object) {
        object.addProperty("State", state);
    }

    private void writeDirection(final String direction, final HierarchicalStreamWriter writer) {
        writer.startNode("Direction");
        writer.setValue(direction);
        writer.endNode();
    }

    private void writeDirection(final String direction, final JsonObject object) {
        object.addProperty("direction", direction);
    }

    private void writeForwardedFrom(final String forwardedFrom, final HierarchicalStreamWriter writer) {
        writer.startNode("ForwardedFrom");
        if (forwardedFrom != null) {
            writer.setValue(forwardedFrom);
        }
        writer.endNode();
    }

    private void writeForwardedFrom(final String forwardedFrom, final JsonObject object) {
        object.addProperty("ForwardedFrom", forwardedFrom);
    }

    private void writeCallerName(final String callerName, final HierarchicalStreamWriter writer) {
        writer.startNode("CallerName");
        if (callerName != null) {
            writer.setValue(callerName);
        }
        writer.endNode();
    }

    private void writeCallerName(final String callerName, final JsonObject object) {
        object.addProperty("CallerName",  callerName);
    }

    private void writeInviteUri(final URI requestUri, final HierarchicalStreamWriter writer) {
        writer.startNode("Initial Invite");
        if (requestUri != null) {
            writer.setValue(requestUri.toString());
        }
        writer.endNode();
    }

    private void writeInviteUri(final URI requestUri, final JsonObject object) {
        object.addProperty("Initial Invite", requestUri.toString());
    }

    private void writeLastResponseUri(final int responseCode, final HierarchicalStreamWriter writer) {
        writer.startNode("Last Response");
        if (responseCode > -1) {
            writer.setValue(String.valueOf(responseCode));
        }
        writer.endNode();
    }

    private void writeLastResponseUri(final int responseCode, final JsonObject object) {
        object.addProperty("Last Response", responseCode);
    }
}
