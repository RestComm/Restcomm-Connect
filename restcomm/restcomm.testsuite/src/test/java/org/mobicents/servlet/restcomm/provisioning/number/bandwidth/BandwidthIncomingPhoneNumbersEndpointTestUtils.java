package org.mobicents.servlet.restcomm.provisioning.number.bandwidth;

/**
 * Created by sbarstow on 10/14/14.
 */
public class BandwidthIncomingPhoneNumbersEndpointTestUtils {
    public static String validOrderResponseXml="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><OrderResponse><Order><Name>A New Order</Name><OrderCreateDate>2014-10-14T17:58:15.299Z</OrderCreateDate><BackOrderRequested>false</BackOrderRequested><id>someid</id><ExistingTelephoneNumberOrderType><TelephoneNumberList><TelephoneNumber>4156902867</TelephoneNumber></TelephoneNumberList></ExistingTelephoneNumberOrderType><PartialAllowed>false</PartialAllowed><SiteId>2858</SiteId></Order></OrderResponse>";
    public static String jSonResultPurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+14156902867\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";
    public static String validDisconnectOrderResponseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><DisconnectTelephoneNumberOrderResponse><orderRequest><Name>Disconnect</Name><OrderCreateDate>2014-10-17T15:02:46.077Z</OrderCreateDate><id>disconnectId</id><DisconnectTelephoneNumberOrderType><TelephoneNumberList><TelephoneNumber>4156902867</TelephoneNumber></TelephoneNumberList><DisconnectMode>normal</DisconnectMode></DisconnectTelephoneNumberOrderType></orderRequest></DisconnectTelephoneNumberOrderResponse>";
    public static String jSonResultDeletePurchaseNumber = "{\"sid\":\"PN*\",\"account_sid\":\"ACae6e420f425248d6a26948c17a9e2acf\",\"friendly_name\":\"My Company Line\",\"phone_number\":\"+14156902867\",\"voice_url\":\"http://demo.telestax.com/docs/voice.xml\",\"voice_method\":\"GET\",\"voice_fallback_url\":null,\"voice_fallback_method\":\"POST\",\"status_callback\":null,\"status_callback_method\":\"POST\",\"voice_caller_id_lookup\":false,\"voice_application_sid\":null,\"date_created\":\"*\",\"date_updated\":\"*\",\"sms_url\":null,\"sms_method\":\"POST\",\"sms_fallback_url\":null,\"sms_fallback_method\":\"POST\",\"sms_application_sid\":null,\"capabilities\":{\"voice_capable\":false,\"sms_capable\":false,\"mms_capable\":false,\"fax_capable\":false},\"api_version\":\"2012-04-24\",\"uri\":\"/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PN*.json\"}";

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
