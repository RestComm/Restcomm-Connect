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
package org.mobicents.servlet.restcomm.email.api;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.mobicents.servlet.restcomm.email.EmailResponse;
import org.mobicents.servlet.restcomm.email.Mail;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public class EmailInterpreter extends CreateEmailInterpreter {

    public EmailInterpreter(final Session session) { //Constructor for Email-service
        super(session);
    }

     EmailResponse send(final Mail mail) {
        try {
            final InternetAddress from = new InternetAddress(mail.from());
            final InternetAddress to = new InternetAddress(mail.to());
            final MimeMessage email = new MimeMessage(session);
            email.setFrom(from);
            email.addRecipient(Message.RecipientType.TO, to);
            email.setSubject(mail.subject());
            email.setText(mail.body());
            email.addRecipients(Message.RecipientType.CC, InternetAddress.parse(mail.cc(), false));
            email.addRecipients(Message.RecipientType.BCC,InternetAddress.parse(mail.bcc(),false));
            Transport.send(email);
            return new EmailResponse(mail);
        } catch (final MessagingException exception) {
            logger.error(exception.getMessage(), exception);
            return new EmailResponse(exception,exception.getMessage());
        }
    }
}
