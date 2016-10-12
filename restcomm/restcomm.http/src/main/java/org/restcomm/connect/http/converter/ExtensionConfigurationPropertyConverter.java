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
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.extension.api.ExtensionConfigurationProperty;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class ExtensionConfigurationPropertyConverter extends AbstractConverter implements JsonSerializer<ExtensionConfigurationProperty> {
    private final String apiVersion;
    private final String rootUri;

    public ExtensionConfigurationPropertyConverter(final Configuration configuration) {
        super(configuration);
        this.apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return ExtensionConfigurationProperty.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final ExtensionConfigurationProperty extensionConfigurationProperty = (ExtensionConfigurationProperty) object;
        writer.startNode("ExtensionConfigurationProperty");
        writer.startNode("Extension");
        writer.setValue(extensionConfigurationProperty.getExtension());
        writer.endNode();
        writer.startNode("Property");
        writer.setValue(extensionConfigurationProperty.getProperty());
        writer.endNode();
        writer.startNode("ExtraParameter");
        writer.setValue(extensionConfigurationProperty.getExtraParameter());
        writer.endNode();
        writer.startNode("PropertyValue");
        writer.setValue(extensionConfigurationProperty.getPropertyValue());
        writer.endNode();
        writeDateCreated(extensionConfigurationProperty.getDateCreated(),writer);
        writeDateCreated(extensionConfigurationProperty.getDateUpdated(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final ExtensionConfigurationProperty extensionConfigurationProperty, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        object.addProperty("extension", extensionConfigurationProperty.getExtension());
        object.addProperty("property", extensionConfigurationProperty.getProperty());
        object.addProperty("extraParameter", extensionConfigurationProperty.getExtraParameter());
        object.addProperty("propertyValue", extensionConfigurationProperty.getPropertyValue());
        writeDateCreated(extensionConfigurationProperty.getDateCreated(), object);
        writeDateUpdated(extensionConfigurationProperty.getDateUpdated(), object);
        return object;
    }
}
