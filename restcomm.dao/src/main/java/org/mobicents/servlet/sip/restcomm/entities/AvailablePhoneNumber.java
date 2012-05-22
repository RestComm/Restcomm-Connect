/*
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
package org.mobicents.servlet.sip.restcomm;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class AvailablePhoneNumber {
  private final String friendlyName;
  private final String phoneNumber;
  private final Integer lata;
  private final String rateCenter;
  private final Double latitude;
  private final Double longitude;
  private final String region;
  private final Integer postalCode;
  private final String isoCountry;
  
  public AvailablePhoneNumber(final String friendlyName, final String phoneNumber, final Integer lata,
      final String rateCenter, final Double latitude, final Double longitude, final String region,
      final Integer postalCode, final String isoCountry) {
    super();
    this.friendlyName = friendlyName;
    this.phoneNumber = phoneNumber;
    this.lata = lata;
    this.rateCenter = rateCenter;
    this.latitude = latitude;
    this.longitude = longitude;
    this.region = region;
    this.postalCode = postalCode;
    this.isoCountry = isoCountry;
  }
  
  public String getFriendlyName() {
    return friendlyName;
  }
  
  public String getPhoneNumber() {
    return phoneNumber;
  }
  
  public Integer getLata() {
    return lata;
  }
  
  public String getRateCenter() {
    return rateCenter;
  }
  
  public Double getLatitude() {
	return latitude;  
  }
  
  public Double getLongitude() {
    return longitude;
  }
  
  public String getRegion() {
    return region;
  }
  
  public Integer getPostalCode() {
    return postalCode;
  }
  
  public String getIsoCountry() {
    return isoCountry;
  }
  
  public AvailablePhoneNumber setFriendlyName(final String friendlyName) {
    return new AvailablePhoneNumber(friendlyName, phoneNumber, lata, rateCenter, latitude, longitude,
        region, postalCode, isoCountry);
  }
}
