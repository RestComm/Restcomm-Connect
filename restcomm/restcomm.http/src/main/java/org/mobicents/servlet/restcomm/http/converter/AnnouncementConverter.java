package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Announcement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

@ThreadSafe
public final class AnnouncementConverter extends AbstractConverter implements JsonSerializer<Announcement> {
    public AnnouncementConverter(final Configuration configuration) {
        super(configuration);
    }

    @Override
    public JsonElement serialize(final Announcement announcement, Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(announcement.getSid(), object);
        writeDateCreated(announcement.getDateCreated(), object);
        writeAccountSid(announcement.getAccountSid(), object);
        writeGender(announcement.getGender(), object);
        writeLanguage(announcement.getLanguage(), object);
        writeText(announcement.getText(), object);
        writeUri(announcement.getUri(), object);
        return object;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final Announcement announcement = (Announcement) object;
        writer.startNode("Announcement");
        writeSid(announcement.getSid(), writer);
        writeDateCreated(announcement.getDateCreated(), writer);
        writeAccountSid(announcement.getAccountSid(), writer);
        writeGender(announcement.getGender(), writer);
        writeLanguage(announcement.getLanguage(), writer);
        writeText(announcement.getText(), writer);
        writeUri(announcement.getUri(), writer);
        writer.endNode();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class klass) {
        return Announcement.class.equals(klass);
    }

    private void writeText(final String text, final JsonObject object) {
        object.addProperty("text", text);
    }

    private void writeLanguage(final String language, final JsonObject object) {
        object.addProperty("language", language);
    }

    private void writeGender(final String gender, final JsonObject object) {
        object.addProperty("gender", gender);
    }

    private void writeText(String text, HierarchicalStreamWriter writer) {
        writer.startNode("Text");
        if (text != null) {
            writer.setValue(text);
        }
        writer.endNode();
    }

    private void writeLanguage(String language, HierarchicalStreamWriter writer) {
        writer.startNode("Language");
        if (language != null) {
            writer.setValue(language);
        }
        writer.endNode();
    }

    private void writeGender(String gender, HierarchicalStreamWriter writer) {
        writer.startNode("Gender");
        if (gender != null) {
            writer.setValue(gender);
        }
        writer.endNode();
    }

}
