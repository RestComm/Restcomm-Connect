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
import java.util.Date;

/**
 * Created by sbarstow on 10/17/14.
 */
@XmlRootElement(name="orderRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class OrderRequest {
    @XmlElement(name="Name")
    private String name;

    @XmlElement(name="OrderCreateDate")
    private Date orderCreateDate;

    @XmlElement(name="id")
    private String id;

    @XmlElement(name="DisconnectTelephoneNumberOrderType")
    private DisconnectTelephoneNumberOrderType disconnectTelephoneNumberOrderType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getOrderCreateDate() {
        return orderCreateDate;
    }

    public void setOrderCreateDate(Date orderCreateDate) {
        this.orderCreateDate = orderCreateDate;
    }

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public DisconnectTelephoneNumberOrderType getDisconnectTelephoneNumberOrderType() {
        return disconnectTelephoneNumberOrderType;
    }

    public void setDisconnectTelephoneNumberOrderType(DisconnectTelephoneNumberOrderType disconnectTelephoneNumberOrderType) {
        this.disconnectTelephoneNumberOrderType = disconnectTelephoneNumberOrderType;
    }
}
