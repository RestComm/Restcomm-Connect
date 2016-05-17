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
import org.apache.log4j.Logger;
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
    private static final Logger logger = Logger.getLogger(GeolocationEndpoint.class);
    private static final String ImmediateGT = Geolocation.GeolocationType.Immediate.toString();
    private static final String NotificationGT = Geolocation.GeolocationType.Notification.toString();

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
            if (!geolocationType.toString().equals(ImmediateGT) && !geolocationType.toString().equals(NotificationGT)) {
                validateGeolocationType(data);
            }
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        Geolocation geolocation = createFrom(new Sid(accountSid), data, geolocationType);

        if (geolocation.getResponseStatus() != null && geolocation.getResponseStatus().equalsIgnoreCase("rejected")) {
            logger.info("Geolocation ResponseStatus rejected or unauthorized for that Sid");
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
        Geolocation gl = validateCreateParams(accountSid, data, glType);

        if (gl == null) {
            gl = buildGeolocation(accountSid, data, glType); // *** All parameters with specified values validations passed ***
            return gl;
        } else {
            return gl;
        }

    }

    private Geolocation buildGeolocation(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {
        final Geolocation.Builder builder = Geolocation.builder();
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        String geoloctype = glType.toString();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
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
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation buildIncorrectGeolocationRequest(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, final Geolocation.Builder builder) {
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        String geoloctype = glType.toString();
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
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation rejectedGeolocationRequest(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, String cause) {
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setResponseStatus("rejected");
        builder.setCause(cause);
        return buildIncorrectGeolocationRequest(accountSid, data, glType, builder);
    }

    private Geolocation failedGeolocationRequest(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, String cause) {
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setResponseStatus("failed");
        builder.setCause(cause);
        return buildIncorrectGeolocationRequest(accountSid, data, glType, builder);
    }

    private Geolocation buildIncorrectGeolocationUpdateRequest(final Sid accountSid, final Sid sid,
            final MultivaluedMap<String, String> data, Geolocation.GeolocationType glType, final Geolocation.Builder builder) {
        String geoloctype = glType.toString();
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
                .append("/Geolocation/" + geoloctype + "/").append(sid.toString());
        builder.setUri(URI.create(buffer.toString()));
        return builder.build();
    }

    private Geolocation incorrectGeolocationUpdateRequest(Geolocation geolocation, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType, String responseStatus, String badUpdateCause) {
        final Sid accountSid = geolocation.getAccountSid();
        final Sid sid = geolocation.getSid();
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setResponseStatus(responseStatus);
        builder.setCause(badUpdateCause);
        return buildIncorrectGeolocationUpdateRequest(accountSid, sid, data, glType, builder);
    }

    private Geolocation validateCreateParams(final Sid accountSid, final MultivaluedMap<String, String> data,
            Geolocation.GeolocationType glType) {

        // *** DeviceIdentifier can not be null ***/
        try {
            if (!data.containsKey("DeviceIdentifier")) {
                return rejectedGeolocationRequest(accountSid, data, glType, "DeviceIdentifier value con not be null");
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DeviceIdentifier value");
        }

        // *** StatusCallback can not be null ***/
        try {
            if (!data.containsKey("StatusCallback")) {
                return rejectedGeolocationRequest(accountSid, data, glType, "StatusCallback value con not be null");
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for StatusCallback value");
        }

        // *** DesiredAccuracy must be API compliant: High, Average or Low***/
        try {
            if (data.containsKey("DesiredAccuracy")) {
                String desiredAccuracy = data.getFirst("DesiredAccuracy");
                if (!desiredAccuracy.equalsIgnoreCase("High") && !desiredAccuracy.equalsIgnoreCase("Average")
                        && !desiredAccuracy.equalsIgnoreCase("Low")) {
                    return rejectedGeolocationRequest(accountSid, data, glType, "DesiredAccuracy value not API compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DesiredAccuracy value");
        }

        // *** DeviceLatitude must be API compliant***/
        try {
            if (data.containsKey("DeviceLatitude")) {
                String deviceLat = data.getFirst("DeviceLatitude");
                Boolean devLatWGS84 = validateWGS84(deviceLat);
                if (!devLatWGS84) {
                    return failedGeolocationRequest(accountSid, data, glType, "DeviceLatitude not API compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DeviceLatitude value");
        }

        // *** DeviceLongitude must be API compliant***/
        try {
            if (data.containsKey("DeviceLongitude")) {
                String deviceLong = data.getFirst("DeviceLongitude");
                Boolean devLongWGS84 = validateWGS84(deviceLong);
                if (!devLongWGS84) {
                    return failedGeolocationRequest(accountSid, data, glType, "DeviceLongitude not API compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for DeviceLatitude value");
        }

        // *** GeofenceEvent must belong to Notification type of Geolocation, not null and API compliant: in, out or in-out***/
        try {
            if (!data.containsKey("GeofenceEvent") && glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceEvent value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceEvent") && !glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceEvent only applies for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceEvent") && glType.toString().equals(NotificationGT)) {
                String geofenceEvent = data.getFirst("GeofenceEvent");
                if (!geofenceEvent.equalsIgnoreCase("in") && !geofenceEvent.equalsIgnoreCase("out")
                        && !geofenceEvent.equalsIgnoreCase("in-out")) {
                    return rejectedGeolocationRequest(accountSid, data, glType, "GeofenceEvent value not API compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for GeofenceEvent value");
        }

        // *** EventGeofenceLatitude must belong to Notification type of Geolocation, not null and API compliant ***/
        try {
            if (!data.containsKey("EventGeofenceLatitude") && glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLatitude value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLatitude") && !glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLatitude only applies for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLatitude") && glType.toString().equals(NotificationGT)) {
                String eventGeofenceLat = data.getFirst("EventGeofenceLatitude");
                Boolean eventGeofenceLongWGS84 = validateWGS84(eventGeofenceLat);
                if (!eventGeofenceLongWGS84) {
                    return rejectedGeolocationRequest(accountSid, data, glType,
                            "EventGeofenceLatitude value not WGS84 compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for EventGeofenceLatitude value");
        }

        // *** EventGeofenceLongitude must belong to Notification type of Geolocation and must be API compliant ***/
        try {
            if (!data.containsKey("EventGeofenceLongitude") && glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLongitude value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLongitude") && !glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "EventGeofenceLongitude only applies for Notification type of Geolocation");
            } else if (data.containsKey("EventGeofenceLongitude") && glType.toString().equals(NotificationGT)) {
                String eventGeofenceLong = data.getFirst("EventGeofenceLongitude");
                Boolean eventGeofenceLongWGS84 = validateWGS84(eventGeofenceLong);
                if (!eventGeofenceLongWGS84) {
                    return rejectedGeolocationRequest(accountSid, data, glType,
                            "EventGeofenceLongitude value not WGS84 compliant");
                }
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for EventGeofenceLongitude value");
        }

        // *** GeofenceRange can not be null in Notification type of Geolocation***/
        try {
            if (!data.containsKey("GeofenceRange") && glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceRange value con not be null for Notification type of Geolocation");
            } else if (data.containsKey("GeofenceRange") && !glType.toString().equals(NotificationGT)) {
                return rejectedGeolocationRequest(accountSid, data, glType,
                        "GeofenceRange only applies for Notification type of Geolocation");
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "Exception for GeofenceRange value");
        }

        // *** LocationTimestamp must be API compliant: DateTime format only***/
        try {
            if (data.containsKey("LocationTimestamp")) {
                @SuppressWarnings("unused")
                DateTime locationTimestamp = getDateTime("LocationTimestamp", data);
            }
        } catch (Exception exception) {
            logger.info("Exception: " + exception.getMessage());
            return rejectedGeolocationRequest(accountSid, data, glType, "LocationTimestamp value is not API compliant");
        }

        return null;

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

        Geolocation updatedGeolocation = validateAndUpdateParams(geolocation, data);
        return updatedGeolocation;
    }

    private Geolocation validateAndUpdateParams(Geolocation geolocation, final MultivaluedMap<String, String> data) {

        Geolocation updatedGeolocation = geolocation;
        // *** Set of parameters with provided data for Geolocation update***//
        if (data.containsKey("Source")) {
            try {
                updatedGeolocation = updatedGeolocation.setSource(data.getFirst("Source"));
            } catch (Exception exception) {
                logger.info(
                        "Exception in updating \"source\" parameter for Geolocation update request: " + exception.getMessage());
            }
        }
        if (data.containsKey("DeviceIdentifier")) {
            try {
                updatedGeolocation = updatedGeolocation.setDeviceIdentifier(data.getFirst("DeviceIdentifier"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"deviceIdentifier\" parameter for Geolocation update request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("ResponseStatus")) {
            updatedGeolocation = updatedGeolocation.setResponseStatus(data.getFirst("ResponseStatus"));
            updatedGeolocation = updatedGeolocation.setDateUpdated(DateTime.now());
            if (data.containsKey("Cause") && (updatedGeolocation.getResponseStatus().equalsIgnoreCase("rejected")
                    || updatedGeolocation.getResponseStatus().equalsIgnoreCase("unauthorized")
                    || updatedGeolocation.getResponseStatus().equalsIgnoreCase("failed"))) {
                updatedGeolocation = updatedGeolocation.setCause(data.getFirst("Cause"));
                // cause is set to null if responseStatus is not rejected, failed or unauthorized
            }
            if (!updatedGeolocation.getResponseStatus().equalsIgnoreCase("rejected")
                    || !updatedGeolocation.getResponseStatus().equalsIgnoreCase("unauthorized")
                    || !updatedGeolocation.getResponseStatus().equalsIgnoreCase("failed")) {
                updatedGeolocation = updatedGeolocation.setCause(null);
                // cause is set to null if responseStatus is not rejected, failed or unauthorized
            }
        }
        if (updatedGeolocation.getResponseStatus() != null
                && (!updatedGeolocation.getResponseStatus().equalsIgnoreCase("unauthorized")
                        || !updatedGeolocation.getResponseStatus().equalsIgnoreCase("failed"))) {
            updatedGeolocation = updatedGeolocation.setCause(null);
            // "Cause" is set to null if "ResponseStatus" is not null and is neither "rejected", "unauthorized" nor "failed"
        }
        if (data.containsKey("CellId")) {
            try {
                updatedGeolocation = updatedGeolocation.setCellId(data.getFirst("CellId"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"cellId\" parameter for Geolocation request: " + exception.getMessage());
            }
        }
        if (data.containsKey("LocationAreaCode")) {
            try {
                updatedGeolocation = updatedGeolocation.setLocationAreaCode(data.getFirst("LocationAreaCode"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"location Area Code\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("MobileCountryCode")) {
            try {
                updatedGeolocation = updatedGeolocation.setMobileCountryCode(getInteger("MobileCountryCode", data));
            } catch (Exception exception) {
                logger.info("Exception in updating \"mobileCountryCode\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("MobileNetworkCode")) {
            try {
                updatedGeolocation = updatedGeolocation.setMobileNetworkCode(data.getFirst("MobileNetworkCode"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"mobileNetworkCode\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("NetworkEntityAddress")) {
            try {
                updatedGeolocation = updatedGeolocation.setNetworkEntityAddress(getLong("NetworkEntityAddress", data));
            } catch (Exception exception) {
                logger.info("Exception in updating \"networkEntityAddress\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("LocationAge")) {
            try {
                updatedGeolocation = updatedGeolocation.setAgeOfLocationInfo(getInteger("LocationAge", data));
            } catch (Exception exception) {
                logger.info(
                        "Exception in updating \"locationAge\" parameter for Geolocation request: " + exception.getMessage());
            }
        }
        if (data.containsKey("DeviceLatitude")) {
            try {
                String deviceLat = data.getFirst("DeviceLatitude");
                Boolean deviceLatWGS84 = validateWGS84(deviceLat);
                if (!deviceLatWGS84) {
                    return incorrectGeolocationUpdateRequest(geolocation, data, geolocation.getGeolocationType(), "failed",
                            "DeviceLatitude not WGS84 compliant");
                } else {
                    updatedGeolocation = updatedGeolocation.setDeviceLatitude(deviceLat);
                }
            } catch (Exception exception) {
                logger.info("Exception in updating \"deviceLatitude\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("DeviceLongitude")) {
            updatedGeolocation = updatedGeolocation.setDeviceLongitude(data.getFirst("DeviceLongitude"));
            try {
                String deviceLong = data.getFirst("DeviceLongitude");
                Boolean deviceLongGS84 = validateWGS84(deviceLong);
                if (!deviceLongGS84) {
                    return incorrectGeolocationUpdateRequest(geolocation, data, geolocation.getGeolocationType(), "failed",
                            "DeviceLongitude not WGS84 compliant");
                } else {
                    updatedGeolocation = updatedGeolocation.setDeviceLongitude(deviceLong);

                }
            } catch (Exception exception) {
                logger.info("Exception in updating \"deviceLongitude\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("Accuracy")) {
            try {
                updatedGeolocation = updatedGeolocation.setAccuracy(getLong("Accuracy", data));
            } catch (Exception exception) {
                logger.info("Exception in updating \"accuracy\" parameter for Geolocation request: " + exception.getMessage());
            }
        }
        if (data.containsKey("PhysicalAddress")) {
            try {
                updatedGeolocation = updatedGeolocation.setPhysicalAddress(data.getFirst("PhysicalAddress"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"physicalAddress\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("InternetAddress")) {
            try {
                updatedGeolocation = updatedGeolocation.setInternetAddress(data.getFirst("InternetAddress"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"internetAddress\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("FormattedAddress")) {
            try {
                updatedGeolocation = updatedGeolocation.setFormattedAddress(data.getFirst("FormattedAddress"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"formattedAddress\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("LocationTimestamp")) {
            try {
                updatedGeolocation = updatedGeolocation.setLocationTimestamp(getDateTime("LocationTimestamp", data));
            } catch (Exception exception) {
                DateTime locTimestamp = DateTime.parse("1900-01-01");
                updatedGeolocation = updatedGeolocation.setLocationTimestamp(locTimestamp);
            }
        }
        if (data.containsKey("EventGeofenceLatitude") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            try {
                String eventGeofenceLat = data.getFirst("EventGeofenceLatitude");
                Boolean eventGeofenceLatWGS84 = validateWGS84(eventGeofenceLat);
                if (!eventGeofenceLatWGS84) {
                    return incorrectGeolocationUpdateRequest(geolocation, data, geolocation.getGeolocationType(), "failed",
                            "EventGeofenceLatitude not WGS84 compliant");

                } else {
                    updatedGeolocation = updatedGeolocation.setEventGeofenceLatitude(eventGeofenceLat);

                }
            } catch (Exception exception) {
                logger.info("Exception in updating \"eventGeofenceLatitude\" parameter for Geolocation request: "
                        + exception.getMessage());
            }

        }
        if (data.containsKey("EventGeofenceLongitude") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            try {
                String eventGeofenceLong = data.getFirst("EventGeofenceLongitude");
                Boolean eventGeofenceLongWGS84 = validateWGS84(eventGeofenceLong);
                if (!eventGeofenceLongWGS84) {
                    return incorrectGeolocationUpdateRequest(geolocation, data, geolocation.getGeolocationType(), "failed",
                            "EventGeofenceLongitude not WGS84 compliant");
                } else {
                    updatedGeolocation = updatedGeolocation.setEventGeofenceLongitude(eventGeofenceLong);
                }
            } catch (Exception exception) {
                logger.info("Exception in updating \"eventGeofenceLongitude\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("Radius") && geolocation.getGeolocationType().toString().equals(NotificationGT)) {
            try {
                updatedGeolocation = updatedGeolocation.setRadius(getLong("Radius", data));
            } catch (Exception exception) {
                logger.info("Exception in updating \"radius\" parameter for Geolocation request: " + exception.getMessage());
            }
        }
        if (data.containsKey("GeolocationPositioningType")) {
            try {
                updatedGeolocation = updatedGeolocation
                        .setGeolocationPositioningType(data.getFirst("GeolocationPositioningType"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"GeolocationPositioningType\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        if (data.containsKey("LastGeolocationResponse")) {
            try {
                updatedGeolocation = updatedGeolocation.setLastGeolocationResponse(data.getFirst("LastGeolocationResponse"));
            } catch (Exception exception) {
                logger.info("Exception in updating \"lastGeolocationResponse\" parameter for Geolocation request: "
                        + exception.getMessage());
            }
        }
        DateTime thisDateTime = DateTime.now();
        updatedGeolocation = updatedGeolocation.setDateUpdated(thisDateTime);
        return updatedGeolocation;
    }

    private void validateGeolocationType(final MultivaluedMap<String, String> data) throws RuntimeException {
        // ** Validation of Geolocation POST requests with valid type**/
        throw new NullPointerException("Geolocation Type can not be null, but either Immediate or Notification.");

    }

    private boolean validateWGS84(String coordinates) {

        String degrees = "\\u00b0";
        String minutes = "'";
        Boolean WGS84_validation;
        Boolean pattern1 = coordinates.matches("[NWSE]{1}\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern2 = coordinates.matches("\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}[NWSE]{1}$");
        Boolean pattern3 = coordinates.matches("\\d{1,3}[" + degrees + "]\\d{1,3}[" + minutes + "]\\d{1,2}\\.\\d{1,2}["
                + minutes + "][" + minutes + "][NWSE]{1}$");
        Boolean pattern4 = coordinates.matches("[NWSE]{1}\\d{1,3}[" + degrees + "]\\d{1,3}[" + minutes + "]\\d{1,2}\\.\\d{1,2}["
                + minutes + "][" + minutes + "]$");
        Boolean pattern5 = coordinates.matches("\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern6 = coordinates.matches("-?\\d{1,3}\\s\\d{1,2}\\s\\d{1,2}\\.\\d{1,2}$");
        Boolean pattern7 = coordinates.matches("-?\\d+(\\.\\d+)?");

        if (pattern1 || pattern2 || pattern3 || pattern4 || pattern5 || pattern6 || pattern7) {
            WGS84_validation = true;
            return WGS84_validation;
        } else {
            WGS84_validation = false;
            return WGS84_validation;
        }
    }

}