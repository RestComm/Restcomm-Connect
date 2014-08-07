/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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
	
package org.mobicents.servlet.restcomm.provisioning.number.vi;

/**
 * @author jean.deruelle@telestax.com
 *
 */
public class AvailablePhoneNumbersEndpointTestUtils {
    public static String body501AreaCode = 
            "<response id=\"7bf8a6c56f594126a6cdecadb430b672\"><header><sessionid>d46146b4132d611304269388cffabb17</sessionid></header><body><search><name>npa = '510' </name>"
            + "<status>Results Found</status><statuscode>100</statuscode>"
            + "<state><name>CA</name><lata>"
            + "<name>722</name><rate_center><name>BELVEDERE</name><npa><name>415</name><nxx><name>690</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156902867</tn></nxx><nxx><name>691</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156914883</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156914885</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156914887</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156914995</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4156914996</tn></nxx><nxx><name>797</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4157977554</tn></nxx></npa></rate_center><rate_center><name>CORTEMADRA</name><npa><name>415</name><nxx><name>329</name><tn tier=\"777\" t38=\"0\" cnamStorage=\"0\">4153290373</tn><tn tier=\"777\" t38=\"0\" cnamStorage=\"0\">4153290374</tn></nxx><nxx><name>413</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154132282</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154132350</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154132353</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154132354</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154132355</tn></nxx><nxx><name>496</name><tn tier=\"0\" t38=\"0\" cnamStorage=\"0\">4154964606</tn><tn tier=\"0\" t38=\"0\" cnamStorage=\"0\">4154964607</tn></nxx><nxx><name>758</name><tn tier=\"3\" t38=\"0\" cnamStorage=\"0\">4157583846</tn></nxx></npa></rate_center><rate_center><name>IGNACIO</name><npa><name>415</name><nxx><name>234</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341046</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341054</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341058</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341068</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">5105551214</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341208</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152341209</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152344296</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4152344541</tn></nxx><nxx><name>475</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154750513</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154750519</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154751618</tn></nxx><nxx><name>483</name><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154831578</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154831604</tn><tn tier=\"0\" t38=\"1\" cnamStorage=\"1\">4154834163</tn></nxx></npa></rate_center></lata></state></search></body></response>";
    public static String firstJSonResult501AreaCode = "{\"friendlyName\":\"+14156902867\",\"phoneNumber\":\"+14156902867\",\"lata\":722,\"rateCenter\":\"BELVEDERE\",\"region\":\"CA\",\"isoCountry\":\"US\",\"voiceCapable\":true,\"faxCapable\":true}";
}
