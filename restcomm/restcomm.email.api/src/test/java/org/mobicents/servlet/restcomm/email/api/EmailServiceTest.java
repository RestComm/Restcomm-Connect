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
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import org.mobicents.servlet.restcomm.email.EmailResponse;
import org.mobicents.servlet.restcomm.email.EmailRequest;
import org.mobicents.servlet.restcomm.email.Mail;


/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
public final class EmailServiceTest {
    private ActorSystem system;
    private ActorRef emailService;

    public EmailServiceTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        final URL input = getClass().getResource("/emailServiceTest.xml");
        final XMLConfiguration configuration = new XMLConfiguration(input);
        emailService = emailService(configuration);
    }

    @After
    public void after() throws Exception {
        system.shutdown();
    }

    private ActorRef emailService(final Configuration configuration) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                final CreateEmailService builder = new EmailService();
                builder.CreateEmailSession(configuration);
                return builder.build();
            }
        }));
    }

    @Test
    public void testSendEmail() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Send the email.
                final Mail emailMsg = new Mail("from@****.com", "to@****.com","Testing Email Service" , "This is the subject of the email service testing");
                emailService.tell(new EmailRequest(emailMsg), observer);

                final EmailResponse response = expectMsgClass(FiniteDuration.create(60, TimeUnit.SECONDS), EmailResponse.class);
                assertTrue(response.succeeded());
                System.out.println(response.get().toString());
            }
        };
    }
}
