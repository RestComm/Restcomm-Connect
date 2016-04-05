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

import java.io.InputStream;
import java.net.URI;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;

import org.mobicents.servlet.restcomm.dao.GeolocationDao;
import org.mobicents.servlet.restcomm.entities.Geolocation;
import org.mobicents.servlet.restcomm.entities.Geolocation.GeolocationType;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
public class GeolocationsDaoTest {

    private static MybatisDaoManager manager;

    public GeolocationsDaoTest() {
        super();
    }

    @Before
    public void before() {
        final InputStream data = getClass().getResourceAsStream("/mybatis.xml");
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final SqlSessionFactory factory = builder.build(data);
        manager = new MybatisDaoManager();
        manager.start(factory);
    }

    @After
    public void after() {
        manager.shutdown();
    }

    @Test
    public void geolocationCreateReadUpdateDelete() {

        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource("mlpclient1");
        builder.setDeviceIdentifier("device1");
        builder.setGeolocationType(GeolocationType.Immediate);
        builder.setResponseStatus("successfull");
        builder.setCause("NA");
        builder.setCellId("12345");
        builder.setLocationAreaCode("978");
        builder.setMobileCountryCode(748);
        builder.setMobileNetworkCode("03");
        builder.setNetworkEntityAddress((long) 59848779);
        builder.setAgeOfLocationInfo(0);
        builder.setDeviceLatitude("105.638643");
        builder.setDeviceLongitude("172.4394389");
        builder.setAccuracy((long) 7845);
        builder.setInternetAddress("2001:0:9d38:6ab8:30a5:1c9d:58c6:5898");
        builder.setPhysicalAddress("D8-97-BA-19-02-D8");
        builder.setFormattedAddress("Avenida Brasil 2681, 11500, Montevideo, Uruguay");
        builder.setLocationTimestamp(currentDateTime);
        builder.setEventGeofenceLatitude("-33.426280");
        builder.setEventGeofenceLongitude("-70.566560");
        builder.setRadius((long) 1500);
        builder.setGeolocationPositioningType("Network");
        builder.setLastGeolocationResponse("true");
        builder.setApiVersion("2012-04-24");
        builder.setUri(url);
        Geolocation geolocation = builder.build();
        final GeolocationDao geolocations = manager.getGeolocationDao();

        // Create a new Geolocation in the data store.
        geolocations.addGeolocation(geolocation);

        // Read the Geolocation from the data store.
        Geolocation result = geolocations.getGeolocation(sid);

        // Validate the results.
        assertTrue(result.getSid().equals(geolocation.getSid()));
        assertTrue(result.getAccountSid().equals(geolocation.getAccountSid()));
        assertTrue(result.getDateUpdated().equals(geolocation.getDateUpdated()));
        assertTrue(result.getSource().equals(geolocation.getSource()));
        assertTrue(result.getDeviceIdentifier().equals(geolocation.getDeviceIdentifier()));
        assertTrue(result.getGeolocationType().equals(geolocation.getGeolocationType()));
        assertTrue(result.getResponseStatus().equals(geolocation.getResponseStatus()));
        assertTrue(result.getCause() == geolocation.getCause());
        assertTrue(result.getCellId().equals(geolocation.getCellId()));
        assertTrue(result.getLocationAreaCode().equals(geolocation.getLocationAreaCode()));
        assertTrue(result.getMobileCountryCode().equals(geolocation.getMobileCountryCode()));
        assertTrue(result.getMobileNetworkCode().equals(geolocation.getMobileNetworkCode()));
        assertTrue(result.getNetworkEntityAddress().equals(geolocation.getNetworkEntityAddress()));
        assertTrue(result.getAgeOfLocationInfo().equals(geolocation.getAgeOfLocationInfo()));
        assertTrue(result.getDeviceLatitude().equals(geolocation.getDeviceLatitude()));
        assertTrue(result.getDeviceLongitude().equals(geolocation.getDeviceLongitude()));
        assertTrue(result.getAccuracy().equals(geolocation.getAccuracy()));
        assertTrue(result.getInternetAddress().equals(geolocation.getInternetAddress()));
        assertTrue(result.getPhysicalAddress().equals(geolocation.getPhysicalAddress()));
        assertTrue(result.getFormattedAddress().equals(geolocation.getFormattedAddress()));
        assertTrue(result.getLocationTimestamp().equals(geolocation.getLocationTimestamp()));
        assertTrue(result.getEventGeofenceLatitude().equals(geolocation.getEventGeofenceLatitude()));
        assertTrue(result.getEventGeofenceLongitude().equals(geolocation.getEventGeofenceLongitude()));
        assertTrue(result.getRadius().equals(geolocation.getRadius()));
        assertTrue(result.getGeolocationPositioningType().equals(geolocation.getGeolocationPositioningType()));
        assertTrue(result.getLastGeolocationResponse().equals(geolocation.getLastGeolocationResponse()));
        assertTrue(result.getApiVersion().equals(geolocation.getApiVersion()));
        assertTrue(result.getUri().equals(geolocation.getUri()));

        // Update the Geolocation
        // The API is designed so that source, deviceIdentifier and geolocationType shall not possible to update once created
        URI url2 = URI.create("http://127.0.0.1:8080/restcomm/demos/geolocation-hello.xml");
        geolocation = geolocation.setDateUpdated(currentDateTime);
        geolocation = geolocation.setResponseStatus("failed");
        geolocation = geolocation.setCause("API not compliant");
        geolocation = geolocation.setCellId("00010");
        geolocation = geolocation.setLocationAreaCode("0A1");
        geolocation = geolocation.setMobileCountryCode(1);
        geolocation = geolocation.setMobileNetworkCode("33");
        geolocation = geolocation.setAgeOfLocationInfo(1);
        geolocation = geolocation.setDeviceLatitude("-1.638643");
        geolocation = geolocation.setDeviceLongitude("-172.4394389");
        geolocation = geolocation.setAccuracy((long) 9876352);
        geolocation = geolocation.setInternetAddress("200.0.91.253");
        geolocation = geolocation.setPhysicalAddress("A1-DD-0A-27-92-00");
        geolocation = geolocation.setFormattedAddress("27NW Street, 23456, Greenwich, Ireland");
        geolocation = geolocation.setLocationTimestamp(currentDateTime);
        geolocation = geolocation.setEventGeofenceLatitude("33.426280");
        geolocation = geolocation.setEventGeofenceLongitude("70.426280");
        geolocation = geolocation.setRadius((long) 99999);
        geolocation = geolocation.setGeolocationPositioningType("GPS");
        geolocation = geolocation.setLastGeolocationResponse("false");
        geolocation = geolocation.setUri(url2);

        // Update the Geolocation in the data store g
        geolocations.updateGeolocation(geolocation);

        // Read the updated Geolocation from the data store
        result = geolocations.getGeolocation(sid);

        // Validate the results
        assertTrue(result.getDateUpdated().equals(geolocation.getDateUpdated()));
        assertTrue(result.getAccountSid().equals(geolocation.getAccountSid()));
        assertTrue(result.getResponseStatus().equals(geolocation.getResponseStatus()));
        assertTrue(result.getCause() == geolocation.getCause());
        assertTrue(result.getCellId().equals(geolocation.getCellId()));
        assertTrue(result.getLocationAreaCode().equals(geolocation.getLocationAreaCode()));
        assertTrue(result.getMobileCountryCode().equals(geolocation.getMobileCountryCode()));
        assertTrue(result.getMobileNetworkCode().equals(geolocation.getMobileNetworkCode()));
        assertTrue(result.getNetworkEntityAddress().equals(geolocation.getNetworkEntityAddress()));
        assertTrue(result.getAgeOfLocationInfo().equals(geolocation.getAgeOfLocationInfo()));
        assertTrue(result.getDeviceLatitude().equals(geolocation.getDeviceLatitude()));
        assertTrue(result.getDeviceLongitude().equals(geolocation.getDeviceLongitude()));
        assertTrue(result.getAccuracy().equals(geolocation.getAccuracy()));
        assertTrue(result.getInternetAddress().equals(geolocation.getInternetAddress()));
        assertTrue(result.getPhysicalAddress().equals(geolocation.getPhysicalAddress()));
        assertTrue(result.getFormattedAddress().equals(geolocation.getFormattedAddress()));
        assertTrue(result.getLocationTimestamp().equals(geolocation.getLocationTimestamp()));
        assertTrue(result.getEventGeofenceLatitude().equals(geolocation.getEventGeofenceLatitude()));
        assertTrue(result.getEventGeofenceLongitude().equals(geolocation.getEventGeofenceLongitude()));
        assertTrue(result.getRadius().equals(geolocation.getRadius()));
        assertTrue(result.getGeolocationPositioningType().equals(geolocation.getGeolocationPositioningType()));
        assertTrue(result.getLastGeolocationResponse().equals(geolocation.getLastGeolocationResponse()));

        // Delete the Geolocation record
        geolocations.removeGeolocation(sid);

        // Validate the Geolocation record was removed.
        assertTrue(geolocations.getGeolocation(sid) == null);
    }

