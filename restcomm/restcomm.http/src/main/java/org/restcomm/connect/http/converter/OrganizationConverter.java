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
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.entities.Organization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author maria farooq
 */
@ThreadSafe
public final class OrganizationConverter extends AbstractConverter implements JsonSerializer<Organization> {

    public OrganizationConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Organization.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Organization organization = (Organization) object;
        writer.startNode("Organization");
        writeSid(organization.getSid(), writer);
        writeDomainName(organization.getDomainName(), writer);
        writeStatus(organization.getStatus().toString(), writer);
        writeDateCreated(organization.getDateCreated(), writer);
        writeDateUpdated(organization.getDateUpdated(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Organization organization, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(organization.getSid(), object);
        writeDomainName(organization.getDomainName(), object);
        writeStatus(organization.getStatus().toString(), object);
        writeDateCreated(organization.getDateCreated(), object);
        writeDateUpdated(organization.getDateUpdated(), object);
        return object;
    }

}
