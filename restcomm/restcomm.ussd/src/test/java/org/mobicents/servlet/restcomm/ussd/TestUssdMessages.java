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

import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.mobicents.servlet.restcomm.ussd.commons.UssdMessageType;
import org.mobicents.servlet.restcomm.ussd.commons.UssdRequest;
import org.mobicents.servlet.restcomm.ussd.commons.UssdResponse;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class TestUssdMessages {

    private String requestLanguage = "en";
    private String requestMessage = "Press 1 to know your airtime and 2 to know your SMS balance";
    private UssdMessageType requestMessageType = UssdMessageType.unstructuredSSRequest_Request;
    UssdRequest ussdRequest;

    String ussdOriginalRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ussd-data><language value=\"en\"></language>"
            + "<ussd-string value=\"Press 1 to know your airtime and 2 to know your SMS balance\"></ussd-string>"
            + "<anyExt><message-type>unstructuredSSRequest_Request</message-type></anyExt></ussd-data>";
    
    String ussdOriginalResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ussd-data><language value=\"en\"/>"
            + "<ussd-string value=\"1\"/><anyExt><message-type>unstructuredSSRequest_Response</message-type></anyExt></ussd-data>";
    
    String ussdOriginalResponse2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ussd-data>"
            + "<ussd-string value=\"2\"/></ussd-data>";
    
    private void createUssdMessage() {
        ussdRequest = new UssdRequest();
        ussdRequest.setLanguage("en");
        ussdRequest.setMessage(requestMessage);
        ussdRequest.setMessageType(requestMessageType);
    }
    
    @Test
    public void testUssdRequest() throws XMLStreamException{
        createUssdMessage();
        String ussdPayload = ussdRequest.createUssdPayload();
        assertTrue(ussdOriginalRequest.equals(ussdPayload));
    }
    
    @Test
    public void testUssdResponse() {
        UssdResponse ussdResponse = new UssdResponse(ussdOriginalResponse);
        assertTrue("1".equals(ussdResponse.getMessage()));
        assertTrue("en".equals(ussdResponse.getLanguage()));
        assertTrue(UssdMessageType.unstructuredSSRequest_Response.equals(ussdResponse.getUssdMessageType()));
        
        ussdResponse = new UssdResponse(ussdOriginalResponse2);
        assertTrue("2".equals(ussdResponse.getMessage()));
        assertTrue("en".equals(ussdResponse.getLanguage()));
        assertTrue(UssdMessageType.unstructuredSSRequest_Response.equals(ussdResponse.getUssdMessageType()));
    }

}
