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
package org.restcomm.connect.tts.awspolly;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.VoiceId;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.cache.HashGenerator;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import org.restcomm.connect.commons.util.PcmToWavConverterUtils;
import org.restcomm.connect.tts.api.GetSpeechSynthesizerInfo;
import org.restcomm.connect.tts.api.SpeechSynthesizerException;
import org.restcomm.connect.tts.api.SpeechSynthesizerInfo;
import org.restcomm.connect.tts.api.SpeechSynthesizerRequest;
import org.restcomm.connect.tts.api.SpeechSynthesizerResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ricardo.limonta@gmail.com (Ricardo Limonta)
 */
public class AWSPollySpeechSyntetizer extends RestcommUntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final AmazonPollyClient pollyClient;
    private final Map<String, String> men, women;

    public AWSPollySpeechSyntetizer(final Configuration configuration) {
        super();
        // retrieve polly api credentials
        final String awsAccessKey = configuration.getString("aws-access-key");
        final String awsSecretKey = configuration.getString("aws-secret-key");
        final String awsRegion = configuration.getString("aws-region");

        // Initialize the Amazon Cognito credentials provider.
        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        // Default region
        String region = "eu-west-1";

        //AWS Specific Region
        if ((awsRegion != null) && (!"".equals(awsRegion))) {
            region = awsRegion;
        }

        // Create a client that supports generation of presigned URLs.
        this.pollyClient = (AmazonPollyClient) AmazonPollyClientBuilder.standard()
                                                                       .withCredentials(new AWSStaticCredentialsProvider(credentials))
                                                                       .withRegion(region)
                                                                       .build();
        //initialize voice´s list
        men = new HashMap<>();
        women = new HashMap<>();

        load(configuration);
    }

    private void load(final Configuration configuration) throws RuntimeException {
        // Initialize female voices.
        women.put("ja", configuration.getString("speakers.ja-JP.female"));
        women.put("tr-tr", configuration.getString("speakers.tr-TR.female"));
        women.put("ru", configuration.getString("speakers.ru-RU.female"));
        women.put("ro", configuration.getString("speakers.ro-RO.female"));
        women.put("pt", configuration.getString("speakers.pt-PT.female"));
        women.put("bp", configuration.getString("speakers.pt-BR.female"));
        women.put("pl", configuration.getString("speakers.pl-PL.female"));
        women.put("nl", configuration.getString("speakers.nl-NL.female"));
        women.put("nb", configuration.getString("speakers.nb-NO.female"));
        women.put("it", configuration.getString("speakers.it-IT.female"));
        women.put("is", configuration.getString("speakers.is-IS.female"));
        women.put("fr", configuration.getString("speakers.fr-FR.female"));
        women.put("fr-ca", configuration.getString("speakers.fr-CA.female"));
        women.put("es-mx", configuration.getString("speakers.es-US.female"));
        women.put("es", configuration.getString("speakers.es-ES.female"));
        women.put("en-gb", configuration.getString("speakers.en-GB-WLS.female"));
        women.put("cy-gb", configuration.getString("speakers.cy-GB.female"));
        women.put("en", configuration.getString("speakers.en-US.female"));
        women.put("en-in", configuration.getString("speakers.en-IN.female"));
        women.put("en-gb", configuration.getString("speakers.en-GB.female"));
        women.put("en-ca", configuration.getString("speakers.en-GB.female"));
        women.put("en-au", configuration.getString("speakers.en-AU.female"));
        women.put("de", configuration.getString("speakers.de-DE.female"));
        women.put("da", configuration.getString("speakers.da-DK.female"));

        // Initialize male voices.
        men.put("ja", configuration.getString("speakers.ja-JP.male"));
        men.put("tr-tr", configuration.getString("speakers.tr-TR.male"));
        men.put("ru", configuration.getString("speakers.ru-RU.male"));
        men.put("ro", configuration.getString("speakers.ro-RO.male"));
        men.put("pt", configuration.getString("speakers.pt-PT.male"));
        men.put("bp", configuration.getString("speakers.pt-BR.male"));
        men.put("pl", configuration.getString("speakers.pl-PL.male"));
        men.put("nl", configuration.getString("speakers.nl-NL.male"));
        men.put("nb", configuration.getString("speakers.nb-NO.male"));
        men.put("it", configuration.getString("speakers.it-IT.male"));
        men.put("is", configuration.getString("speakers.is-IS.male"));
        men.put("fr", configuration.getString("speakers.fr-FR.male"));
        men.put("fr-ca", configuration.getString("speakers.fr-CA.male"));
        men.put("es-mx", configuration.getString("speakers.es-US.male"));
        men.put("es", configuration.getString("speakers.es-ES.male"));
        men.put("en-gb", configuration.getString("speakers.en-GB-WLS.male"));
        men.put("cy-gb", configuration.getString("speakers.cy-GB.male"));
        men.put("en", configuration.getString("speakers.en-US.male"));
        men.put("en-in", configuration.getString("speakers.en-IN.male"));
        men.put("en-gb", configuration.getString("speakers.en-GB.male"));
        men.put("en-ca", configuration.getString("speakers.en-GB.male"));
        men.put("en-au", configuration.getString("speakers.en-AU.male"));
        men.put("de", configuration.getString("speakers.de-DE.male"));
        men.put("da", configuration.getString("speakers.da-DK.male"));
    }

    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();

        if (SpeechSynthesizerRequest.class.equals(klass)) {
            try {
                final URI uri = synthesize(message);
                if (sender != null) {
                    sender.tell(new SpeechSynthesizerResponse<>(uri), self);
                }
            } catch (final IOException | SpeechSynthesizerException exception) {
                logger.error("There was an exception while trying to synthesize message: " + exception);
                if (sender != null) {
                    sender.tell(new SpeechSynthesizerResponse<URI>(exception), self);
                }
            }
        } else if (GetSpeechSynthesizerInfo.class.equals(klass)) {
            sender.tell(new SpeechSynthesizerResponse<>(new SpeechSynthesizerInfo(men.keySet())), self);
        }
    }

    private URI synthesize(final Object message) throws IOException, SpeechSynthesizerException {

        //retrieve resquest message
        final org.restcomm.connect.tts.api.SpeechSynthesizerRequest request = (org.restcomm.connect.tts.api.SpeechSynthesizerRequest) message;

        //retrieve gender, language and text message
        final String gender = request.gender();
        final String language = request.language();
        final String text = request.text();

        //generate file hash name
        final String hash = HashGenerator.hashMessage(gender, language, text);

        if (language == null) {
            if(logger.isInfoEnabled()){
                logger.info("There is no suitable speaker to synthesize " + request.language());
            }
            throw new IllegalArgumentException("There is no suitable language to synthesize " + request.language());
        }

        // Create speech synthesis request.
        SynthesizeSpeechRequest pollyRequest = new SynthesizeSpeechRequest().withText(text)
                                                                            .withVoiceId(VoiceId.valueOf(this.retrieveSpeaker(gender, language)))
                                                                            .withOutputFormat(OutputFormat.Pcm)
                                                                            .withSampleRate("8000");
        //retrieve audio result
        SynthesizeSpeechResult result = pollyClient.synthesizeSpeech(pollyRequest);

        //create a temporary file
        File srcFile = new File(System.getProperty("java.io.tmpdir") + File.separator + hash + ".pcm");
        File dstFile = new File(System.getProperty("java.io.tmpdir") + File.separator + hash + ".wav");

        //save temporary pcm file
        Files.copy(result.getAudioStream(), srcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        //convert pcm file to wav
        new PcmToWavConverterUtils().rawToWave(srcFile, dstFile);

        //return file URI
        return dstFile.toURI();
    }

    private String retrieveSpeaker(final String gender, final String language) {
        String speaker = null;
        if ("woman".equalsIgnoreCase(gender)) {
            speaker = women.get(language);
            if (speaker == null || speaker.isEmpty()) {
                speaker = men.get(language);
            }
        } else {
            speaker = men.get(language);
            if (speaker == null || speaker.isEmpty()) {
                speaker = women.get(language);
            }
        }

        //if speaker not found, set default speaker to en-US woman
        if (speaker == null) {
            speaker = "Joanna";
        }

        return speaker;
    }
}