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
import java.util.List;

/**
 * Created by sbarstow on 10/17/14.
 */
@XmlRootElement(name="DisconnectTelephoneNumberOrderResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisconnectTelephoneNumberOrderResponse {
    @XmlElement(name="orderRequest")
    private OrderRequest orderRequest;

    @XmlElementWrapper(name="ErrorList")
    @XmlElement(name="Error")
    private List<Error> errorList = new ArrayList<Error>();

    @XmlElement(name="OrderStatus")
    private String orderStatus;

    public OrderRequest getorderRequest() {
        return orderRequest;
    }

    public void setorderRequest(OrderRequest orderRequest) {
        this.orderRequest = orderRequest;
    }

    public List<Error> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<Error> errorList) {
        this.errorList = errorList;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}