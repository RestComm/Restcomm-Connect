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
package org.mobicents.servlet.restcomm.tts.att;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * @author gvagenas@gmail.com (George Vagenas)
 */
@Ignore //Needs AT&T TTS server to be running so we ignore it and run it manually only
public final class ATTSpeechSynthesizerTest {
    private ActorSystem system;
    private ActorRef tts;
    private ActorRef cache;
    private String tempSystemDirectory;

    public ATTSpeechSynthesizerTest() throws ConfigurationException {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        final URL input = getClass().getResource("/att.xml");
        final XMLConfiguration configuration = new XMLConfiguration(input);
        tts = tts(configuration);
        cache = cache("/tmp/cache", "http://127.0.0.1:8080/restcomm/cache");
        // Fix for MacOS systems: only append "/" to temporary path if it doesnt end with it - hrosa
        String tmpDir = System.getProperty("java.io.tmpdir");
        tempSystemDirectory = "file:" + tmpDir + (tmpDir.endsWith("/") ? "" : "/");
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
                assertTrue(languages.contains("en"));
                assertTrue(languages.contains("en-uk"));
                assertTrue(languages.contains("es"));
                assertTrue(languages.contains("fr"));
                assertTrue(languages.contains("fr-ca"));
                assertTrue(languages.contains("de"));
                assertTrue(languages.contains("it"));
                assertTrue(languages.contains("pt-br"));
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSynthesisManEnglish() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "man";
                String woman = "woman";
                String language = "en";
                String message = "Hello TTS World! This is a test for AT&T TTS engine using man voice";

                String hash = HashGenerator.hashMessage(gender, language, message);
                String womanHash = HashGenerator.hashMessage(woman, language, message);

                assertTrue(!hash.equalsIgnoreCase(womanHash));

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

                assertEquals(tempSystemDirectory + hash + ".wav", response.get().toString());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

                FileUtils.deleteQuietly(new File(response.get()));
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSynthesisWomanEnglish() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "woman";
                String language = "en";
                String message = "Hello TTS World! This is a test for AT&T TTS engine using women voice";

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

                assertEquals(tempSystemDirectory + hash + ".wav", response.get().toString());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

                FileUtils.deleteQuietly(new File(response.get()));
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testSynthesisManSpanish() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "man";
                String woman = "woman";
                String language = "es";
                String message = "Hola TTS mundo! Esta es una prueba para el motor TTS AT&T utilizando la voz del hombre";

                String hash = HashGenerator.hashMessage(gender, language, message);
                String womanHash = HashGenerator.hashMessage(woman, language, message);

                assertTrue(!hash.equalsIgnoreCase(womanHash));

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

                assertEquals(tempSystemDirectory + hash + ".wav", response.get().toString());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

                FileUtils.deleteQuietly(new File(response.get()));
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSynthesisWomanSpanish() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "woman";
                String language = "es";
                String message = "Hola TTS mundo! Esta es una prueba para el motor TTS AT & T utilizando la mujer de voz";

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

                assertEquals(tempSystemDirectory + hash + ".wav", response.get().toString());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

                FileUtils.deleteQuietly(new File(response.get()));
            }
        };
    }
    
}
