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

package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.Geolocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author fernando.mendioroz@telestax.com (Fernando Mendioroz)
 *
 */
public class GeolocationConverter extends AbstractConverter implements JsonSerializer<Geolocation> {

    public GeolocationConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Geolocation.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {

        final Geolocation geolocation = (Geolocation) object;
        writer.startNode("Geolocation");
        writeSid(geolocation.getSid(), writer);
        writeDateCreated(geolocation.getDateCreated(), writer);
        writeDateUpdated(geolocation.getDateUpdated(), writer);
        writeDateExecuted(geolocation.getDateExecuted(), writer);
        writeAccountSid(geolocation.getAccountSid(), writer);
        writeSource(geolocation.getSource(), writer);
        writeDeviceIdentifier(geolocation.getDeviceIdentifier(), writer);
        writeGeolocationType(geolocation.getGeolocationType(), writer);
        writeGeolocationData(geolocation, writer); /*** GeolocationData XML ***/
        writeGeolocationPositioningType(geolocation.getGeolocationPositioningType(), writer);
        writeLastGeolocationResponse(geolocation.getLastGeolocationResponse(), writer);
        writeApiVersion(geolocation.getApiVersion(), writer);
        writeUri(geolocation.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Geolocation geolocation, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(geolocation.getSid(), object);
        writeDateCreated(geolocation.getDateCreated(), object);
        writeDateUpdated(geolocation.getDateUpdated(), object);
        writeDateExecuted(geolocation.getDateExecuted(), object);
        writeAccountSid(geolocation.getAccountSid(), object);
        writeSource(geolocation.getSource(), object);
        writeDeviceIdentifier(geolocation.getDeviceIdentifier(), object);
        writeGeolocationType(geolocation.getGeolocationType(), object);
        writeGeolocationData(geolocation, object); /*** GeolocationData Json ***/
        writeGeolocationPositioningType(geolocation.getGeolocationPositioningType(), object);
        writeLastGeolocationResponse(geolocation.getLastGeolocationResponse(), object);
        writeApiVersion(geolocation.getApiVersion(), object);
        writeUri(geolocation.getUri(), object);
        return object;
    }

    protected void writeDateExecuted(final DateTime dateExecuted, final HierarchicalStreamWriter writer) {
        writer.startNode("DateExecuted");
        writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateExecuted.toDate()));
        writer.endNode();
    }

    protected void writeDateExecuted(final DateTime dateExecuted, final JsonObject object) {
        object.addProperty("date_executed",
                new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(dateExecuted.toDate()));
    }

    protected void writeSource(final String source, final HierarchicalStreamWriter writer) {
        if (source != null) {
            writer.startNode("Source");
            writer.setValue(source);
            writer.endNode();
        }
    }

    protected void writeSource(final String source, final JsonObject object) {
        if (source != null) {
            object.addProperty("source", source);
        } else {
            object.add("source", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceIdentifier(final String deviceIdentifier, final HierarchicalStreamWriter writer) {
        if (deviceIdentifier != null) {
            writer.startNode("DeviceIdentifier");
            writer.setValue(deviceIdentifier);
            writer.endNode();
        }
    }

    protected void writeDeviceIdentifier(final String deviceIdentifier, final JsonObject object) {
        if (deviceIdentifier != null) {
            object.addProperty("device_identifier", deviceIdentifier);
        } else {
            object.add("device_identifier", JsonNull.INSTANCE);
        }
    }

    protected void writeGeolocationType(final String geoLocationType, final HierarchicalStreamWriter writer) {
        if (geoLocationType != null) {
            writer.startNode("GeolocationType");
            writer.setValue(geoLocationType);
            writer.endNode();
        }
    }

    protected void writeGeolocationType(final String geoLocationType, final JsonObject object) {
        if (geoLocationType != null) {
            object.addProperty("geo_location_type", geoLocationType);
        } else {
            object.add("geo_location_type", JsonNull.INSTANCE);
        }
    }

    protected void writeGeolocationData(Geolocation geolocation, final HierarchicalStreamWriter writer) {
        writer.startNode("GeolocationData");
        if (geolocation != null) {
            writeCellId(geolocation.getCellId(), writer);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), writer);
            writeMobileCountryCode(geolocation.getMobileCountryCode(), writer);
            writeMobileNetworkCode(geolocation.getMobileNetworkCode(), writer);
            writeNetworkEntityAddress(geolocation.getNetworkEntityAddress(), writer);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), writer);
            writeDeviceLatitude(geolocation.getDeviceLatitude(), writer);
            writeDeviceLongitude(geolocation.getDeviceLongitude(), writer);
            writeNetworkEntityAddress(geolocation.getAccuracy(), writer);
            writeAccuracy(geolocation.getPhysicalAddress(), writer);
            writeInternetAddress(geolocation.getInternetAddress(), writer);
            writeInternetAddress(geolocation.getFormattedAddress(), writer);
            writeLocationTimestamp(geolocation.getLocationTimestamp(), writer);
            writeEventGeofenceLatitude(geolocation.getEventGeofenceLatitude(), writer);
            writeEventGeofenceLongitude(geolocation.getEventGeofenceLongitude(), writer);
            writeRadius(geolocation.getRadius(), writer);
        }
        writer.endNode();
    }

    protected void writeGeolocationData(Geolocation geolocation, final JsonObject object) {
        if (geolocation != null) {
            final JsonObject other = new JsonObject();
            writeCellId(geolocation.getCellId(), other);
            writeLocationAreaCode(geolocation.getLocationAreaCode(), other);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), other);
            writeMobileCountryCode(geolocation.getMobileCountryCode(), other);
            writeMobileNetworkCode(geolocation.getMobileNetworkCode(), other);
            writeNetworkEntityAddress(geolocation.getNetworkEntityAddress(), other);
            writeAgeOfLocationInfo(geolocation.getAgeOfLocationInfo(), other);
            writeDeviceLatitude(geolocation.getDeviceLatitude(), other);
            writeDeviceLongitude(geolocation.getDeviceLongitude(), other);
            writeNetworkEntityAddress(geolocation.getAccuracy(), other);
            writeAccuracy(geolocation.getPhysicalAddress(), other);
            writeInternetAddress(geolocation.getInternetAddress(), other);
            writeInternetAddress(geolocation.getFormattedAddress(), other);
            writeLocationTimestamp(geolocation.getLocationTimestamp(), other);
            writeEventGeofenceLatitude(geolocation.getEventGeofenceLatitude(), other);
            writeEventGeofenceLongitude(geolocation.getEventGeofenceLongitude(), other);
            writeRadius(geolocation.getRadius(), other);
            object.add("geolocation_data", other);
        } else {
            object.add("geolocation_data", JsonNull.INSTANCE);
        }
    }

    protected void writeCellId(final String cellId, final HierarchicalStreamWriter writer) {
        if (cellId != null) {
            writer.startNode("CellId");
            writer.setValue(cellId);
            writer.endNode();
        }
    }

    protected void writeCellId(final String cellId, final JsonObject object) {
        if (cellId != null) {
            object.addProperty("cell_id", cellId);
        } else {
            object.add("cell_id", JsonNull.INSTANCE);
        }
    }

    protected void writeLocationAreaCode(final String locationAreaCode, final HierarchicalStreamWriter writer) {
        if (locationAreaCode != null) {
            writer.startNode("LocationAreaCode");
            writer.setValue(locationAreaCode);
            writer.endNode();
        }
    }

    protected void writeLocationAreaCode(final String locationAreaCode, final JsonObject object) {
        if (locationAreaCode != null) {
            object.addProperty("location_area_code", locationAreaCode);
        } else {
            object.add("location_area_code", JsonNull.INSTANCE);
        }
    }

    protected void writeMobileCountryCode(final Integer mobileCountryCode, final HierarchicalStreamWriter writer) {
        if (mobileCountryCode != null) {
            writer.startNode("MobileCountryCode");
            writer.setValue(mobileCountryCode.toString());
            writer.endNode();
        }
    }

    protected void writeMobileCountryCode(final Integer mobileCountryCode, final JsonObject object) {
        if (mobileCountryCode != null) {
            object.addProperty("mobile_country_code", mobileCountryCode);
        } else {
            object.add("mobile_country_code", JsonNull.INSTANCE);
        }
    }

    protected void writeMobileNetworkCode(final Integer mobileNetworkCode, final HierarchicalStreamWriter writer) {
        if (mobileNetworkCode != null) {
            writer.startNode("MobileNetworkCode");
            writer.setValue(mobileNetworkCode.toString());
            writer.endNode();
        }
    }

    protected void writeMobileNetworkCode(final Integer mobileNetworkCode, final JsonObject object) {
        if (mobileNetworkCode != null) {
            object.addProperty("mobile_network_code", mobileNetworkCode);
        } else {
            object.add("mobile_network_code", JsonNull.INSTANCE);
        }
    }

    protected void writeNetworkEntityAddress(final BigInteger networkEntityAddress, final HierarchicalStreamWriter writer) {
        if (networkEntityAddress != null) {
            writer.startNode("NetworkEntityAddress");
            writer.setValue(networkEntityAddress.toString());
            writer.endNode();
        }
    }

    protected void writeNetworkEntityAddress(final BigInteger networkEntityAddress, final JsonObject object) {
        if (networkEntityAddress != null) {
            object.addProperty("network_entity_address", networkEntityAddress);
        } else {
            object.add("network_entity_address", JsonNull.INSTANCE);
        }
    }

    protected void writeAgeOfLocationInfo(final Integer ageOfLocationInfo, final HierarchicalStreamWriter writer) {
        if (ageOfLocationInfo != null) {
            writer.startNode("LocationAge");
            writer.setValue(ageOfLocationInfo.toString());
            writer.endNode();
        }
    }

    protected void writeAgeOfLocationInfo(final Integer ageOfLocationInfo, final JsonObject object) {
        if (ageOfLocationInfo != null) {
            object.addProperty("location_age", ageOfLocationInfo);
        } else {
            object.add("location_age", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceLatitude(final String deviceLatitude, final HierarchicalStreamWriter writer) {
        if (deviceLatitude != null) {
            writer.startNode("DeviceLatitude");
            writer.setValue(deviceLatitude);
            writer.endNode();
        }
    }

    protected void writeDeviceLatitude(final String deviceLatitude, final JsonObject object) {
        if (deviceLatitude != null) {
            object.addProperty("device_latitude", deviceLatitude);
        } else {
            object.add("device_latitude", JsonNull.INSTANCE);
        }
    }

    protected void writeDeviceLongitude(final String deviceLongitude, final HierarchicalStreamWriter writer) {
        if (deviceLongitude != null) {
            writer.startNode("DeviceLongitude");
            writer.setValue(deviceLongitude);
            writer.endNode();
        }
    }

    protected void writeDeviceLongitude(final String deviceLongitude, final JsonObject object) {
        if (deviceLongitude != null) {
            object.addProperty("device_longitude", deviceLongitude);
        } else {
            object.add("device_longitude", JsonNull.INSTANCE);
        }
    }

    protected void writeAccuracy(final String accuracy, final HierarchicalStreamWriter writer) {
        if (accuracy != null) {
            writer.startNode("Accuracy");
            writer.setValue(accuracy.toString());
            writer.endNode();
        }
    }

    protected void writeAccuracy(final String accuracy, final JsonObject object) {
        if (accuracy != null) {
            object.addProperty("accuracy", accuracy);
        } else {
            object.add("accuracy", JsonNull.INSTANCE);
        }
    }

    protected void writePhysicalAddress(final String physicalAddress, final HierarchicalStreamWriter writer) {
        if (physicalAddress != null) {
            writer.startNode("PhysicalAddress");
            writer.setValue(physicalAddress);
            writer.endNode();
        }
    }

    protected void writePhysicalAddress(final String physicalAddress, final JsonObject object) {
        if (physicalAddress != null) {
            object.addProperty("physical_address", physicalAddress);
        } else {
            object.add("physical_address", JsonNull.INSTANCE);
        }
    }

    protected void writeInternetAddress(final String internetAddress, final HierarchicalStreamWriter writer) {
        if (internetAddress != null) {
            writer.startNode("InternetAddress");
            writer.setValue(internetAddress);
            writer.endNode();
        }
    }

    protected void writeInternetAddress(final String internetAddress, final JsonObject object) {
        if (internetAddress != null) {
            object.addProperty("internet_address", internetAddress);
        } else {
            object.add("internet_address", JsonNull.INSTANCE);
        }
    }

    protected void writeLocationTimestamp(final DateTime locationTimestamp, final HierarchicalStreamWriter writer) {
        if (locationTimestamp != null) {
            writer.startNode("LocationTimestamp");
            writer.setValue(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(locationTimestamp.toDate()));
            writer.endNode();
        }
    }

    protected void writeLocationTimestamp(final DateTime locationTimestamp, final JsonObject object) {
        if (locationTimestamp != null) {
            object.addProperty("location_timestamp",
                    new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(locationTimestamp.toDate()));
        } else {
            object.add("location_timestamp", JsonNull.INSTANCE);
        }
    }

    protected void writeEventGeofenceLatitude(final String eventGeofenceLatitude, final HierarchicalStreamWriter writer) {
        if (eventGeofenceLatitude != null) {
            writer.startNode("EventGeofenceLatitude");
            writer.setValue(eventGeofenceLatitude);
            writer.endNode();
        }
    }

    protected void writeEventGeofenceLatitude(final String eventGeofenceLatitude, final JsonObject object) {
        if (eventGeofenceLatitude != null) {
            object.addProperty("event_geofence_latitude", eventGeofenceLatitude);
        } else {
            object.add("event_geofence_latitude", JsonNull.INSTANCE);
        }
    }

    protected void writeEventGeofenceLongitude(final String eventGeofenceLongitude, final HierarchicalStreamWriter writer) {
        if (eventGeofenceLongitude != null) {
            writer.startNode("EventGeofenceLongitude");
            writer.setValue(eventGeofenceLongitude);
            writer.endNode();
        }
    }

    protected void writeEventGeofenceLongitude(final String eventGeofenceLongitude, final JsonObject object) {
        if (eventGeofenceLongitude != null) {
            object.addProperty("event_geofence_longitude", eventGeofenceLongitude);
        } else {
            object.add("event_geofence_longitude", JsonNull.INSTANCE);
        }
    }

    protected void writeRadius(final BigInteger radius, final HierarchicalStreamWriter writer) {
        if (radius != null) {
            writer.startNode("Radius");
            writer.setValue(radius.toString());
            writer.endNode();
        }
    }

    protected void writeRadius(final BigInteger radius, final JsonObject object) {
        if (radius != null) {
            object.addProperty("radius", radius);
        } else {
            object.add("radius", JsonNull.INSTANCE);
        }
    }

    protected void writeGeolocationPositioningType(final String geolocationPositioningType,
            final HierarchicalStreamWriter writer) {
        if (geolocationPositioningType != null) {
            writer.startNode("GeolocationPositioningType");
            writer.setValue(geolocationPositioningType.toString());
            writer.endNode();
        }
    }

    protected void writeGeolocationPositioningType(final String geolocationPositioningType, final JsonObject object) {
        if (geolocationPositioningType != null) {
            object.addProperty("geolocation_positioning_type", geolocationPositioningType);
        } else {
            object.add("geolocation_positioning_type", JsonNull.INSTANCE);
        }
    }

    protected void writeLastGeolocationResponse(final Boolean lastGeolocationResponse,
            final HierarchicalStreamWriter writer) {
        if (lastGeolocationResponse != null) {
            writer.startNode("LastGeolocationResponse");
            writer.setValue(lastGeolocationResponse.toString());
            writer.endNode();
        }
    }

    protected void writeLastGeolocationResponse(final Boolean lastGeolocationResponse, final JsonObject object) {
        if (lastGeolocationResponse != null) {
            object.addProperty("last_geolocation_response", lastGeolocationResponse);
        } else {
            object.add("last_geolocation_response", JsonNull.INSTANCE);
        }
    }


}
