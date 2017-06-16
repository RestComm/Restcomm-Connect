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
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class IncomingPhoneNumberListConverter extends AbstractConverter
        implements JsonSerializer<IncomingPhoneNumberList> {

    Integer page, pageSize, total;
    String pathUri;

    public IncomingPhoneNumberListConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return IncomingPhoneNumberList.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final IncomingPhoneNumberList list = (IncomingPhoneNumberList) object;
        writer.startNode("IncomingPhoneNumbers");
        writer.addAttribute("page", String.valueOf(page));
        writer.addAttribute("numpages", String.valueOf(getTotalPages()));
        writer.addAttribute("pagesize", String.valueOf(pageSize));
        writer.addAttribute("total", String.valueOf(getTotalPages()));
        writer.addAttribute("start", getFirstIndex());
        writer.addAttribute("end", getLastIndex(list));
        writer.addAttribute("uri", pathUri);
        writer.addAttribute("firstpageuri", getFirstPageUri());
        writer.addAttribute("previouspageuri", getPreviousPageUri());
        writer.addAttribute("nextpageuri", getNextPageUri(list));
        writer.addAttribute("lastpageuri", getLastPageUri());
        for (final IncomingPhoneNumber incomingPhoneNumber : list.getIncomingPhoneNumbers()) {
            context.convertAnother(incomingPhoneNumber);
        }
        writer.endNode();
    }

    @Override
    public JsonObject serialize(IncomingPhoneNumberList list, Type type, JsonSerializationContext context) {

        JsonObject result = new JsonObject();

        JsonArray array = new JsonArray();
        for (IncomingPhoneNumber phoneNumber : list.getIncomingPhoneNumbers()) {
            array.add(context.serialize(phoneNumber));
        }

        if (total != null && pageSize != null && page != null) {
            result.addProperty("page", page);
            result.addProperty("num_pages", getTotalPages());
            result.addProperty("page_size", pageSize);
            result.addProperty("total", total);
            result.addProperty("start", getFirstIndex());
            result.addProperty("end", getLastIndex(list));
            result.addProperty("uri", pathUri);
            result.addProperty("first_page_uri", getFirstPageUri());
            result.addProperty("previous_page_uri", getPreviousPageUri());
            result.addProperty("next_page_uri", getNextPageUri(list));
            result.addProperty("last_page_uri", getLastPageUri());
        }

        result.add("incomingPhoneNumbers", array);

        return result;
    }

    private int getTotalPages() {
        return total / pageSize;
    }

    private String getFirstIndex() {
        return String.valueOf(page * pageSize);
    }

    private String getLastIndex(IncomingPhoneNumberList list) {
        return String.valueOf((page == getTotalPages()) ? (page * pageSize) + list.getIncomingPhoneNumbers().size()
                : (pageSize - 1) + (page * pageSize));
    }

    private String getFirstPageUri() {
        return pathUri + "?Page=0&PageSize=" + pageSize;
    }

    private String getPreviousPageUri() {
        return ((page == 0) ? "null" : pathUri + "?Page=" + (page - 1) + "&PageSize=" + pageSize);
    }

    private String getNextPageUri(IncomingPhoneNumberList list) {
        String lastSid = (page == getTotalPages()) ? "null"
                : list.getIncomingPhoneNumbers().get(pageSize - 1).getSid().toString();
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
