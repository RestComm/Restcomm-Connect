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

    static String ussdPushNotifyOnlyResponse = "<ussd-data>\n"
            + "<language>en</language>\n"
            + "<ussd-string></ussd-string>\n"
            + "<anyExt>\n"
            + "<message-type>unstructuredSSNotify_Response</message-type>\n"
            + "</anyExt>\n"
            + "</ussd-data>";

}
