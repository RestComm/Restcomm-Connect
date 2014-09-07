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
package org.mobicents.servlet.restcomm.http;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
public class UssdPushTestMessages {

    static String ussdPushNotifyOnlyMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            +"<ussd-string value=\"The information you requested is 1234567890\"></ussd-string>\n"
            +"<anyExt>\n"
            +"<message-type>unstructuredSSNotify_Request</message-type>\n"
            +"</anyExt>\n"
            +"</ussd-data>";

    static String ussdPushNotifyOnlyClientResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
            + "<ussd-data>\n"
            + "<language>en</language>\n"
            + "<ussd-string></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSNotify_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>";
    
    static String ussdPushNotifyOnlyFinalResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<error-code value=\"1\"></error-code>\n"
            + "</ussd-data>";

    static String ussdPushCollectMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"Please press\n1 For option1\n2 For option2\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSRequest_Request</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";
    
    static String ussdPushCollectClientResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"1\"/>\n"
            + "\t<anyExt>\n"
            + "\t\t<message-type>unstructuredSSRequest_Response</message-type>\n"
            + "\t</anyExt>\n"
            + "</ussd-data>";
    
//    <?xml version="1.0" encoding="UTF-8"?>
//    <ussd-data>
//    <language value="en"></language>
//    <ussd-string value="Please press
//    1 For option1
//    2 For option2"></ussd-string>
//    <anyExt>
//    <message-type>unstructuredSSRequest_Request</message-type>
//    </anyExt>
//    </ussd-data>
    
}
