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

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdRequest {

    private String message;
    private String language;
    private int messageLength;
    private UssdMessageType messageType;
    private String ussdRepresentation;
    private Boolean isFinalMessage = true;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }

    public UssdMessageType getUssdMessageType() {
        return messageType;
    }

    public void setMessageType(UssdMessageType messageType) {
        this.messageType = messageType;
    }

    public Boolean getIsFinalMessage() {
        return isFinalMessage;
    }

    public void setIsFinalMessage(Boolean isFinalMessage) {
        this.isFinalMessage = isFinalMessage;
    }

    public String createUssdPayload() throws XMLStreamException {
        // create an XMLOutputFactory
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);

        streamWriter.writeStartDocument("UTF-8", "1.0");
        streamWriter.writeStartElement("ussd-data");

        // Write Language element
        streamWriter.writeStartElement("language");
        streamWriter.writeAttribute("value", getLanguage());
        streamWriter.writeEndElement();

        // Write ussd-string
        streamWriter.writeStartElement("ussd-string");
        streamWriter.writeAttribute("value", getMessage());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement("anyExt");
        streamWriter.writeStartElement("message-type");
        streamWriter.writeCharacters(getUssdMessageType().name());
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();

        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();

        streamWriter.flush();
        streamWriter.close();

        return writer.toString();
    }
}