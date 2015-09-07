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

import javax.mail.Session;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.mobicents.servlet.restcomm.email.EmailRequest;
import org.mobicents.servlet.restcomm.email.EmailResponse;
import org.mobicents.servlet.restcomm.email.Mail;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public abstract class CreateEmailInterpreter extends UntypedActor {
    // Logger.
    final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    // Email client session.
    final Session session;

    public CreateEmailInterpreter(final Session session) { //Constructor for Email-service
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

    abstract EmailResponse send(final Mail mail);

}


