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
	
package org.mobicents.servlet.restcomm.provisioning.number.nexmo;


/**
 * @author jean.deruelle@telestax.com
 *
 */
public class NexmoIncomingPhoneNumbersEndpointTestUtils {
    public static String purchaseNumberSuccessResponse = "";
    public static String deleteNumberSuccessResponse = "";
    
    public static String jSonResultPurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+14156902867\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String jSonResultDeletePurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+34911067000\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String jSonResultUpdatePurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+33911067000\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String jSonResultUpdateSuccessPurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+33911067000\",\"voice_url\":\"http://demo.telestax.com/docs/voice2.xml\",\"voice_method\":\"POST\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":\"http://demo.telestax.com/docs/sms2.xml\",\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String jSonResultAccountAssociatedPurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+14216902867\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String jSonResultAccountAssociatedPurchaseNumberResult = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My*Company Line\",\"phone_number\":\"+1*6902867\",\"voice_url\":\"http://demo.telestax.com/docs/voice*.xml\",\"voice_method\":\"*\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":*,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";

    public static boolean match(String text, String pattern)
    {
        text = text.trim();
        pattern = pattern.trim();
        // Create the cards by splitting using a RegEx. If more speed 
        // is desired, a simpler character based splitting can be done.
        String [] cards = pattern.split("\\*");
 
        // Iterate over the cards.
        for (String card : cards)
        {
            int idx = text.indexOf(card);
 
            // Card not detected in the text.
            if(idx == -1)
            {
                return false;
            }
 
            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }
 
        return true;
    }
}
