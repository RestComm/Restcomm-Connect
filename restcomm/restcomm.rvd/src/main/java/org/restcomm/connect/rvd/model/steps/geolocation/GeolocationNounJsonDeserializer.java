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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationNounJsonDeserializer implements JsonDeserializer<GeolocationNoun> {
    static final Logger logger = Logger.getLogger(GeolocationNounJsonDeserializer.class.getName());

    @Override
    public GeolocationNoun deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {

        JsonObject noun_object = element.getAsJsonObject();
        String geolocationType = noun_object.get("geolocationType").getAsString();

        Gson gson = new GsonBuilder().create();

        GeolocationNoun noun;
        if ("Notification".equalsIgnoreCase(geolocationType)) {
            noun = gson.fromJson(noun_object, NotificationGeolocationNoun.class);
        } else if ("Immediate".equalsIgnoreCase(geolocationType)) {
            noun = gson.fromJson(noun_object, ImmediateGeolocationNoun.class);
        } else {
            noun = null;
            logger.error(
                    "Error deserializing geolocation noun. Noun found '" +geolocationType+ "' is undefined for Geolocation!");
        }

        return noun;
    }

}
