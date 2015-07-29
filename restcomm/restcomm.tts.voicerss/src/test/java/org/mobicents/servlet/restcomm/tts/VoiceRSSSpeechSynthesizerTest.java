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
package org.mobicents.servlet.restcomm.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.Actor;
import akka.japi.Creator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
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

import org.mobicents.servlet.restcomm.util.Pre23Props;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;

/**
 * @author gvagenas@gmail.com (George Vagenas)
 */
public final class VoiceRSSSpeechSynthesizerTest {
    private ActorSystem system;
    private ActorRef tts;
    private ActorRef cache;
    private String tempSystemDirectory;

    public VoiceRSSSpeechSynthesizerTest() throws ConfigurationException {
        super();
    }

    @Before
    public void before() throws Exception {
        system = ActorSystem.create();
        final URL input = getClass().getResource("/voicerss.xml");
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

        return system.actorOf(Pre23Props.create(new Creator<Actor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                return (Actor) Class.forName(classpath).getConstructor(Configuration.class).newInstance(configuration);
            }
        }));
    }

    private ActorRef cache(final String path, final String uri) {
        return system.actorOf(Pre23Props.create(new Creator<Actor>(){
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
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
                assertTrue(languages.contains("zh"));
                assertTrue(languages.contains("zh-hk"));
                assertTrue(languages.contains("zh-tw"));
                assertTrue(languages.contains("da"));
                assertTrue(languages.contains("nl"));
                assertTrue(languages.contains("en-au"));
                assertTrue(languages.contains("en-ca"));
                assertTrue(languages.contains("en-gb"));
                assertTrue(languages.contains("en-in"));
                assertTrue(languages.contains("en"));
                assertTrue(languages.contains("fi"));
                assertTrue(languages.contains("fr-ca"));
                assertTrue(languages.contains("fr"));
                assertTrue(languages.contains("de"));
                assertTrue(languages.contains("it"));
                assertTrue(languages.contains("ja"));
                assertTrue(languages.contains("ko"));
                assertTrue(languages.contains("nb"));
                assertTrue(languages.contains("pl"));
                assertTrue(languages.contains("pt-br"));
                assertTrue(languages.contains("pt"));
                assertTrue(languages.contains("ru"));
                assertTrue(languages.contains("es-mx"));
                assertTrue(languages.contains("es"));
                assertTrue(languages.contains("sv"));
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSynthesisMan() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "man";
                String woman = "woman";
                String language = "en";
                String message = "Hello TTS World!";

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
    public void testSynthesisWoman() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                String gender = "woman";
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

                assertEquals(tempSystemDirectory + hash + ".wav", response.get().toString());
                assertEquals("http://127.0.0.1:8080/restcomm/cache/" + hash + ".wav", diskCacheResponse.get().toString());

                FileUtils.deleteQuietly(new File(response.get()));
            }
        };
    }

}
