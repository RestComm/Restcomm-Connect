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
    public void createReadUpdateDelete() {

        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        URI url = URI.create("http://127.0.0.1:8080/restcomm/demos/geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.parse("");
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(account);
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
        assertTrue(result.getCause().equals(geolocation.getCause()));
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
        url = URI.create("http://127.0.0.1:8080/restcomm/demos/geolocation-hello.xml");
        DateTime customDateTime1 = DateTime.parse("Tue, 22 Mar 2016 00:00:00 -0300");
        DateTime customDateTime2 = DateTime.parse("Tue, 22 Mar 2016 21:59:59 -0300");
        geolocation = geolocation.setDateUpdated(customDateTime1);
        geolocation = geolocation.setSource("099077937");
        geolocation = geolocation.setDeviceIdentifier("device:fernando'siPhone");
        geolocation = geolocation.setGeolocationType(GeolocationType.Notification);
        geolocation = geolocation.setResponseStatus("queued");
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
        geolocation = geolocation.setLocationTimestamp(customDateTime2);
        geolocation = geolocation.setEventGeofenceLatitude("N32 87 91.74");
        geolocation = geolocation.setEventGeofenceLongitude("E121 98 01.99");
        geolocation = geolocation.setRadius((long) 99999);
        geolocation = geolocation.setGeolocationPositioningType("GPS");
        geolocation = geolocation.setLastGeolocationResponse("false");

        // Read the updated Geolocation from the data store
        Geolocation updateResult = geolocations.getGeolocation(sid);

        // Validate the results
        assertTrue(updateResult.getDateUpdated().equals(geolocation.getDateUpdated()));
        assertTrue(updateResult.getSource().equals(geolocation.getSource()));
        assertTrue(updateResult.getDeviceIdentifier().equals(geolocation.getDeviceIdentifier()));
        assertTrue(updateResult.getGeolocationType().equals(geolocation.getGeolocationType()));
        assertTrue(updateResult.getResponseStatus().equals(geolocation.getResponseStatus()));
        assertTrue(updateResult.getCause().equals(geolocation.getCause()));
        assertTrue(updateResult.getCellId().equals(geolocation.getCellId()));
        assertTrue(updateResult.getLocationAreaCode().equals(geolocation.getLocationAreaCode()));
        assertTrue(updateResult.getMobileCountryCode().equals(geolocation.getMobileCountryCode()));
        assertTrue(updateResult.getMobileNetworkCode().equals(geolocation.getMobileNetworkCode()));
        assertTrue(updateResult.getNetworkEntityAddress().equals(geolocation.getNetworkEntityAddress()));
        assertTrue(updateResult.getAgeOfLocationInfo().equals(geolocation.getAgeOfLocationInfo()));
        assertTrue(updateResult.getDeviceLatitude().equals(geolocation.getDeviceLatitude()));
        assertTrue(updateResult.getDeviceLongitude().equals(geolocation.getDeviceLongitude()));
        assertTrue(updateResult.getAccuracy().equals(geolocation.getAccuracy()));
        assertTrue(updateResult.getInternetAddress().equals(geolocation.getInternetAddress()));
        assertTrue(updateResult.getPhysicalAddress().equals(geolocation.getPhysicalAddress()));
        assertTrue(updateResult.getFormattedAddress().equals(geolocation.getFormattedAddress()));
        assertTrue(updateResult.getLocationTimestamp().equals(geolocation.getLocationTimestamp()));
        assertTrue(updateResult.getEventGeofenceLatitude().equals(geolocation.getEventGeofenceLatitude()));
        assertTrue(updateResult.getEventGeofenceLongitude().equals(geolocation.getEventGeofenceLongitude()));
        assertTrue(updateResult.getRadius().equals(geolocation.getRadius()));
        assertTrue(updateResult.getGeolocationPositioningType().equals(geolocation.getGeolocationPositioningType()));
        assertTrue(updateResult.getLastGeolocationResponse().equals(geolocation.getLastGeolocationResponse()));

        // Delete the Geolocation record
        geolocations.removeGeolocation(sid);

        // Validate the Geolocation record was removed.
        assertTrue(geolocations.getGeolocation(sid) == null);
    }

    @Test
    public void readDeleteByAccountSid() {
        final Sid account = Sid.generate(Sid.Type.ACCOUNT);
        final Sid sid = Sid.generate(Sid.Type.GEOLOCATION);
        URI url = URI.create("geolocation-hello-world.xml");
        final Geolocation.Builder builder = Geolocation.builder();
        builder.setSid(sid);
        DateTime currentDateTime = DateTime.parse("");
        builder.setDateUpdated(currentDateTime);
        builder.setAccountSid(account);
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
        assertTrue(geolocations.getGeolocation(account) != null);

        // Remove all the Geolocations for a specific account.
        geolocations.removeGeolocation(account);

        // Validate that the Geolocations were removed.
        assertTrue(geolocations.getGeolocation(account) == null);
    }

}
