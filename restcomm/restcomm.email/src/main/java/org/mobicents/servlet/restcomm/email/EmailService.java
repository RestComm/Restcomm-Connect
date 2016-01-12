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
package org.mobicents.servlet.restcomm.email;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.api.EmailRequest;
import org.mobicents.servlet.restcomm.api.EmailResponse;
import org.mobicents.servlet.restcomm.api.Mail;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public class EmailService extends UntypedActor  {

    final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final List<ActorRef> observers;
    private Configuration configuration;
    private Session session;
    private String host;
    private String port;
    private String user;
    private String password;

    public EmailService(final Configuration config) {
        this.observers = new ArrayList<ActorRef>();
        configuration = config;
        host = configuration.getString("host");
        port = configuration.getString("port");
        user = configuration.getString("user");
        password = configuration.getString("password");
        final Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        if (user != null && !user.isEmpty()) {
            properties.setProperty("mail.smtp.user", user);
        }
        if (password != null && !password.isEmpty()) {
            properties.setProperty("mail.smtp.password", password);
        }
        if (port != null && !port.isEmpty()) {
            properties.setProperty("mail.smtp.port", port);
        }

        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.transport.protocol", "smtps");
        // properties.setProperty("mail.smtp.ssl.enable", "true");
        properties.setProperty("mail.smtp.auth", "true");

        session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();

        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        }else if (EmailRequest.class.equals(klass)) {
            EmailRequest request = (EmailRequest)message;
            sender.tell(send(request.getObject()), self);
        }
    }

     EmailResponse send(final Mail mail) {
        try {
            InternetAddress from;
            if (mail.from() != null || !mail.from().equalsIgnoreCase("")) {
                from = new InternetAddress(mail.from());
            } else {
                from = new InternetAddress(user);
            }
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
