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

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallInfoList;

import com.google.gson.JsonArray;
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
public class CallinfoListConverter extends AbstractConverter implements JsonSerializer<CallInfoList>{

    public CallinfoListConverter(Configuration configuration) {
        super(configuration);
    }

    @Override
    public boolean canConvert(final Class klass) {
        return CallInfoList.class.equals(klass);
    }

    @Override
    public JsonElement serialize(CallInfoList callInfoList, Type typeOfSrc, JsonSerializationContext context) {
        int size = callInfoList.getCallInfoList().size();
        int callsUpToNow = callInfoList.getCallsUpToNow();
        JsonObject result = new JsonObject();
        JsonArray array = new JsonArray();
        for (CallInfo callInfo: callInfoList.getCallInfoList()) {
            array.add(context.serialize(callInfo));
        }
        result.addProperty("LiveCalls", size);
        result.addProperty("CallsUpToNow",callsUpToNow);
        if (size > 0)
            result.add("CallInfoList", array);
        return result;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final CallInfoList list = (CallInfoList) object;
        int size = list.getCallInfoList().size();
        int callsUpToNow = list.getCallsUpToNow();

        writer.startNode("LiveCalls");
        writer.setValue(String.valueOf(size));
        writer.endNode();

        writer.startNode("CallsUpToNow");
        writer.setValue(String.valueOf(callsUpToNow));
        writer.endNode();

        if (size > 0) {
            writer.startNode("CallsInfo");

            for (final CallInfo callInfo : list.getCallInfoList()) {
                context.convertAnother(callInfo);
            }
            writer.endNode();
        }
    }
}
