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
import org.joda.time.DateTime;
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
            if (!geolocationType.toString().equalsIgnoreCase("Immediate")
                    && !geolocationType.toString().equalsIgnoreCase("Notification")) {
                validateGeolocationType(data);
            }
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        Geolocation geolocation = createFrom(new Sid(accountSid), data, geolocationType);

        if (geolocation.getResponseStatus() != null && geolocation.getResponseStatus().equalsIgnoreCase("rejected")) {
            System.out.println("Geolocation ResponseStatus rejected or unauthorized for that Sid");
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(geolocation);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(geolocation), APPLICATION_JSON).build();
            } else {
                return null;
            }
        } else {

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
    }

    private Geolocation createFrom(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {

        // *** Validation of not null and specific formatted parameters *** //

        // *** Source can not be null ***/
        try {
            if (!data.containsKey("Source")) {
                return rejectedGeolocationRequest(accountSid, data, glType, "Source value con not be null");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for Source value");
        }

        // *** DeviceIdentifier can not be null ***/
        try {
            if (!data.containsKey("DeviceIdentifier")) {
                return rejectedGeolocationRequest(accountSid, data, glType, "DeviceIdentifier value con not be null");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DeviceIdentifier value");
        }

        // *** StatusCallback can not be null ***/
        try {
            if (!data.containsKey("StatusCallback")) {
                return rejectedGeolocationRequest(accountSid, data, glType, "StatusCallback value con not be null");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for StatusCallback value");
        }

        // *** DesiredAccuracy can not be null and must be API compliant: High, Average or Low***/
        try {
            if (data.containsKey("DesiredAccuracy")) {
                String desiredAccuracy = data.getFirst("DesiredAccuracy");
                if (!desiredAccuracy.equalsIgnoreCase("High") && !desiredAccuracy.equalsIgnoreCase("Average")
                        && !desiredAccuracy.equalsIgnoreCase("Low")) {
                    return rejectedGeolocationRequest(accountSid, data, glType, "DesiredAccuracy value not API compliant");
                }
            } else {
                return rejectedGeolocationRequest(accountSid, data, glType, "DesiredAccuracy can not be null");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DesiredAccuracy value");
        }

        // *** GeofenceEvent must belong to Notification type of Geolocation, not null and API compliant: in, out or in-out***/
        try {
            if (!data.containsKey("GeofenceEvent") && glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceEvent value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceEvent") && !glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceEvent only applies for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceEvent") && glType.toString().equalsIgnoreCase("Notification")) {
                String geofenceEvent = data.getFirst("GeofenceEvent");
                if (!geofenceEvent.equalsIgnoreCase("in") && !geofenceEvent.equalsIgnoreCase("out")
                        && !geofenceEvent.equalsIgnoreCase("in-out")) {
                        return rejectedGeolocationRequest(accountSid, data, glType, "GeofenceEvent value not API compliant");
                }
            }
        } catch (Exception exception) {
            System.out.println("Exception: "+exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for GeofenceEvent value");
        }

        // *** EventGeofenceLatitude must belong to Notification type of Geolocation, not null and API compliant: WGS84 only***/
        try {
            if (!data.containsKey("EventGeofenceLatitude") && glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLatitude value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLatitude") && !glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLatitude only applies for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLatitude") && glType.toString().equalsIgnoreCase("Notification")) {
                String eventGeofenceLat = data.getFirst("EventGeofenceLatitude");
                Boolean eventGeofenceLongWGS84 = validateWGS84(eventGeofenceLat);
                if (!eventGeofenceLongWGS84) {
                    return rejectedGeolocationRequest(accountSid, data, glType,
                            "EventGeofenceLatitude value not WGS84 compliant");
                }
            }
        } catch (Exception exception) {
                System.out.println("Exception: "+exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for EventGeofenceLatitude value");
        }

        // *** EventGeofenceLongitude must belong to Notification type of Geolocation and must be API compliant: WGS84 only ***/
        try {
            if (!data.containsKey("EventGeofenceLongitude") && glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLongitude value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLongitude") && !glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLongitude only applies for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLongitude") && glType.toString().equalsIgnoreCase("Notification")) {
                String eventGeofenceLong = data.getFirst("EventGeofenceLongitude");
                Boolean eventGeofenceLongWGS84 = validateWGS84(eventGeofenceLong);
                if (!eventGeofenceLongWGS84) {
                    return rejectedGeolocationRequest(accountSid, data, glType,
                            "EventGeofenceLongitude value not WGS84 compliant");
                }
            }
        } catch (Exception exception) {
                System.out.println("Exception: "+exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for EventGeofenceLongitude value");
        }

        // *** GeofenceRange can not be null in Notification type of Geolocation***/
        try {
            if (!data.containsKey("GeofenceRange") && glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceRange value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceRange") && !glType.toString().equalsIgnoreCase("Notification")) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceRange only applies for Notification type of Geolocation");
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for GeofenceRange value");
        }

        // *** LocationTimestamp must be API compliant: DateTime format only***/
        try {
            if (data.containsKey("LocationTimestamp")) {
                DateTime locationTimestamp = getDateTime("LocationTimestamp", data);
                System.out.println("locationTimestamp value: " + locationTimestamp);
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "LocationTimestamp value not API compliant");
        }

        // *** All parameters with specified values validations passed *** //
        Geolocation gl = buildGeolocation(accountSid, data, glType);
        return gl;
    }

    private Geolocation buildGeolocation(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        System.out.println("GeolocationEndpoint buildGeolocation, currentDateTime: " + currentDateTime);
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setGeolocationType(glType);
        builder.setResponseStatus(data.getFirst("ResponseStatus"));
        builder.setCause(data.getFirst("Cause"));
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
        builder.setApiVersion(getApiVersion(data));
        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(data)).append("/Accounts/").append(accountSid.toString())
                .append("/Geolocation/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation buildDeniedGeolocationRequest(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, final Geolocation.Builder builder) {
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource(data.getFirst("Source"));
        builder.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        builder.setGeolocationType(glType);
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
            } catch (final NullPointerException exception) {
                return status(BAD_REQUEST).entity(exception.getMessage()).build();
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

        Geolocation updatedGeolocation = geolocation;

        // *** Validation of already rejected or unauthorized Geolocations ***//
        if ((geolocation.getResponseStatus() != null && geolocation.getResponseStatus().equalsIgnoreCase("rejected"))
                || (geolocation.getResponseStatus() != null
                        && geolocation.getResponseStatus().equalsIgnoreCase("unauthorized"))) {
            geolocation.setDateUpdated(DateTime.now());
            return geolocation;
        }

        // *** Set of parameters with provided data for Geolocation update***//
        if (data.containsKey("Source")) {
            updatedGeolocation = updatedGeolocation.setSource(data.getFirst("Source"));
        }
        if (data.containsKey("DeviceIdentifier")) {
            updatedGeolocation = updatedGeolocation.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
        }
        if (data.containsKey("ResponseStatus")) {
            updatedGeolocation = updatedGeolocation.setResponseStatus(data.getFirst("ResponseStatus"));
        }
        if (data.containsKey("CellId")) {
            updatedGeolocation = updatedGeolocation.setCellId(data.getFirst("CellId"));
        }
        if (data.containsKey("LocationAreaCode")) {
            updatedGeolocation = updatedGeolocation.setLocationAreaCode(data.getFirst("LocationAreaCode"));
        }
        if (data.containsKey("MobileCountryCode")) {
            updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
        }
        if (data.containsKey("MobileNetworkCode")) {
            updatedGeolocation = updatedGeolocation.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
        }
        if (data.containsKey("NetworkEntityAddress")) {
            updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
        }
        if (data.containsKey("LocationAge")) {
            updatedGeolocation = updatedGeolocation.setAgeOfLocationInfo(getInteger("LocationAge", data));
        }
        if (data.containsKey("DeviceLatitude")) {
            updatedGeolocation = updatedGeolocation.setDeviceLatitude(data.getFirst("DeviceLatitude"));
        }
        if (data.containsKey("DeviceLongitude")) {
            updatedGeolocation = updatedGeolocation.setDeviceLongitude(data.getFirst("DeviceLongitude"));
        }
        if (data.containsKey("Accuracy")) {
            updatedGeolocation = updatedGeolocation.setAccuracy(getLong("Accuracy", data));
        }
        if (data.containsKey("PhysicalAddress")) {
            updatedGeolocation = updatedGeolocation.setPhysicalAddress(data.getFirst("PhysicalAddress"));
        }
        if (data.containsKey("InternetAddress")) {
            updatedGeolocation = updatedGeolocation.setInternetAddress(data.getFirst("InternetAddress"));
        }
        if (data.containsKey("FormattedAddress")) {
            updatedGeolocation = updatedGeolocation.setFormattedAddress(data.getFirst("FormattedAddress"));
        }
        if (data.containsKey("LocationTimestamp")) {
            updatedGeolocation = updatedGeolocation.setLocationTimestamp(getDateTime("LocationTimestamp", data));
        }
        if (data.containsKey("EventGeofenceLatitude")
                && geolocation.getGeolocationType().toString().equalsIgnoreCase("Notification")) {
            updatedGeolocation = updatedGeolocation.setEventGeofenceLatitude(data.getFirst("EventGeofenceLatitude"));
        }
        if (data.containsKey("EventGeofenceLongitude")
                && geolocation.getGeolocationType().toString().equalsIgnoreCase("Notification")) {
            updatedGeolocation = updatedGeolocation.setEventGeofenceLongitude(data.getFirst("EventGeofenceLongitude"));
        }
        if (data.containsKey("Radius") && geolocation.getGeolocationType().toString().equalsIgnoreCase("Notification")) {
            updatedGeolocation = updatedGeolocation.setRadius(getLong("Radius", data));
        }
        if (data.containsKey("GeolocationPositioningType")) {
            updatedGeolocation = updatedGeolocation.setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
        }
        if (data.containsKey("LastGeolocationResponse")) {
            updatedGeolocation = updatedGeolocation.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
        }
        if (data.containsKey("Cause") && geolocation.getCause() != null) {
            updatedGeolocation = updatedGeolocation.setCause(data.getFirst("Cause"));
        }
        DateTime thisDateTime = DateTime.now();
        System.out.println("GeolocationEndpoint, DateTime.now(): " + thisDateTime);
        updatedGeolocation = updatedGeolocation.setDateUpdated(thisDateTime);
        System.out.println(
                "GeolocationEndpoint update, updatedGeolocation.getDateUpdated(): " + updatedGeolocation.getDateUpdated());
        return updatedGeolocation;
    }

    private void validateGeolocationType(final MultivaluedMap<String, String> data) throws RuntimeException {
        // ** Validation of Geolocation POST requests with valid type**/
        throw new NullPointerException("Geolocation Type can not be null, but either Immediate or Notification.");

    }


    private boolean validateWGS84(String coordinates) {

        String degrees = "°";
        String minutes = "'";
        Boolean WGS84_validation;
        Boolean pattern1 = coordinates.matches("[NWSE]{1}\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern2 = coordinates.matches("\\d{1,3}[" + degrees + "]\\d{1,3}[" + minutes + "]\\d{1,2}\\.\\d{1,2}["
                + minutes + "][" + minutes + "][NWSE]{1}$");
        Boolean pattern3 = coordinates.matches("\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern4 = coordinates.matches("-?\\d+(\\.\\d+)?");

        if (pattern1 || pattern2 || pattern3 || pattern4) {
            WGS84_validation = true;
            System.out.println("Coordinates " + coordinates + " are WGS84 compliant");
            return WGS84_validation;
        } else {
            WGS84_validation = false;
            System.out.println("Coordinates " + coordinates + " are NOT WGS84 compliant");
            return WGS84_validation;
        }
    }

    private Geolocation rejectedGeolocationRequest(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, String cause) {
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setResponseStatus("rejected");
        builder.setCause(cause);
        return buildDeniedGeolocationRequest(accountSid, data, glType, builder);
    }
}
