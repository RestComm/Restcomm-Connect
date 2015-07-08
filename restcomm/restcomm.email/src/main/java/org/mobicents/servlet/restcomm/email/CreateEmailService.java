package org.mobicents.servlet.restcomm.email;


import org.apache.commons.configuration.Configuration;
import akka.actor.Actor;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;


public final class CreateEmailService {

    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static final class Builder {

        private Configuration configuration;
        private Session session;
        private String host;
        private String port;
        private String user;
        private String password;

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
            return new MailService(session); //Email Tag Service.
        }
    }
}
