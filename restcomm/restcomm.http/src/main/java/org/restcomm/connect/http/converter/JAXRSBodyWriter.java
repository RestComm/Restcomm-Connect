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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.AccountList;
import org.restcomm.connect.dao.entities.AnnouncementList;
import org.restcomm.connect.dao.entities.ApplicationList;
import org.restcomm.connect.dao.entities.ApplicationNumberSummary;
import org.restcomm.connect.dao.entities.AvailablePhoneNumber;
import org.restcomm.connect.dao.entities.AvailablePhoneNumberList;
import org.restcomm.connect.dao.entities.RestCommResponse;

@Provider
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class JAXRSBodyWriter implements MessageBodyWriter<Object> {

    private static final Map<Class, AbstractConverter> converters = new HashMap();
    private static Gson gson = null;
    private static XStream xstream = null;

    @PostConstruct
    public void init() {
        if (gson == null) {

            Configuration rootConfiguration = (Configuration) context.getAttribute(Configuration.class.getName());
            Configuration runtimeConfiguration = rootConfiguration.subset("runtime-settings");

            converters.put(Account.class, new AccountConverter(runtimeConfiguration));
            converters.put(AccountList.class, new AccountListConverter(runtimeConfiguration));
            converters.put(RestCommResponse.class, new RestCommResponseConverter(runtimeConfiguration));
            converters.put(AnnouncementList.class, new AnnouncementListConverter(runtimeConfiguration));
            converters.put(ApplicationList.class, new ApplicationListConverter(runtimeConfiguration));
            converters.put(ApplicationNumberSummary.class, new ApplicationNumberSummaryConverter(runtimeConfiguration));
            converters.put(AvailablePhoneNumber.class, new AvailablePhoneNumberConverter(runtimeConfiguration));
            converters.put(AvailablePhoneNumberList.class, new AvailablePhoneNumberListConverter(runtimeConfiguration));

            xstream = new XStream();
            xstream.alias("RestcommResponse", RestCommResponse.class);
            xstream.alias("Number", ApplicationNumberSummary.class);

            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();

            for (Class cAux : converters.keySet()) {
                builder.registerTypeAdapter(cAux, converters.get(cAux));
                xstream.registerConverter(converters.get(cAux));
            }

            gson = builder.create();

        }
    }

    @Context
    protected ServletContext context;

    @Override
    public long getSize(Object t, Class type, Type type1, Annotation[] antns, MediaType mt) {
        return -1;
    }

    @Override
    public void writeTo(Object t, Class type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap mm, OutputStream out) throws IOException, WebApplicationException {
        if (mt.equals(MediaType.APPLICATION_JSON_TYPE)) {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));
            gson.toJson(t, type1, writer);
        } else {
            xstream.toXML(t, out);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return converters.containsKey(type);
    }
}
