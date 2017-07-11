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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.entities.Transcription;
import org.restcomm.connect.dao.entities.TranscriptionList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Type;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class TranscriptionListConverter extends AbstractConverter implements JsonSerializer<TranscriptionList> {
    Integer page, pageSize, total;
    String pathUri;

    public TranscriptionListConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return TranscriptionList.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final TranscriptionList list = (TranscriptionList) object;
        writer.startNode("Transcriptions");
        for (final Transcription transcription : list.getTranscriptions()) {
            context.convertAnother(transcription);
        }
        writer.endNode();
    }

    @Override
    public JsonObject serialize(TranscriptionList transList, Type type, JsonSerializationContext context) {

        JsonObject result = new JsonObject();

        JsonArray array = new JsonArray();
        for (Transcription cdr : transList.getTranscriptions()) {
            array.add(context.serialize(cdr));
        }

        if (total != null && pageSize != null && page != null) {
            result.addProperty("page", page);
            result.addProperty("num_pages", getTotalPages());
            result.addProperty("page_size", pageSize);
            result.addProperty("total", total);
            result.addProperty("start", getFirstIndex());
            result.addProperty("end", getLastIndex(transList));
            result.addProperty("uri", pathUri);
            result.addProperty("first_page_uri", getFirstPageUri());
            result.addProperty("previous_page_uri", getPreviousPageUri());
            result.addProperty("next_page_uri", getNextPageUri(transList));
            result.addProperty("last_page_uri", getLastPageUri());
        }

        result.add("transcriptions", array);

        return result;
    }

    private int getTotalPages() {
        return total / pageSize;
    }

    private String getFirstIndex() {
        return String.valueOf(page * pageSize);
    }

    private String getLastIndex(TranscriptionList list) {
        return String.valueOf((page == getTotalPages()) ? (page * pageSize) + list.getTranscriptions().size()
                : (pageSize - 1) + (page * pageSize));
    }

    private String getFirstPageUri() {
        return pathUri + "?Page=0&PageSize=" + pageSize;
    }

    private String getPreviousPageUri() {
        return ((page == 0) ? "null" : pathUri + "?Page=" + (page - 1) + "&PageSize=" + pageSize);
    }

    private String getNextPageUri(TranscriptionList list) {
        String lastSid = (page == getTotalPages()) ? "null" : list.getTranscriptions().get(pageSize - 1).getSid().toString();
        return (page == getTotalPages()) ? "null"
                : pathUri + "?Page=" + (page + 1) + "&PageSize=" + pageSize + "&AfterSid=" + lastSid;
    }

    private String getLastPageUri() {
        return pathUri + "?Page=" + getTotalPages() + "&PageSize=" + pageSize;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setCount(Integer count) {
        this.total = count;
    }

    public void setPathUri(String pathUri) {
        this.pathUri = pathUri;
    }
}
