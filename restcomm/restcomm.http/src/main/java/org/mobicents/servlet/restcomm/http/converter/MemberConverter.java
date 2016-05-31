/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Member;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
@ThreadSafe
public final class MemberConverter extends AbstractConverter implements JsonSerializer<Member> {

    public MemberConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class klass) {
        return Member.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Member member = (Member) object;
        writer.startNode("QueueMember");
        writeCallSid(member.getSid(), writer);
        writeDateCreated(member.getDateEnqueued(), writer);
        writeWaitTime(member.getWaitTime(), writer);
        writePosition(member.getPosition(), writer);
        writer.endNode();

    }

    @Override
    public JsonElement serialize(final Member member, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeCallSid(member.getSid(), object);
        writeDateCreated(member.getDateEnqueued(), object);
        writeWaitTime(member.getWaitTime(), object);
        writePosition(member.getPosition(), object);
        return object;
    }

    private void writeWaitTime(final Integer waitTime, final HierarchicalStreamWriter writer) {
        writer.startNode("WaitTime");
        writer.setValue(waitTime.toString());
        writer.endNode();
    }

    private void writeWaitTime(final Integer waitTime, final JsonObject object) {
        object.addProperty("waitTime", waitTime.toString());
    }

    private void writePosition(final Integer position, final HierarchicalStreamWriter writer) {
        writer.startNode("position");
        writer.setValue(position.toString());
        writer.endNode();
    }

    private void writePosition(final Integer position, final JsonObject object) {
        object.addProperty("position", position.toString());
    }

    protected void writeDateEnqueued(final DateTime dateEnqueued, final HierarchicalStreamWriter writer) {
        writer.startNode("DateCreated");
        writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateEnqueued.toDate()));
        writer.endNode();
    }

    protected void writeDateEnqueued(final DateTime dateEnqueued, final JsonObject object) {
        object.addProperty("date_created",
                new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateEnqueued.toDate()));
    }

}
