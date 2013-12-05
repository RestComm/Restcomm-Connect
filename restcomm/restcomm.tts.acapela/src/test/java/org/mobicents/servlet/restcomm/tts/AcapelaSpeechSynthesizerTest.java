/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.tts;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Ignore;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.servlet.restcomm.cache.DiskCache;
import org.mobicents.servlet.restcomm.cache.DiskCacheRequest;
import org.mobicents.servlet.restcomm.cache.DiskCacheResponse;
import org.mobicents.servlet.restcomm.cache.HashGenerator;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;

import scala.concurrent.duration.FiniteDuration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class AcapelaSpeechSynthesizerTest {
    private ActorSystem system;
    private ActorRef tts;
    private ActorRef cache;

    public AcapelaSpeechSynthesizerTest() {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        final URL input = getClass().getResource("/acapela.xml");
        final XMLConfiguration configuration = new XMLConfiguration(input);
        tts = tts(configuration);
        cache = cache("/tmp/cache", "http://127.0.0.1:8080/restcomm/cache");
    }

    @After
    public void after() throws Exception {
        system.shutdown();
    }

    private ActorRef tts(final Configuration configuration) {
        final String classpath = configuration.getString("[@class]");

        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return (UntypedActor) Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
            }
        }));
    }

    private ActorRef cache(final String path, final String uri) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new DiskCache(path, uri, true);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInfo() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                tts.tell(new GetSpeechSynthesizerInfo(), observer);
                final SpeechSynthesizerResponse<SpeechSynthesizerInfo> response = this.expectMsgClass(
                        FiniteDuration.create(30, TimeUnit.SECONDS), SpeechSynthesizerResponse.class);
                assertTrue(response.succeeded());
                final Set<String> languages = response.get().languages();
                assertTrue(languages.contains("ca"));
                assertTrue(languages.contains("it"));
                assertTrue(languages.contains("tr"));
                assertTrue(languages.contains("no"));
                assertTrue(languages.contains("ar"));
                assertTrue(languages.contains("cs"));
                assertTrue(languages.contains("de"));
                assertTrue(languages.contains("bp"));
                assertTrue(languages.contains("el"));
                assertTrue(languages.contains("dan"));
                assertTrue(languages.contains("fi"));
                assertTrue(languages.contains("pt"));
                assertTrue(languages.contains("pl"));
                assertTrue(languages.contains("bf"));
                assertTrue(languages.contains("sv"));
                assertTrue(languages.contains("fr"));
                assertTrue(languages.contains("en"));
                assertTrue(languages.contains("ru"));
                assertTrue(languages.contains("es"));
                assertTrue(languages.contains("cf"));
                assertTrue(languages.contains("nl"));
                assertTrue(languages.contains("en-gb"));
                System.out.println(response.get().languages());
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test @Ignore //Acapela account is expired
    public void testSynthesis() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "man";
                String language = "en";
                String message = "Hello TTS World!";
                String hash = HashGenerator.hashMessage(gender, language, message);

                final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(gender, language, message);
                tts.tell(synthesize, observer);
                final SpeechSynthesizerResponse<URI> response = this.expectMsgClass(
                        FiniteDuration.create(30, TimeUnit.SECONDS), SpeechSynthesizerResponse.class);
                assertTrue(response.succeeded());

                DiskCacheRequest diskCacheRequest = new DiskCacheRequest(response.get());
                cache.tell(diskCacheRequest, observer);

                final DiskCacheResponse diskCacheResponse = this.expectMsgClass(FiniteDuration.create(30, TimeUnit.SECONDS),
                        DiskCacheResponse.class);
                assertTrue(diskCacheResponse.succeeded());

                assertEquals("hash=" + hash, response.get().getFragment());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test @Ignore //Acapela account is expired
    public void testHash() {
        final String gender = "man";
        final String language = "en";
        final String text = "Hello There!";
        final String calculatedHash = "hash=" + HashGenerator.hashMessage(gender, language, text);

        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                final SpeechSynthesizerRequest synthesize = new SpeechSynthesizerRequest(gender, language, text);
                tts.tell(synthesize, observer);
                final SpeechSynthesizerResponse<URI> response = this.expectMsgClass(
                        FiniteDuration.create(30, TimeUnit.SECONDS), SpeechSynthesizerResponse.class);
                assertTrue(response.succeeded());
                assertTrue(response.get().getFragment().equals(calculatedHash));
                System.out.println(response.get().toString());
            }
        };
    }

}
