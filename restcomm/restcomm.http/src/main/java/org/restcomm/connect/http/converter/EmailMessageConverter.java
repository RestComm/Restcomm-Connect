
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
package org.restcomm.connect.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.restcomm.connect.email.api.Mail;
import org.restcomm.connect.dao.entities.Sid;


import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author eleftherios.banos@telestax.com (Lefteis Banos)
 */
public class EmailMessageConverter extends AbstractConverter implements JsonSerializer<Mail> {
    public EmailMessageConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Mail.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Mail  mailMessage = (Mail) object;
        writer.startNode("EmailMessage");
        writeDateSent(mailMessage.dateSent(), writer);
        writeAccountSid(new Sid(mailMessage.accountSid()), writer);
        writeFrom(mailMessage.from(), writer);
        writeTo(mailMessage.to(), writer);
        writeBody(mailMessage.body(), writer);
        writeSubject(mailMessage.subject().toString(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Mail mailMessage, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeDateSent(mailMessage.dateSent(), object);
        writeAccountSid(new Sid(mailMessage.accountSid()), object);
        writeFrom(mailMessage.from(), object);
        writeTo(mailMessage.to(), object);
        writeBody(mailMessage.body(), object);
        writeSubject(mailMessage.subject().toString(), object);
        return object;
    }

    private void writeBody(final String body, final HierarchicalStreamWriter writer) {
        writer.startNode("Body");
        if (body != null) {
            writer.setValue(body);
        }
        writer.endNode();
    }

    private void writeBody(final String body, final JsonObject object) {
        if (body != null) {
            object.addProperty("body", body);
        } else {
            object.add("body", JsonNull.INSTANCE);
        }
    }

    private void writeDateSent(final DateTime dateSent, final HierarchicalStreamWriter writer) {
        writer.startNode("DateSent");
        if (dateSent != null) {
            writer.setValue(dateSent.toString());
        }
        writer.endNode();
    }

    private void writeDateSent(final DateTime dateSent, final JsonObject object) {
        object.addProperty("date_sent", dateSent != null ? dateSent.toString() : null);
    }

    private void writeSubject(final String subject, final HierarchicalStreamWriter writer) {
        writer.startNode("Subject");
        writer.setValue(subject);
        writer.endNode();
    }

    private void writeSubject(final String subject, final JsonObject object) {
        object.addProperty("subject", subject);
    }
}
