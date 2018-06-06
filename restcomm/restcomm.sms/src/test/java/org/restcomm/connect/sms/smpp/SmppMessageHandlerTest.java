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
package org.restcomm.connect.sms.smpp;

import java.util.ArrayList;
import java.util.List;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import org.apache.commons.configuration.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.restcomm.connect.core.service.api.NumberSelectorService;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.SmsMessagesDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.SmsMessage;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.sms.smpp.dlr.spi.DLRPayload;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class SmppMessageHandlerTest {

    private static ActorSystem system;

    @BeforeClass
    public static void beforeAll() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void afterAll() {
        system.shutdown();
    }

    @Test
    public void testRemover() {
        List<IncomingPhoneNumber> numbers = new ArrayList();
        IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();

        builder.setPhoneNumber("123.");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("123*");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("^123");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber(".");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("[5]");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("1|2");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("\\d");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("+1234");
        numbers.add(builder.build());
        builder.setPhoneNumber("+1234*");
        numbers.add(builder.build());
        builder.setPhoneNumber("+1234.*");
        numbers.add(builder.build());
        builder.setPhoneNumber("9887");
        numbers.add(builder.build());
        builder.setPhoneNumber("+");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertEquals(3, numbers.size());
    }

    @Test
    public void testOnReceiveDlrPayload() {
        new JavaTestKit(system) {
            {
                // given
                final ServletContext servletContext = mock(ServletContext.class);
                final Configuration configuration = mock(Configuration.class);
                final SipFactory sipFactory = mock(SipFactory.class);
                final MonitoringService monitoringService = mock(MonitoringService.class);
                final NumberSelectorService numberSelectorService = mock(NumberSelectorService.class);

                final DaoManager daoManager = mock(DaoManager.class);
                final SmsMessagesDao smsMessagesDao = mock(SmsMessagesDao.class);
                final SmsMessage smsMessage = mock(SmsMessage.class);

                final DLRPayload dlrPayload = new DLRPayload();
                dlrPayload.setId("12345");
                dlrPayload.setStat(SmsMessage.Status.QUEUED);

                when(servletContext.getAttribute(DaoManager.class.getName())).thenReturn(daoManager);
                when(servletContext.getAttribute(Configuration.class.getName())).thenReturn(configuration);
                when(servletContext.getAttribute(SipFactory.class.getName())).thenReturn(sipFactory);
                when(servletContext.getAttribute(MonitoringService.class.getName())).thenReturn(mock(ActorRef.class));
                when(servletContext.getAttribute(NumberSelectorService.class.getName())).thenReturn(numberSelectorService);

                when(daoManager.getSmsMessagesDao()).thenReturn(smsMessagesDao);
                when(smsMessagesDao.getSmsMessageBySmppMessageId(dlrPayload.getId())).thenReturn(smsMessage);
                when(smsMessage.setStatus(any(SmsMessage.Status.class))).thenReturn(smsMessage);

                final ActorRef messageHandler = system.actorOf(Props.apply(new Creator<Actor>() {
                    @Override
                    public Actor create() throws Exception {
                        return new SmppMessageHandler(servletContext);
                    }
                }));

                // when

                messageHandler.tell(dlrPayload, getRef());

                // then
                verify(smsMessage, timeout(100)).setSmppMessageId(null);
                verify(smsMessage, timeout(100)).setStatus(dlrPayload.getStat());
                verify(smsMessagesDao, timeout(100)).updateSmsMessage(any(SmsMessage.class));
            }
        };
    }

    @Test
    public void testOnReceiveDlrPayloadForUnknownSms() {
        new JavaTestKit(system) {
            {
                // given
                final ServletContext servletContext = mock(ServletContext.class);
                final Configuration configuration = mock(Configuration.class);
                final SipFactory sipFactory = mock(SipFactory.class);
                final MonitoringService monitoringService = mock(MonitoringService.class);
                final NumberSelectorService numberSelectorService = mock(NumberSelectorService.class);

                final DaoManager daoManager = mock(DaoManager.class);
                final SmsMessagesDao smsMessagesDao = mock(SmsMessagesDao.class);

                final DLRPayload dlrPayload = new DLRPayload();
                dlrPayload.setId("12345");

                when(servletContext.getAttribute(DaoManager.class.getName())).thenReturn(daoManager);
                when(servletContext.getAttribute(Configuration.class.getName())).thenReturn(configuration);
                when(servletContext.getAttribute(SipFactory.class.getName())).thenReturn(sipFactory);
                when(servletContext.getAttribute(MonitoringService.class.getName())).thenReturn(mock(ActorRef.class));
                when(servletContext.getAttribute(NumberSelectorService.class.getName())).thenReturn(numberSelectorService);

                when(daoManager.getSmsMessagesDao()).thenReturn(smsMessagesDao);
                when(smsMessagesDao.getSmsMessageBySmppMessageId(dlrPayload.getId())).thenReturn(null);

                final ActorRef messageHandler = system.actorOf(Props.apply(new Creator<Actor>() {
                    @Override
                    public Actor create() throws Exception {
                        return new SmppMessageHandler(servletContext);
                    }
                }));

                // when

                messageHandler.tell(dlrPayload, getRef());

                // then
                verify(smsMessagesDao, never()).updateSmsMessage(any(SmsMessage.class));
            }
        };
    }

}
