package org.mobicents.servlet.restcomm.provisioning.number.bandwidth;

/**
 * Created by sbarstow on 10/7/14.
 */
public class BandwidthAvailablePhoneNumbersEndpointTestUtils {
    public static String areaCode201SearchResult = "<SearchResult><ResultCount>1</ResultCount><TelephoneNumberDetailList><TelephoneNumberDetail><City>JERSEY CITY</City><LATA>224</LATA><RateCenter>JERSEYCITY</RateCenter><State>NJ</State><FullNumber>2012001555</FullNumber></TelephoneNumberDetail></TelephoneNumberDetailList></SearchResult>";
    public static String firstJSonResult201AreaCode = "{\"friendlyName\":\"+12012001555\",\"phoneNumber\":\"+12012001555\",\"LATA\":224,\"rateCenter\":\"JERSEYCITY\",\"region\":\"NJ\",\"isoCountry\":\"US\",\"voiceCapable\":true,\"smsCapable\":true,\"mmsCapable\":false,\"faxCapable\":false,\"ussdCapable\":false}";
    public static String emptySearchResult = "<SearchResult><ResultCount>0</ResultCount></SearchResult>";
    public static String malformedSearchResult = "<SearchResult><SomeElement>Test</SomeElement";
    public static String zipCode27601SearchResult = "<SearchResult><ResultCount>1</ResultCount><TelephoneNumberDetailList><TelephoneNumberDetail><City>RALEIGH</City><LATA>123</LATA><RateCenter>RALEIGH</RateCenter><State>NC</State><FullNumber>19195551212</FullNumber></TelephoneNumberDetail></TelephoneNumberDetailList></SearchResult>";
    public static String firstJSONResult27601ZipCode = "{\"friendlyName\":\"+19195551212\",\"phoneNumber\":\"+19195551212\",\"lata\":123,\"rateCenter\":\"RALEIGH\",\"region\":\"NC\",\"isoCountry\":\"US\",\"voiceCapable\":true,\"smsCapable\":true,\"mmsCapable\":false,\"faxCapable\":false,\"ussdCapable\":false}";

    public static String areaCode205SearchResult = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><SearchResult><ResultCount>5</ResultCount><TelephoneNumberDetailList><TelephoneNumberDetail><City>CALERA</City><LATA>476</LATA><RateCenter>CALERA    </RateCenter><State>AL</State><FullNumber>2053194418</FullNumber><Tier>0</Tier><VendorId>49</VendorId><VendorName>Bandwidth CLEC</VendorName></TelephoneNumberDetail><TelephoneNumberDetail><City>CALERA</City><LATA>476</LATA><RateCenter>CALERA    </RateCenter><State>AL</State><FullNumber>2053194421</FullNumber><Tier>0</Tier><VendorId>49</VendorId><VendorName>Bandwidth CLEC</VendorName></TelephoneNumberDetail><TelephoneNumberDetail><City>CALERA</City><LATA>476</LATA><RateCenter>CALERA    </RateCenter><State>AL</State><FullNumber>2053194437</FullNumber><Tier>0</Tier><VendorId>49</VendorId><VendorName>Bandwidth CLEC</VendorName></TelephoneNumberDetail><TelephoneNumberDetail><City>CALERA</City><LATA>476</LATA><RateCenter>CALERA    </RateCenter><State>AL</State><FullNumber>2053194457</FullNumber><Tier>0</Tier><VendorId>49</VendorId><VendorName>Bandwidth CLEC</VendorName></TelephoneNumberDetail><TelephoneNumberDetail><City>CALERA</City><LATA>476</LATA><RateCenter>CALERA    </RateCenter><State>AL</State><FullNumber>2053194459</FullNumber><Tier>0</Tier><VendorId>49</VendorId><VendorName>Bandwidth CLEC</VendorName></TelephoneNumberDetail></TelephoneNumberDetailList></SearchResult>";



}
