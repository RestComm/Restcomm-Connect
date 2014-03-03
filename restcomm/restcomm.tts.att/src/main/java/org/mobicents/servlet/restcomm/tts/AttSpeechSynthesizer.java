/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import naturalvoices.ClientPlayer;
import naturalvoices.Player;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.cache.HashGenerator;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerException;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public final class AttSpeechSynthesizer extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final Map<String, String> men;
    private final Map<String, String> women;

    private final String rootDir;
    private final Player player;

    public AttSpeechSynthesizer(final Configuration configuration) {
        super();
        men = new ConcurrentHashMap<String, String>();
        women = new ConcurrentHashMap<String, String>();
        load(configuration);
        rootDir = configuration.getString("tts-client-directory");
        player = new ClientPlayer(rootDir, configuration.getString("host"), configuration.getInt("port", 7000));
        player.Verbose = configuration.getBoolean("verbose-output",false);
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();

        if (SpeechSynthesizerRequest.class.equals(klass)) {
            try {
                final URI uri = synthesize(message);
                if (sender != null) {
                    sender.tell(new SpeechSynthesizerResponse<URI>(uri), self);
                }
            } catch (final Exception exception) {
                if (sender != null) {
                    sender.tell(new SpeechSynthesizerResponse<URI>(exception), self);
                }
            }
        } else if (GetSpeechSynthesizerInfo.class.equals(klass)) {
            sender.tell(new SpeechSynthesizerResponse<SpeechSynthesizerInfo>(info()), self);
        }
    }

    private SpeechSynthesizerInfo info() {
        return new SpeechSynthesizerInfo(men.keySet());
    }

    private void load(final Configuration configuration) throws RuntimeException {
        // Initialize female voices.
        women.put("en", configuration.getString("speakers.english.female"));
        women.put("en-uk", configuration.getString("speakers.english-uk.female"));
        women.put("es", configuration.getString("speakers.spanish.female"));
        women.put("fr", configuration.getString("speakers.french.female"));
        women.put("de-de", configuration.getString("speakers.german.female"));
        women.put("it-it", configuration.getString("speakers.italian.female"));
        women.put("pt-br", configuration.getString("speakers.brazilian-portuguese.female"));

        // Initialize male voices.
        men.put("en", configuration.getString("speakers.english.male"));
        men.put("en-uk", configuration.getString("speakers.english-uk.male"));
        men.put("es", configuration.getString("speakers.spanish.male"));
        men.put("fr", configuration.getString("speakers.french.male"));
        men.put("fr-ca", configuration.getString("speakers.canadian-french.male"));
        men.put("de", configuration.getString("speakers.german.male"));
        men.put("it", configuration.getString("speakers.italian.male"));
        men.put("pt-br", configuration.getString("speakers.brazilian-portuguese.male"));
    }

    private URI synthesize(final Object message) throws IOException, SpeechSynthesizerException {
        final SpeechSynthesizerRequest request = (SpeechSynthesizerRequest) message;

        final String gender = request.gender();
        final String language = request.language();
        final String text = request.text();

        if (language == null) {
            logger.info("There is no suitable speaker to synthesize " + request.language());
            throw new IllegalArgumentException("There is no suitable language to synthesize " + request.language());
        }

        final String hash = HashGenerator.hashMessage(gender, language, text);



        //setup parameters as needed
        if(gender.equalsIgnoreCase("man")) {
            player.setVoice(men.get(language));
        } else {
            player.setVoice(women.get(language));
        }

        player.setLatin1(true);

        //source text to play
        player.setSourceText(text);

        //save it to a file
        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + hash + ".wav");
        player.Convert(file.getAbsolutePath());

        if(file.exists()) {
            return file.toURI();
        } else {
            logger.info("There was a problem with AT&T TTS server. Check configuration and that TTS Server is running");
            throw new SpeechSynthesizerException("There was a problem with TTSClientFile. Check configuration and that TTS Server is running");
        }
    }
}
