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

package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readBigInteger;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readInteger;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readUri;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeDateTime;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeSid;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.writeUri;
import static org.mobicents.servlet.restcomm.dao.DaoUtils.readGeolocationType;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.GeolocationDao;
import org.mobicents.servlet.restcomm.entities.Geolocation;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
public class MybatisGeolocationDao implements GeolocationDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.GeolocationDao.";
    private final SqlSessionFactory sessions;

    public MybatisGeolocationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addGeolocation(Geolocation gl) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addGeolocation", toMap(gl));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Geolocation getGeolocation(Sid sid) {
        return getGeolocation(namespace + "getGeolocation", sid.toString());
    }

    private Geolocation getGeolocation(final String selector, final String parameter) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameter);
            if (result != null) {
                return toGeolocation(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Geolocation> getGeolocations(Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getGeolocations", accountSid.toString());
            final List<Geolocation> geolocations = new ArrayList<Geolocation>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    geolocations.add(toGeolocation(result));
                }
            }
            return geolocations;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeGeolocation(Sid sid) {
        removeGeolocations(namespace + "removeGeolocation", sid);
    }

    @Override
    public void removeGeolocations(final Sid accountSid) {
        removeGeolocations(namespace + "removeGeolocations", accountSid);
    }

    private void removeGeolocations(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateGeolocation(Geolocation gl) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateGeolocation", toMap(gl));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(Geolocation gl) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(gl.getSid()));
        map.put("date_created", writeDateTime(gl.getDateCreated()));
        map.put("date_updated", writeDateTime(gl.getDateUpdated()));
        map.put("date_executed", writeDateTime(gl.getDateExecuted()));
        map.put("account_sid", writeSid(gl.getAccountSid()));
        map.put("source", gl.getSource());
        map.put("device_identifier", gl.getDeviceIdentifier());
        map.put("geolocation_type", gl.getGeolocationType());
        map.put("response_status", gl.getResponseStatus());
        map.put("cell_id", gl.getCellId());
        map.put("location_area_code", gl.getLocationAreaCode());
        map.put("mobile_country_code", gl.getMobileCountryCode());
        map.put("mobile_network_code", gl.getMobileNetworkCode());
        map.put("network_entity_address", gl.getNetworkEntityAddress());
        map.put("age_of_location_info", gl.getAgeOfLocationInfo());
        map.put("device_latitude", gl.getDeviceLatitude());
        map.put("device_longitude", gl.getDeviceLongitude());
        map.put("accuracy", gl.getAccuracy());
        map.put("physical_address", gl.getPhysicalAddress());
        map.put("internet_address", gl.getInternetAddress());
        map.put("formatted_address", gl.getFormattedAddress());
        map.put("location_timestamp", writeDateTime(gl.getLocationTimestamp()));
        map.put("event_geofence_latitude", gl.getEventGeofenceLatitude());
        map.put("event_geofence_longitude", gl.getEventGeofenceLongitude());
        map.put("radius", gl.getRadius());
        map.put("geolocation_positioning_type", gl.getGeolocationPositioningType());
        map.put("last_geolocation_response", gl.getLastGeolocationResponse());
        map.put("cause", gl.getCause());
        map.put("api_version", gl.getApiVersion());
        map.put("uri", writeUri(gl.getUri()));
        return map;
    }

    private Geolocation toGeolocation(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime date_created = readDateTime(map.get("date_created"));
        final DateTime date_updated = readDateTime(map.get("date_updated"));
        final DateTime date_executed = readDateTime(map.get("date_executed"));
        final Sid account_sid = readSid(map.get("account_sid"));
        final String source = readString(map.get("source"));
        final String device_identifier = readString(map.get("device_identifier"));
        final Geolocation.GeolocationType geolocation_type = readGeolocationType(map.get("geolocation_type"));
        final String response_status = readString(map.get("response_status"));
        final String cell_id = readString(map.get("cell_id"));
        final String location_area_code = readString(map.get("location_area_code"));
        final Integer mobile_country_code = readInteger(map.get("mobile_country_code"));
        final String mobile_network_code = readString(map.get("mobile_network_code"));
        final BigInteger network_entity_address = readBigInteger(map.get("network_entity_address"));
        final Integer age_of_location_info = readInteger(map.get("age_of_location_info"));
        final String device_latitude = readString(map.get("device_latitude"));
        final String device_longitude = readString(map.get("device_longitude"));
        final BigInteger accuracy = readBigInteger(map.get("accuracy"));
        final String physical_address = readString(map.get("physical_address"));
        final String internet_address = readString(map.get("internet_address"));
        final String formatted_address = readString(map.get("formatted_address"));
        final DateTime location_timestamp = readDateTime(map.get("location_timestamp"));
        final String event_geofence_latitude = readString(map.get("event_geofence_latitude"));
        final String event_geofence_longitude = readString(map.get("event_geofence_longitude"));
        final BigInteger radius = readBigInteger(map.get("radius"));
        final String geolocation_positioning_type = readString(map.get("geolocation_positioning_type"));
        final String last_geolocation_response = readString(map.get("last_geolocation_response"));
        final String cause = readString(map.get("cause"));
        final String api_version = readString(map.get("api_version"));
        final URI uri = readUri(map.get("uri"));
        return new Geolocation(sid, date_created, date_updated, date_executed, account_sid, source, device_identifier,
                geolocation_type, response_status, cell_id, location_area_code, mobile_country_code, mobile_network_code,
                network_entity_address, age_of_location_info, device_latitude, device_longitude, accuracy, physical_address,
                internet_address, formatted_address, location_timestamp, event_geofence_latitude, event_geofence_longitude, radius,
                geolocation_positioning_type, last_geolocation_response, cause, api_version, uri);
    }

}
