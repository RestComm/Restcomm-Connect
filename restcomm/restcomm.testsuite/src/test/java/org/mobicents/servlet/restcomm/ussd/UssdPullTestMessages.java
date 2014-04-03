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
package org.mobicents.servlet.restcomm.ussd;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdPullTestMessages {

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
    
    static String ussdRestcommResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"The information you requested is 1234567890\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>processUnstructuredSSRequest_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";

    static String ussdRestcommResponseWithCollect = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ussd-data>\n"
            + "<language value=\"en\"></language>\n"
            + "<ussd-string value=\"Please press\n1 For option1\n2 For option2\"></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSRequest_Request</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>\n";
    
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
