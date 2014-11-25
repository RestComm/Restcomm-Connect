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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sbarstow on 10/17/14.
 */
@XmlRootElement(name = "DisconnectTelephoneNumberOrderType")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisconnectTelephoneNumberOrderType {
    @XmlElementWrapper(name = "TelephoneNumberList")
    @XmlElement(name = "TelephoneNumber")
    private List<String> telephoneNumberList = new ArrayList<String>();

    @XmlElement(name = "DisconnectMode")
    private String disconnectMode;

    public List<String> getTelephoneNumberList() {
        return telephoneNumberList;
    }

    public void setTelephoneNumberList(List<String> telephoneNumberList) {
        this.telephoneNumberList = telephoneNumberList;
    }

    public String getDisconnectMode() {
        return disconnectMode;
    }

    public void setDisconnectMode(String disconnectMode) {
        this.disconnectMode = disconnectMode;
    }
}