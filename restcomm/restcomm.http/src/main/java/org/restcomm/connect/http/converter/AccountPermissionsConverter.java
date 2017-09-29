package org.restcomm.connect.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.util.StringUtils;
import org.restcomm.connect.dao.entities.AccountPermission;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class AccountPermissionsConverter extends AbstractConverter implements JsonSerializer<AccountPermission>{
    private final String apiVersion;
    private final String rootUri;

    public AccountPermissionsConverter(Configuration configuration) {
        super(configuration);
        this.apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return AccountPermission.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final AccountPermission permission = (AccountPermission) object;
        writer.startNode("Permission");
        writeSid(permission.getSid(),writer);
        writer.startNode("Name");
        writer.setValue(permission.getName());
        writer.endNode();
        writer.startNode("Value");
        writer.setValue(Boolean.toString(permission.getValue()));
        writer.endNode();
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final AccountPermission permission, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(permission.getSid(), object);
        object.addProperty("name", permission.getName());
        object.addProperty("value", permission.getValue());
        return object;
    }}