    @Test
    public void geolocationReadDeleteByAccountSid() {

        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        final Sid accountSid = Sid.generate(Sid.Type.ACCOUNT);
        URI url = URI.create("geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.now();
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(accountSid);
        builder.setSource("Source");
        builder.setDeviceIdentifier("DeviceIdentifier");
        builder.setGeolocationType(GeolocationType.Immediate);
        builder.setResponseStatus("successfull");
        builder.setCause("NA");
        builder.setCellId("12345");
        builder.setLocationAreaCode("978");
        builder.setMobileCountryCode(748);
        builder.setMobileNetworkCode("03");
        builder.setNetworkEntityAddress((long) 59848779);
        builder.setAgeOfLocationInfo(0);
        builder.setDeviceLatitude("105.638643");
        builder.setDeviceLongitude("172.4394389");
        builder.setAccuracy((long) 7845);
        builder.setInternetAddress("2001:0:9d38:6ab8:30a5:1c9d:58c6:5898");
        builder.setPhysicalAddress("D8-97-BA-19-02-D8");
        builder.setFormattedAddress("Avenida Brasil 2681, 11500, Montevideo, Uruguay");
        builder.setLocationTimestamp(currentDateTime);
        builder.setEventGeofenceLatitude("-33.426280");
        builder.setEventGeofenceLongitude("-70.566560");
        builder.setRadius((long) 1500);
        builder.setGeolocationPositioningType("Network");
        builder.setLastGeolocationResponse("true");
        builder.setApiVersion("2012-04-24");
        builder.setUri(url);
        final Geolocation geolocation = builder.build();
        final GeolocationDao geolocations = manager.getGeolocationDao();

        // Create a new Geolocation in the data store.
        geolocations.addGeolocation(geolocation);

        // Get all the Geolocations for a specific account.
        assertTrue(geolocations.getGeolocations(accountSid) != null);

        // Remove the Geolocations for a specific account.
        geolocations.removeGeolocation(accountSid);

        // Validate that the Geolocation were removed.
        assertTrue(geolocations.getGeolocation(accountSid) == null);
    }

}
