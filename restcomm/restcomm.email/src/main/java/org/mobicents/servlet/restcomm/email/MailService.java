package org.mobicents.servlet.restcomm.email;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.MessagingException;

import akka.actor.ActorRef;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;


import org.mobicents.servlet.restcomm.email.api.EmailResponse;
import org.mobicents.servlet.restcomm.email.api.EmailRequest;
import org.mobicents.servlet.restcomm.email.api.Mail;

public final class  MailService extends UntypedActor {
    // Logger.
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Email client session.
    private final Session session;


    public MailService(final Session session) { //Constructor for Email-service
        super();
        this.session = session;
    }


    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        EmailRequest request = (EmailRequest)message;
        if (EmailRequest.class.equals(klass)) {
            sender.tell(send(request.getObject()), self);
        }
    }

    private EmailResponse send(final Mail mail) {
        try {
            final InternetAddress from = new InternetAddress(mail.from());
            final InternetAddress to = new InternetAddress(mail.to());
            final MimeMessage email = new MimeMessage(session);
            email.setFrom(from);
            email.addRecipient(Message.RecipientType.TO, to);
            email.setSubject(mail.subject());
            email.setText(mail.body());
            email.setRecipients(Message.RecipientType.TO,InternetAddress.parse(mail.to(),false));
            Transport.send(email);
            return new EmailResponse(mail);
        } catch (final MessagingException exception) {
            logger.error(exception.getMessage(), exception);
            return new EmailResponse(exception,exception.getMessage());
        }
    }
}


