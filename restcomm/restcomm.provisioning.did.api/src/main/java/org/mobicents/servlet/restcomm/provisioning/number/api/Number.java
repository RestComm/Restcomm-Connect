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
package org.mobicents.servlet.restcomm.provisioning.number.api;

import java.util.List;

/**
 * A POJO Representation of a Number. It contains the following properties :
 * <ul>
 * 	<li>country code - 2 letters Country Code as defined per http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2</li>
 *  <li>MSISDN - Number in MSISDN representation ie 34911067000</li>
 *  <li>Type - Mobile number or Fixed Line</li>
 *  <li>Features- Whether the number provides Voice or/and SMS capabilities</li>
 *  <li>Cost - cost of the number per minute</li>
 * <ul>
 * 
 * @author jean.deruelle@telestax.com
 *
 */
public interface Number {
	String getCountryCode();
	void setCountryCode(String countryCode);
	
	String getMsisdn();
	void setMsisdn(String msisdn);
	
	String getType();
	void setType(String type);
	
	List<String> getFeatures();
	void setFeatures(List<String> features);
	
	String getCost();
	void setCost(String cost);
}
