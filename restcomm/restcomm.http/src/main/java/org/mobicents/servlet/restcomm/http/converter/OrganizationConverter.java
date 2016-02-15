/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
import org.mobicents.servlet.restcomm.entities.Organization;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author guilherme.jansen@telestax.com
 */
public class OrganizationConverter extends AbstractConverter implements JsonSerializer<Organization> {

    public OrganizationConverter(Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Organization.class.equals(klass);
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final Organization organization = (Organization) object;
        writer.startNode("Organization");
        writeSid(organization.getSid(), writer);
        writeDateCreated(organization.getDateCreated(), writer);
        writeDateUpdated(organization.getDateUpdated(), writer);
        writeFriendlyName(organization.getFriendlyName(), writer);
        writeNamespace(organization, writer);
        writeAccountSid(organization.getAccountSid(), writer);
        writeApiVersion(organization.getApiVersion(), writer);
        writeUri(organization.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Organization organization, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(organization.getSid(), object);
        writeDateCreated(organization.getDateCreated(), object);
        writeDateUpdated(organization.getDateUpdated(), object);
        writeFriendlyName(organization.getFriendlyName(), object);
        writeNamespace(organization, object);
        writeAccountSid(organization.getAccountSid(), object);
        writeApiVersion(organization.getApiVersion(), object);
        writeUri(organization.getUri(), object);
        return object;
    }

    private void writeNamespace(final Organization organization, final HierarchicalStreamWriter writer) {
        if (organization.getNamespace() != null) {
            writer.startNode("Namespace");
            writer.setValue(organization.getNamespace());
            writer.close();
        }
    }

    private void writeNamespace(final Organization organization, final JsonObject object) {
        if (organization.getNamespace() != null) {
            object.addProperty("namespace", organization.getNamespace());
        } else {
            object.add("namespace", JsonNull.INSTANCE);
        }
    }

}
