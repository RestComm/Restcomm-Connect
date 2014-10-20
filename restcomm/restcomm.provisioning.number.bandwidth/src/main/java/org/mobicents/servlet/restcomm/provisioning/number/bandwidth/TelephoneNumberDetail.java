/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.provisioning.number.bandwidth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Describes a telephone number with details.
 */
@XmlRootElement(name = "TelephoneNumberDetail")
@XmlAccessorType(XmlAccessType.FIELD)
public class TelephoneNumberDetail {
    @XmlElement(name = "City")
    private String city;

    @XmlElement(name = "LATA")
    private String lata;

    @XmlElement(name = "RateCenter")
    private String rateCenter;

    @XmlElement(name = "State")
    private String state;

    @XmlElement(name = "FullNumber")
    private String fullNumber;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLATA() {
        return lata;
    }

    public void setLATA(String lata) {
        this.lata = lata;
    }

    public String getRateCenter() {
        return rateCenter;
    }

    public void setRateCenter(String rateCenter) {
        this.rateCenter = rateCenter;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getFullNumber() {
        return fullNumber;
    }

    public void setFullNumber(String fullNumber) {
        this.fullNumber = fullNumber;
    }
}
