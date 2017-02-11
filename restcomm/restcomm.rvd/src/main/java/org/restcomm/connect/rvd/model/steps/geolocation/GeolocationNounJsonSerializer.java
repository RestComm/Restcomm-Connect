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

package org.restcomm.connect.rvd.model.steps.geolocation;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationNounJsonSerializer implements JsonSerializer<GeolocationNoun> {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    @Override
    public JsonElement serialize(GeolocationNoun noun, Type arg1, JsonSerializationContext arg2) {
        Gson gson = new GsonBuilder().registerTypeAdapter(GeolocationNoun.class, new GeolocationNounJsonSerializer()).create();
        JsonElement resultElement = null; // TODO update this default value to something or throw an exception or something
        if (noun.getClass().equals(NotificationGeolocationNoun.class)) {
            resultElement = gson.toJsonTree((NotificationGeolocationNoun) noun);
        } else if (noun.getClass().equals(ImmediateGeolocationNoun.class)) {
            resultElement = gson.toJsonTree((ImmediateGeolocationNoun) noun);
        }

        return resultElement;
    }

}
