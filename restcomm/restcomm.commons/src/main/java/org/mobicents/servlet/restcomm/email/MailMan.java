package org.mobicents.servlet.restcomm.email;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.configuration.Configuration;

public final class MailMan extends UntypedActor {
  // Logger.
  private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
  // Email client session.
  private final Session session;

  public MailMan(final Configuration configuration) {
    super();
    final String host = configuration.getString("host");
    final String user = configuration.getString("user");
    final String password = configuration.getString("password");
    final Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host", host);
    if(user != null && !user.isEmpty()) {
      properties.setProperty("mail.user", user);
    }
    if(password != null && !password.isEmpty()) {
      properties.setProperty("mail.password", password);
    }
    session = Session.getDefaultInstance(properties);
  }

  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    if(Mail.class.equals(klass)) {
      send(message);
    }
  }
  
  private void send(final Object message) {
    final Mail mail = (Mail)message;
    try {
      final InternetAddress from = new InternetAddress(mail.from());
      final InternetAddress to = new InternetAddress(mail.to());
      final MimeMessage email = new MimeMessage(session);
      email.setFrom(from);
      email.addRecipient(Message.RecipientType.TO, to);
      email.setSubject(mail.subject());
      email.setText(mail.body());
      Transport.send(email);
    } catch(final MessagingException exception) {
      logger.error(exception.getMessage(), exception);
    }
  }
}
