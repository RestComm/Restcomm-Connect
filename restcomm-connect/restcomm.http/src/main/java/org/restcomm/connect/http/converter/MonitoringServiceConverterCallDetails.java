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
package org.restcomm.connect.http.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.monitoringservice.LiveCallsDetails;
import org.restcomm.connect.telephony.api.CallInfo;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class MonitoringServiceConverterCallDetails extends AbstractConverter implements JsonSerializer<LiveCallsDetails>{

    public MonitoringServiceConverterCallDetails (Configuration configuration) {
        super(configuration);
    }

    @Override
    public boolean canConvert(final Class klass) {
        return (List.class.equals(klass));
    }

    @Override
    public JsonElement serialize(LiveCallsDetails callDetails, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        JsonArray callsArray = new JsonArray();

        //First add InstanceId and Version details
        result.addProperty("InstanceId", RestcommConfiguration.getInstance().getMain().getInstanceId());
        result.addProperty("Version", Version.getVersion());
        result.addProperty("Revision", Version.getRevision());

        List<CallInfo> liveCalls = callDetails.getCallDetails();

        if (liveCalls.size() > 0)
            for (CallInfo callInfo: liveCalls) {
                callsArray.add(context.serialize(callInfo));
            }
        result.add("LiveCallDetails", callsArray);
        return result;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final List<CallInfo> callDetails = (List<CallInfo>) object;

        writer.startNode("InstanceId");
        writer.setValue(RestcommConfiguration.getInstance().getMain().getInstanceId());
        writer.endNode();

        writer.startNode("Version");
        writer.setValue(Version.getVersion());
        writer.endNode();

        writer.startNode("Revision");
        writer.setValue(Version.getRevision());
        writer.endNode();

        if (callDetails.size() > 0) {
            writer.startNode("LiveCallDetails");

            for (final CallInfo callInfo : callDetails) {
                context.convertAnother(callInfo);
            }
            writer.endNode();
        }
    }
}
