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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.restcomm.cache.HashGenerator;
import org.mobicents.servlet.restcomm.tts.api.GetSpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerException;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerInfo;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerRequest;
import org.mobicents.servlet.restcomm.tts.api.SpeechSynthesizerResponse;
import org.mobicents.servlet.restcomm.util.HttpUtils;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class AcapelaSpeechSynthesizer extends UntypedActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private static final String client = "0-01";
    private static final String environment = "RESTCOMM_1.6.0";
    private static final String mime = "WAV";
    private static final String version = "2";

    private static final List<NameValuePair> parameters;
    static {
        parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("prot_vers", version));
        parameters.add(new BasicNameValuePair("cl_env", environment));
        parameters.add(new BasicNameValuePair("cl_vers", client));
        parameters.add(new BasicNameValuePair("req_type", null));
        parameters.add(new BasicNameValuePair("req_snd_id", null));
        parameters.add(new BasicNameValuePair("req_vol", null));
        parameters.add(new BasicNameValuePair("req_spd", null));
        parameters.add(new BasicNameValuePair("req_vct", null));
        parameters.add(new BasicNameValuePair("req_eq1", null));
        parameters.add(new BasicNameValuePair("req_eq2", null));
        parameters.add(new BasicNameValuePair("req_eq3", null));
        parameters.add(new BasicNameValuePair("req_eq4", null));
        parameters.add(new BasicNameValuePair("req_snd_type", mime));
        parameters.add(new BasicNameValuePair("req_snd_ext", null));
        parameters.add(new BasicNameValuePair("req_snd_kbps", null));
        parameters.add(new BasicNameValuePair("req_alt_snd_type", null));
        parameters.add(new BasicNameValuePair("req_alt_snd_ext", null));
        parameters.add(new BasicNameValuePair("req_alt_snd_kbps", null));
        parameters.add(new BasicNameValuePair("req_wp", null));
        parameters.add(new BasicNameValuePair("req_bp", null));
        parameters.add(new BasicNameValuePair("req_mp", null));
        parameters.add(new BasicNameValuePair("req_comment", null));
        parameters.add(new BasicNameValuePair("req_start_time", null));
        parameters.add(new BasicNameValuePair("req_timeout", null));
        parameters.add(new BasicNameValuePair("req_asw_type", null));
        parameters.add(new BasicNameValuePair("req_asw_as_alt_snd", null));
        parameters.add(new BasicNameValuePair("req_err_as_id3", null));
        parameters.add(new BasicNameValuePair("req_echo", null));
        parameters.add(new BasicNameValuePair("req_asw_redirect_url", null));
    }

    private final URI service;
    private final Map<String, String> men;
    private final Map<String, String> women;

    public AcapelaSpeechSynthesizer(final Configuration configuration) {
        super();
        // Add the credentials.
        final String application = configuration.getString("application");
        final String login = configuration.getString("login");
        final String password = configuration.getString("password");
        parameters.add(new BasicNameValuePair("cl_app", application));
        parameters.add(new BasicNameValuePair("cl_login", login));
        parameters.add(new BasicNameValuePair("cl_pwd", password));
        // Initialize the speech synthesizer state.
        service = URI.create(configuration.getString("service-root"));
        men = new HashMap<String, String>();
        women = new HashMap<String, String>();
        load(configuration);
    }

    private SpeechSynthesizerInfo info() {
        return new SpeechSynthesizerInfo(women.keySet());
    }

    private void load(final Configuration configuration) throws RuntimeException {
        // Initialize female voices.
        women.put("bf", configuration.getString("speakers.belgium-french.female"));
        women.put("bp", configuration.getString("speakers.brazilian-portuguese.female"));
        women.put("en-gb", configuration.getString("speakers.british-english.female"));
        women.put("cf", configuration.getString("speakers.canadian-french.female"));
        women.put("cs", configuration.getString("speakers.czech.female"));
        women.put("dan", configuration.getString("speakers.danish.female"));
        women.put("en", configuration.getString("speakers.english.female"));
        women.put("fi", configuration.getString("speakers.finnish.female"));
        women.put("fr", configuration.getString("speakers.french.female"));
        women.put("de", configuration.getString("speakers.german.female"));
        women.put("el", configuration.getString("speakers.greek.female"));
        women.put("it", configuration.getString("speakers.italian.female"));
        women.put("nl", configuration.getString("speakers.netherlands-dutch.female"));
        women.put("no", configuration.getString("speakers.norwegian.female"));
        women.put("pl", configuration.getString("speakers.polish.female"));
        women.put("pt", configuration.getString("speakers.portuguese.female"));
        women.put("ru", configuration.getString("speakers.russian.female"));
        women.put("ar", configuration.getString("speakers.saudi-arabia-arabic.female"));
        women.put("ca", configuration.getString("speakers.spain-catalan.female"));
        women.put("es", configuration.getString("speakers.spanish.female"));
        women.put("sv", configuration.getString("speakers.swedish.female"));
        women.put("tr", configuration.getString("speakers.turkish.female"));
        // Initialize male voices.
        men.put("bf", configuration.getString("speakers.belgium-french.male"));
        men.put("bp", configuration.getString("speakers.brazilian-portuguese.male"));
        men.put("en-gb", configuration.getString("speakers.british-english.male"));
        men.put("cf", configuration.getString("speakers.canadian-french.male"));
        men.put("cs", configuration.getString("speakers.czech.male"));
        men.put("dan", configuration.getString("speakers.danish.male"));
        men.put("en", configuration.getString("speakers.english.male"));
        men.put("fi", configuration.getString("speakers.finnish.male"));
        men.put("fr", configuration.getString("speakers.french.male"));
        men.put("de", configuration.getString("speakers.german.male"));
        men.put("el", configuration.getString("speakers.greek.male"));
        men.put("it", configuration.getString("speakers.italian.male"));
        men.put("nl", configuration.getString("speakers.netherlands-dutch.male"));
        men.put("no", configuration.getString("speakers.norwegian.male"));
        men.put("pl", configuration.getString("speakers.polish.male"));
        men.put("pt", configuration.getString("speakers.portuguese.male"));
        men.put("ru", configuration.getString("speakers.russian.male"));
        men.put("ar", configuration.getString("speakers.saudi-arabia-arabic.male"));
        men.put("ca", configuration.getString("speakers.spain-catalan.male"));
        men.put("es", configuration.getString("speakers.spanish.male"));
        men.put("sv", configuration.getString("speakers.swedish.male"));
        men.put("tr", configuration.getString("speakers.turkish.male"));
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

    private String speaker(final String gender, final String language) {
        String speaker = null;
        if ("woman".equalsIgnoreCase(gender)) {
            speaker = women.get(language);
            if (speaker == null || speaker.isEmpty()) {
                speaker = men.get(language);
            }
        } else if ("man".equalsIgnoreCase(gender)) {
            speaker = men.get(language);
            if (speaker == null || speaker.isEmpty()) {
                speaker = women.get(language);
            }
        } else {
            return null;
        }
        return speaker;
    }

    private URI synthesize(final Object message) throws IOException, SpeechSynthesizerException {
        final SpeechSynthesizerRequest request = (SpeechSynthesizerRequest) message;
        final String gender = request.gender();
        final String language = request.language();
        final String text = request.text();
        final String speaker = speaker(gender, language);
        if (speaker == null) {
            logger.info("There is no suitable speaker to synthesize " + request.language());
            throw new IllegalArgumentException("There is no suitable speaker to synthesize " + request.language());
        }
        final List<NameValuePair> query = new ArrayList<NameValuePair>();
        query.addAll(parameters);
        query.add(new BasicNameValuePair("req_voice", speaker));
        query.add(new BasicNameValuePair("req_text", text));
        final HttpPost post = new HttpPost(service);
        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(query, "UTF-8");
        post.setEntity(entity);
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(post);
        final StatusLine line = response.getStatusLine();
        final int status = line.getStatusCode();
        if (status == HttpStatus.SC_OK) {
            final Map<String, String> results = HttpUtils.toMap(response.getEntity());
            if ("OK".equals(results.get("res"))) {
                logger.info("AcapelaSpeechSynthesizer success!");
                String ret = results.get("snd_url") + "#hash=" + HashGenerator.hashMessage(gender, language, text);
                return URI.create(ret);
            } else {
                logger.info("AcapelaSpeechSynthesizer error code: " + results.get("err_code") + " error message: "
                        + results.get("err_msg"));
                final StringBuilder buffer = new StringBuilder();
                buffer.append(results.get("err_code")).append(" ").append(results.get("err_msg"));
                throw new SpeechSynthesizerException(buffer.toString());
            }
        } else {
            logger.info("AcapelaSpeechSynthesizer error, status code: " + line.getStatusCode() + (" reason phrase: ")
                    + line.getReasonPhrase());
            final StringBuilder buffer = new StringBuilder();
            buffer.append(line.getStatusCode()).append(" ").append(line.getReasonPhrase());
            throw new SpeechSynthesizerException(buffer.toString());
        }
    }
}
