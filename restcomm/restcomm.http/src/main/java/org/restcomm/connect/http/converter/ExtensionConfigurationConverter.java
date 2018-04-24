/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.connect.http.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.util.StringUtils;
import org.restcomm.connect.extension.api.ExtensionRules;

import java.lang.reflect.Type;

/**
 * @author gvagenas@gmail.com
 */
@ThreadSafe
public final class ExtensionConfigurationConverter extends AbstractConverter implements JsonSerializer<ExtensionRules> {
    private final String apiVersion;
    private final String rootUri;

    public ExtensionConfigurationConverter(final Configuration configuration) {
        super(configuration);
        this.apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return ExtensionRules.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final ExtensionRules extensionRules = (ExtensionRules) object;
        writer.startNode("ExtensionRules");
        writeSid(extensionRules.getSid(),writer);
        writer.startNode("Extension");
        writer.setValue(extensionRules.getExtensionName());
        writer.endNode();
        writer.startNode("Configuration");
        writer.setValue(extensionRules.getConfigurationData().toString());
        writer.endNode();
        writer.startNode("Configuration Type");
        writer.setValue(extensionRules.getConfigurationType().name());
        writer.endNode();
        writeDateCreated(extensionRules.getDateCreated(),writer);
        writeDateCreated(extensionRules.getDateUpdated(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final ExtensionRules extensionRules, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(extensionRules.getSid(), object);
        object.addProperty("extension", extensionRules.getExtensionName());
        object.addProperty("configuration", extensionRules.getConfigurationData().toString());
        object.addProperty("configuration type", extensionRules.getConfigurationType().name());
        writeDateCreated(extensionRules.getDateCreated(), object);
        writeDateUpdated(extensionRules.getDateUpdated(), object);
        return object;
    }
}
