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
	
package org.mobicents.servlet.restcomm.provisioning.number.voxbone;

/**
 * @author jean.deruelle@telestax.com
 *
 */
public class VoxboneAvailablePhoneNumbersEndpointTestUtils {
    public static String jsonResponseES700 = 
            "{\"count\":1,\"numbers\":[{\"country\":\"ES\",\"msisdn\":\"34911067000\",\"type\":\"landline\",\"features\":[\"SMS\"],\"cost\":\"0.50\"}]}";
    public static String jsonResponseUSRange = 
            "{\"count\":177,\"numbers\":[{\"country\":\"US\",\"msisdn\":\"15102694548\",\"type\":\"mobile-lvn\",\"features\":[\"SMS\",\"VOICE\"],\"cost\":\"0.67\"},{\"country\":\"US\",\"msisdn\":\"17088568490\",\"type\":\"mobile-lvn\",\"features\":[\"SMS\",\"VOICE\"],\"cost\":\"0.67\"},{\"country\":\"US\",\"msisdn\":\"17088568491\",\"type\":\"mobile-lvn\",\"features\":[\"SMS\",\"VOICE\"],\"cost\":\"0.67\"},{\"country\":\"US\",\"msisdn\":\"17088568492\",\"type\":\"mobile-lvn\",\"features\":[\"SMS\",\"VOICE\"],\"cost\":\"0.67\"},{\"country\":\"US\",\"msisdn\":\"17088568973\",\"type\":\"mobile-lvn\",\"features\":[\"SMS\",\"VOICE\"],\"cost\":\"0.67\"}]}";
    public static String jsonResultES700 = "{\"friendlyName\":\"+34911067000\",\"phoneNumber\":\"34911067000\",\"isoCountry\":\"ES\",\"voiceCapable\":false,\"smsCapable\":true}";
    public static String jsonResultUSRange = "{\"friendlyName\":\"+15102694548\",\"phoneNumber\":\"15102694548\",\"isoCountry\":\"US\",\"voiceCapable\":true,\"smsCapable\":true}";
}
