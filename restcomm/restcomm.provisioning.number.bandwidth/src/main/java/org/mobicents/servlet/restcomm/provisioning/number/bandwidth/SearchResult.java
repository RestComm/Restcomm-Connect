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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The root entity returned by IRIS for a local or toll-free number search.
 * <p/>
 * If number details were desired (only works for local queries),
 * {@link #telephoneNumberDetailList} will be populated and
 * {@link #telephoneNumberList} will be empty.
 * <p/>
 * If details were not desired, the plain list of numbers can be found in
 * {@link #telephoneNumberList} and {@link #telephoneNumberDetailList} will be
 * empty.
 */
@XmlRootElement(name = "SearchResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResult {

    @XmlElement(name = "Error")
    private Error error;

    @XmlElement(name = "ResultCount")
    private Integer resultCount;

    @XmlElementWrapper(name = "TelephoneNumberList")
    @XmlElement(name = "TelephoneNumber")
    private List<String> telephoneNumberList = new ArrayList<String>();

    @XmlElementWrapper(name = "TelephoneNumberDetailList")
    @XmlElement(name = "TelephoneNumberDetail")
    private List<TelephoneNumberDetail> telephoneNumberDetailList = new ArrayList<TelephoneNumberDetail>();

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public List<String> getTelephoneNumberList() {
        return telephoneNumberList;
    }

    public void setTelephoneNumberList(List<String> telephoneNumberList) {
        this.telephoneNumberList = telephoneNumberList;
    }

    public List<TelephoneNumberDetail> getTelephoneNumberDetailList() {
        return telephoneNumberDetailList;
    }

    public void setTelephoneNumberDetailList(List<TelephoneNumberDetail> telephoneNumberDetailList) {
        this.telephoneNumberDetailList = telephoneNumberDetailList;
    }
}
