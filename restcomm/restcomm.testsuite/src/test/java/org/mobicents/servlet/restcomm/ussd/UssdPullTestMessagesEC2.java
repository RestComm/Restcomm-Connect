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
package org.mobicents.servlet.restcomm.ussd;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdPullTestMessagesEC2 {

    static String ussdClientRequestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5544\"/>\n"
            + "</ussd-data>";

    static String ussdClientRequestBodyForCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5555\"/>\n"
            + "</ussd-data>";
    
    static String ussdRestcommResponse = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n"
            +"<ussd-data>\n"
            +"<language value=\"en\"/>\n"
            +"<ussd-string value=\"You pressed 1 for option1, so here it is OPTION1\"/>\n"
            +"<anyExt>\n"
            +"<message-type>processUnstructuredSSRequest_Response</message-type>\n"
            +"</anyExt>\n"
            +"</ussd-data>";

    static String ussdRestcommResponseWithCollect = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"/>\n"
            + "<ussd-string value=\"Please press 1 for option1 or 2 for option 2\"/>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSRequest_Request</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>";
    
    static String ussdClientResponseBodyToCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"1\"/>\n"
            + "\t<anyExt>\n"
            + "\t\t<message-type>unstructuredSSRequest_Response</message-type>\n"
            + "\t</anyExt>\n"
            + "</ussd-data>";
    
    static String ussdClientRequestBodyForMessageLenghtExceeds = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
            + "<ussd-data>\n"
            + "\t<language value=\"en\"/>\n"
            + "\t<ussd-string value=\"5566\"/>\n"
            + "</ussd-data>";
    
    static String ussdRestcommResponseForMessageLengthExceeds = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"Error while preparing the response.\nMessage length exceeds the maximum.\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>processUnstructuredSSRequest_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";
    
}
