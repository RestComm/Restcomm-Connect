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

import akka.actor.Actor;
import org.apache.commons.configuration.Configuration;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public class EmailService implements CreateEmailService {

        private Configuration configuration;
        private Session session;
        private String host;
        private String port;
        private String user;
        private String password;

        @Override
        public void CreateEmailSession(final Configuration config) {

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

        public String getUser() {
            return user;
        }

        public Actor build() {
            return new EmailInterpreter(session); //Email Tag Service.
        }

}

