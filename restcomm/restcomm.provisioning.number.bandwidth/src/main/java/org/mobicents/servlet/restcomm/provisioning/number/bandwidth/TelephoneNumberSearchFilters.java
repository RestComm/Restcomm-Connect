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
/**
 * Created by sbarstow on 9/22/14.
 */
public class TelephoneNumberSearchFilters {
    private String inAreaCode;
    private String filterPattern;
    private String inState;
    private String inPostalCode;
    private boolean isTollFree;
    private int quantity = 5;
    private boolean returnTelephoneNumberDetails = true;
    private String inRateCenter;
    private String inLata;


    public boolean isReturnTelephoneNumberDetails() {
        return returnTelephoneNumberDetails;
    }

    public void setReturnTelephoneNumberDetails(boolean returnTelephoneNumberDetails) {
        this.returnTelephoneNumberDetails = returnTelephoneNumberDetails;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getInAreaCode() {
        return inAreaCode;
    }

    public void setInAreaCode(String inAreaCode) {
        this.inAreaCode = inAreaCode;
    }

    public String getFilterPattern() {
        return filterPattern;
    }

    public void setFilterPattern(String filterPattern) {
        this.filterPattern = filterPattern;
    }

    public String getInState() {
        return inState;
    }

    public void setInState(String inState) {
        this.inState = inState;
    }

    public String getInPostalCode() {
        return inPostalCode;
    }

    public void setInPostalCode(String inPostalCode) {
        this.inPostalCode = inPostalCode;
    }

    public boolean isTollFree() {
        return isTollFree;
    }

    public void setTollFree(boolean isTollFree) {
        this.isTollFree = isTollFree;
    }

    public String getInRateCenter() {
        return inRateCenter;
    }

    public void setInRateCenter(String inRateCenter) {
        this.inRateCenter = inRateCenter;
    }

    public String getInLata() {
        return inLata;
    }

    public void setInLata(String inLata) {
        this.inLata = inLata;
    }

}