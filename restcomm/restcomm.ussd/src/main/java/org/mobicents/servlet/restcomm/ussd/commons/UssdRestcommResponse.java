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
package org.mobicents.servlet.restcomm.ussd.commons;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This class represent the USSD response message that Restcomm creates after parsing the RCML application
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdRestcommResponse {

    private String message;
    private String language;
    private int messageLength;
    private UssdMessageType messageType;
    private Boolean isFinalMessage = true;
    private String ussdCollectAction;
    private String errorCode;

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
        streamWriter.writeCharacters("\n");
        streamWriter.writeStartElement("ussd-data");
        streamWriter.writeCharacters("\n");

        if (getLanguage() != null) {
            // Write Language element
            streamWriter.writeStartElement("language");
            streamWriter.writeAttribute("value", getLanguage());
            streamWriter.writeEndElement();
            streamWriter.writeCharacters("\n");
        }

        if (getMessage() != null) {
            // Write ussd-string
            streamWriter.writeStartElement("ussd-string");
            streamWriter.writeAttribute("value", getMessage());
            streamWriter.writeEndElement();
            streamWriter.writeCharacters("\n");
        }

        if (getUssdMessageType() != null) {
            streamWriter.writeStartElement("anyExt");
            streamWriter.writeCharacters("\n");
            streamWriter.writeStartElement("message-type");
            streamWriter.writeCharacters(getUssdMessageType().name());
            streamWriter.writeEndElement();
            streamWriter.writeCharacters("\n");
            streamWriter.writeEndElement();
            streamWriter.writeCharacters("\n");
        }

        if (getErrorCode() != null) {
            // Write error code
            streamWriter.writeStartElement("error-code");
            streamWriter.writeAttribute("value", getErrorCode());
            streamWriter.writeEndElement();
            streamWriter.writeCharacters("\n");
        }

        streamWriter.writeEndElement();
        streamWriter.writeCharacters("\n");
        streamWriter.writeEndDocument();

        streamWriter.flush();
        streamWriter.close();

        return writer.toString();
    }

    /**
     * @param ussdCollectAction
     */
    public void setUssdCollectAction(String ussdCollectAction) {
        this.ussdCollectAction = ussdCollectAction;
    }

    public String getUssdCollectAction() {
        return ussdCollectAction;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "UssdRestCommResponse :"+message+" messageType: "+messageType.name()+" isFinalMessage: "+isFinalMessage;
    }
}