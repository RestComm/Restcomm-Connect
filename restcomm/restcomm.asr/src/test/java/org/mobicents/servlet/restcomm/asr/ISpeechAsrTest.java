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
package org.mobicents.servlet.restcomm.asr;

import akka.actor.*;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.mobicents.servlet.restcomm.util.Pre23Props;
import scala.concurrent.duration.FiniteDuration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ISpeechAsrTest {
    private ActorSystem system;
    private ActorRef asr;

    public ISpeechAsrTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        final URL input = getClass().getResource("/ispeech.xml");
        final XMLConfiguration configuration = new XMLConfiguration(input);
        asr = asr(configuration);
    }

    @After
    public void after() throws Exception {
        system.shutdown();
    }

    private ActorRef asr(final Configuration configuration) {
        return system.actorOf(Pre23Props.create(new Creator<Actor>() {
            @Override
            public UntypedActor create() throws Exception {
                return new ISpeechAsr(configuration);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInfo() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                asr.tell(new GetAsrInfo(), observer);
                final AsrResponse<AsrInfo> response = expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
                        AsrResponse.class);
                assertTrue(response.succeeded());
                final Set<String> languages = response.get().languages();
                assertTrue(languages.contains("en"));
                assertTrue(languages.contains("en-gb"));
                assertTrue(languages.contains("es"));
                assertTrue(languages.contains("it"));
                assertTrue(languages.contains("fr"));
                assertTrue(languages.contains("pl"));
                assertTrue(languages.contains("pt"));
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test @Ignore //To pass the test the ISpeech account have to be active
    public void testRecognition() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                final File file = new File(getClass().getResource("/hello-world.wav").getPath());
                final AsrRequest request = new AsrRequest(file, "en");
                asr.tell(request, observer);
                final AsrResponse<String> response = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
                        AsrResponse.class);
                assertTrue(response.succeeded());
                assertTrue(response.get().equals("hello world"));
            }
        };
    }
}
