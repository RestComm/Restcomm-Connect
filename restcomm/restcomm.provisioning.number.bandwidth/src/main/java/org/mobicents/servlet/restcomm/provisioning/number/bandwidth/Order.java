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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "Order")
@XmlAccessorType(XmlAccessType.FIELD)
public class Order {
    @XmlElement(name="id")
    private String id;

    @XmlElement(name="BackOrderRequested")
    private boolean backOrderRequested;

    @XmlElement(name="OrderCreateDate")
    private Date orderCreateDate;

    @XmlElement(name="Name")
    private String name;

    @XmlElement(name="SiteId")
    private String siteId;

    @XmlElement(name="CustomerOrderId")
    private String customerOrderId;

    @XmlElement(name="PartialAllowed")
    private boolean partialAllowed = false;

    @XmlElement(name="ExistingTelephoneNumberOrderType")
    private ExistingTelephoneNumberOrderType existingTelephoneNumberOrderType;

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public boolean isBackOrderRequested() {
        return backOrderRequested;
    }

    public void setBackOrderRequested(boolean backOrderRequested) {
        this.backOrderRequested = backOrderRequested;
    }

    public Date getOrderCreateDate() {
        return orderCreateDate;
    }

    public void setOrderCreateDate(Date orderCreateDate) {
        this.orderCreateDate = orderCreateDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(String customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public boolean isPartialAllowed() {
        return partialAllowed;
    }

    public void setPartialAllowed(boolean partialAllowed) {
        this.partialAllowed = partialAllowed;
    }

    public ExistingTelephoneNumberOrderType getExistingTelephoneNumberOrderType() {
        return existingTelephoneNumberOrderType;
    }

    public void setExistingTelephoneNumberOrderType(ExistingTelephoneNumberOrderType existingTelephoneNumberOrderType) {
        this.existingTelephoneNumberOrderType = existingTelephoneNumberOrderType;
    }
}