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

package org.mobicents.servlet.restcomm.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.apache.shiro.authz.AuthorizationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.GeolocationDao;
import org.mobicents.servlet.restcomm.entities.Geolocation;
import org.mobicents.servlet.restcomm.entities.Geolocation.GeolocationType;
import org.mobicents.servlet.restcomm.entities.GeolocationList;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.ClientListConverter;
import org.mobicents.servlet.restcomm.http.converter.GeolocationConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
@NotThreadSafe
public abstract class GeolocationEndpoint extends AbstractEndpoint {

    @Context
    protected ServletContext context;
    protected Configuration configuration;
    protected GeolocationDao dao;
    protected Gson gson;
    protected XStream xstream;
    protected AccountsDao accountsDao;

    public GeolocationEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        dao = storage.getGeolocationDao();
        accountsDao = storage.getAccountsDao();
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = configuration.subset("runtime-settings");
        super.init(configuration);
        final GeolocationConverter converter = new GeolocationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Geolocation.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ClientListConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    public Response putGeolocation(final String accountSid, final MultivaluedMap<String, String> data,
            GeolocationType geolocationType, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:Geolocation");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        Geolocation geolocation = createFrom(new Sid(accountSid), data, geolocationType);
        dao.addGeolocation(geolocation);

        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(geolocation);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    private Geolocation createFrom(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        builder.setSid(sid);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        /*
         * if (data.containsKey("GeolocationType")) {
         * builder.setGeolocationType(Geolocation.GeolocationType.getValueOf(data.getFirst("GeolocationType"))); } else {
         * builder.setGeolocationType(glType); }
         */
        builder.setGeolocationType(glType);
        builder.setResponseStatus(data.getFirst("ResponseStatus"));
        builder.setCellId(data.getFirst("CellId"));
        builder.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        builder.setMobileCountryCode(getInteger("MobileCountryCode", data));
        builder.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        builder.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        builder.setAgeOfLocationInfo(getInteger("LocationAge", data));
        builder.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        builder.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        builder.setAccuracy(getLong("Accuracy", data));
        builder.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        builder.setInternetAddress(data.getFirst("InternetAddress"));
        builder.setFormattedAddress(data.getFirst("FormattedAddress"));
        builder.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        builder.setEventGeofenceLatitude(data.getFirst("EventGeofenceLatitude"));
        builder.setEventGeofenceLongitude(data.getFirst("EventGeofenceLongitude"));
        builder.setRadius(getLong("Radius", data));
        builder.setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
        builder.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
        builder.setCause(data.getFirst("Cause"));
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Geolocation/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    protected Response getGeolocation(final String accountSid, final String sid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Geolocation");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final Geolocation geolocation = dao.getGeolocation(new Sid(sid));
        if (geolocation == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(accountsDao, accountSid, String.valueOf(geolocation.getAccountSid()));
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response getGeolocations(final String accountSid, final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Read:Geolocation");
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        final List<Geolocation> geolocations = dao.getGeolocations(new Sid(accountSid));
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new GeolocationList(geolocations));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(geolocations), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response deleteGeolocation(final String accountSid, final String sid) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Delete:Geolocation");
            Geolocation geolocation = dao.getGeolocation(new Sid(sid));
            if (geolocation != null) {
                secureLevelControl(accountsDao, accountSid, String.valueOf(geolocation.getAccountSid()));
            }
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        dao.removeGeolocation(new Sid(sid));
        return ok().build();
    }

    protected Response updateGeolocation(final String accountSid, final String sid, final MultivaluedMap<String, String> data,
            final MediaType responseType) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Modify:Geolocation");
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        Geolocation geolocation = dao.getGeolocation(new Sid(sid));
        if (geolocation == null) {
            return status(NOT_FOUND).build();
        } else {
            try {
                secureLevelControl(accountsDao, accountSid, String.valueOf(geolocation.getAccountSid()));
            } catch (final AuthorizationException exception) {
                return status(UNAUTHORIZED).build();
            }
            geolocation = update(geolocation, data);
            dao.updateGeolocation(geolocation);
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    private Geolocation update(final Geolocation geolocation, final MultivaluedMap<String, String> data) {

        Geolocation result = geolocation;

        if (data.containsKey("Source")) {
            result = result.setSource(data.getFirst("Source"));
        }

        if (data.containsKey("DeviceIdentifier")) {
            result = result.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        }

        /*
         * if (data.containsKey("GeolocationType")) { result =
         * result.setGeolocationType(Geolocation.GeolocationType.getValueOf(data.getFirst("GeolocationType"))); }
         */

        if (data.containsKey("ResponseStatus")) {
            result = result.setResponseStatus(data.getFirst("ResponseStatus"));
        }

        if (data.containsKey("CellId")) {
            result = result.setCellId(data.getFirst("CellId"));
        }

        if (data.containsKey("LocationAreaCode")) {
            result = result.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        }

        if (data.containsKey("MobileCountryCode")) {
            result = result.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }

        if (data.containsKey("MobileNetworkCode")) {
            result = result.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        }

        if (data.containsKey("NetworkEntityAddress")) {
            result = result.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        }

        if (data.containsKey("LocationAge")) {
            result = result.setAgeOfLocationInfo(getInteger("LocationAge", data));
        }

        if (data.containsKey("DeviceLatitude")) {
            result = result.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        }

        if (data.containsKey("DeviceLongitude")) {
            result = result.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        }

        if (data.containsKey("Accuracy")) {
            result = result.setAccuracy(getLong("Accuracy", data));
        }

        if (data.containsKey("PhysicalAddress")) {
            result = result.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        }

        if (data.containsKey("InternetAddress")) {
            result = result.setInternetAddress(data.getFirst("InternetAddress"));
        }

        if (data.containsKey("FormattedAddress")) {
            result = result.setFormattedAddress(data.getFirst("FormattedAddress"));
        }

        if (data.containsKey("LocationTimestamp")) {
            result = result.setLocationTimeStamp(getDateTime("LocationTimestamp", data));
        }

        if (data.containsKey("EventGeofenceLatitude")) {
            result = result.setEventGeofenceLatitude(data.getFirst("EventGeofenceLatitude"));
        }

        if (data.containsKey("EventGeofenceLongitude")) {
            result = result.setEventGeofenceLongitude(data.getFirst("EventGeofenceLongitude"));
        }

        if (data.containsKey("Radius")) {
            result = result.setRadius(getLong("Radius", data));
        }

        if (data.containsKey("GeolocationPositioningType")) {
            result = result.setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
        }

        if (data.containsKey("LastGeolocationResponse")) {
            result = result.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
        }

        if (data.containsKey("Cause")) {
            result = result.setCause(data.getFirst("Cause"));
        }

        return result;
    }

    private void validate(final MultivaluedMap<String, String> data) throws RuntimeException {
        if (!data.containsKey("Source")) {
            throw new NullPointerException("Source can not be null.");
        } else if (!data.containsKey("DeviceIdentifier")) {
            throw new NullPointerException("DeviceIdentifier can not be null.");
        }
    }
}
