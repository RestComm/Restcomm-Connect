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
package org.mobicents.servlet.restcomm.ussd.commons;

import static javax.xml.stream.XMLStreamConstants.*;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.sip.SipServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

/**
 * This class represents the INFO request received by Restcomm from Client.
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdInfoRequest {

    private final String ussdPayload;
    private String message;
    private String language;
    private UssdMessageType ussdMessageType;

    public UssdInfoRequest(SipServletRequest request) throws IOException{
        this.ussdPayload = new String(request.getRawContent());
    }

    public UssdInfoRequest(String payload) {
        this.ussdPayload = payload;
    }

    public void readUssdPayload() throws Exception{

        StringReader reader = new StringReader(ussdPayload.trim().replaceAll("&([^;]+(?!(?:\\w|;)))", "&amp;$1").replaceAll("\\n", ""));

        final XMLInputFactory inputs = XMLInputFactory.newInstance();
        inputs.setProperty("javax.xml.stream.isCoalescing", true);
        XMLStreamReader stream = null;
        try {
            stream = inputs.createXMLStreamReader(reader);
            while (stream.hasNext()){
                stream.next();
                int streamEvent = stream.getEventType();
                if( streamEvent != END_DOCUMENT && streamEvent == START_ELEMENT) {
                    String name = stream.getLocalName();
                    if(name.equalsIgnoreCase("language") && stream.isStartElement()) {
                        this.language = stream.getAttributeValue("", "value");
                    } else if (name.equalsIgnoreCase("ussd-string") && stream.isStartElement()) {
                        this.message = stream.getAttributeValue("", "value");
                    } else if (name.equalsIgnoreCase("anyExt") && stream.isStartElement()) {
                        stream.next();
                        name = stream.getLocalName();
                        if (name.equalsIgnoreCase("message-type") && stream.isStartElement()){
                            stream.next();
                            this.ussdMessageType = UssdMessageType.valueOf(stream.getText().trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.message = e.getMessage();
            throw e;
        }
    }

    public String getMessage() throws Exception {
        if (message == null)
            readUssdPayload();
        return message;
    }

    public String getLanguage() throws Exception {
        if (language == null)
            readUssdPayload();
        return (language == null) ? "en" : language;
    }

    public UssdMessageType getUssdMessageType() throws Exception {
        if(ussdMessageType == null)
            readUssdPayload();
        return (ussdMessageType == null) ? UssdMessageType.unstructuredSSRequest_Response : ussdMessageType;
    }

    public int getMessageLength() throws Exception {
        if(message == null)
            readUssdPayload();
        return message.length();
    }

    public Boolean getIsFinalMessage() {
        // TODO Auto-generated method stub
        return null;
    }
}
