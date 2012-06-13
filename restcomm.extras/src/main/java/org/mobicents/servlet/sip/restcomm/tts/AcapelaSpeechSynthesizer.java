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
package org.mobicents.servlet.sip.restcomm.tts;

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
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.cache.DiskCache;
import org.mobicents.servlet.sip.restcomm.util.HttpUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class AcapelaSpeechSynthesizer implements SpeechSynthesizer {
  private static final String clientVersion = "0-01";
  private static final String environment = "RESTCOMM_1.0.0";
  private static final String protocolVersion = "2";
  private static final String soundFileType = "WAV";
  private static final List<NameValuePair> defaultParameters;
  static {
    defaultParameters = new ArrayList<NameValuePair>(28);
    defaultParameters.add(new BasicNameValuePair("prot_vers", protocolVersion));
    defaultParameters.add(new BasicNameValuePair("cl_env", environment));
    defaultParameters.add(new BasicNameValuePair("cl_vers", clientVersion));
    defaultParameters.add(new BasicNameValuePair("req_type", null));
    defaultParameters.add(new BasicNameValuePair("req_snd_id", null));
    defaultParameters.add(new BasicNameValuePair("req_vol", null));
    defaultParameters.add(new BasicNameValuePair("req_spd", null));
    defaultParameters.add(new BasicNameValuePair("req_vct", null));
    defaultParameters.add(new BasicNameValuePair("req_eq1", null));
    defaultParameters.add(new BasicNameValuePair("req_eq2", null));
    defaultParameters.add(new BasicNameValuePair("req_eq3", null));
    defaultParameters.add(new BasicNameValuePair("req_eq4", null));
    defaultParameters.add(new BasicNameValuePair("req_snd_type", soundFileType));
    defaultParameters.add(new BasicNameValuePair("req_snd_ext", null));
    defaultParameters.add(new BasicNameValuePair("req_snd_kbps", null));
    defaultParameters.add(new BasicNameValuePair("req_alt_snd_type", null));
    defaultParameters.add(new BasicNameValuePair("req_alt_snd_ext", null));
    defaultParameters.add(new BasicNameValuePair("req_alt_snd_kbps", null));
    defaultParameters.add(new BasicNameValuePair("req_wp", null));
    defaultParameters.add(new BasicNameValuePair("req_bp", null));
    defaultParameters.add(new BasicNameValuePair("req_mp", null));
    defaultParameters.add(new BasicNameValuePair("req_comment", null));
    defaultParameters.add(new BasicNameValuePair("req_start_time", null));
    defaultParameters.add(new BasicNameValuePair("req_timeout", null));
    defaultParameters.add(new BasicNameValuePair("req_asw_type", null));
    defaultParameters.add(new BasicNameValuePair("req_asw_as_alt_snd", null));
    defaultParameters.add(new BasicNameValuePair("req_err_as_id3", null));
    defaultParameters.add(new BasicNameValuePair("req_echo", null));
    defaultParameters.add(new BasicNameValuePair("req_asw_redirect_url", null));
  }
  
  private Configuration configuration;
  private String application;
  private String login;
  private String password;
  private URI serviceRoot;
  private Map<String, String> women;
  private Map<String, String> men;
  private DiskCache cache;
  
  public AcapelaSpeechSynthesizer() {
    super();
  }
  
  public String buildKey(final String text, final String gender, final String language) {
    final StringBuilder key = new StringBuilder();
    key.append(language).append(":").append(gender).append(":").append(text);
    return key.toString();
  }
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  public URI getSpeech(final String text, final String gender, final String language)
      throws SpeechSynthesizerException {
    final List<NameValuePair> parameters = new ArrayList<NameValuePair>(33);
    parameters.addAll(defaultParameters);
    parameters.add(new BasicNameValuePair("cl_app", application));
    parameters.add(new BasicNameValuePair("cl_login", login));
    parameters.add(new BasicNameValuePair("cl_pwd", password));
    parameters.add(new BasicNameValuePair("req_voice", findSpeaker(gender, language)));
    parameters.add(new BasicNameValuePair("req_text", text));
    final HttpPost post = new HttpPost(serviceRoot);
    try {
	  post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
	  HttpClient client = new DefaultHttpClient();
	  final HttpResponse response = client.execute(post);
	  final int status = response.getStatusLine().getStatusCode();
	  if(status == HttpStatus.SC_OK) {
	    final Map<String, String> results = HttpUtils.toMap(response.getEntity());
	    if("OK".equals(results.get("res"))) {
	      final URI uri = URI.create(results.get("snd_url"));
	      return cache.put(buildKey(text, gender, language), uri);
	    } else {
	      final StringBuilder buffer = new StringBuilder();
	      buffer.append(results.get("err_code")).append(" ").append(results.get("err_msg"));
	      throw new SpeechSynthesizerException(buffer.toString());
	    }
	  } else {
	    final String reason = response.getStatusLine().getReasonPhrase();
	    final StringBuilder buffer = new StringBuilder();
	    buffer.append(status).append(" ").append(reason);
	    throw new IOException(buffer.toString());
	  }
	} catch(final Exception exception) {
      throw new SpeechSynthesizerException(exception);
	}
  }
  
  private String findSpeaker(final String gender, final String language) throws IllegalArgumentException {
    String speaker = null;
	if("woman".equalsIgnoreCase(gender)) {
	  speaker = women.get(language);
	  if(speaker == null || speaker.isEmpty()) {
	    speaker = men.get(language);
	  }
	} else if("man".equalsIgnoreCase(gender)) {
	  speaker = men.get(language);
	  if(speaker == null || speaker.isEmpty()) {
	    speaker = women.get(language);
	  }
	} else {
	  throw new IllegalArgumentException(gender + " is not a valid gender.");
	}
	return speaker;
  }
  
  @Override public boolean isSupported(final String language) throws IllegalArgumentException {
    String speaker = findSpeaker("woman", language);
    if(speaker == null || speaker.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }
  
  private void loadVoices() throws RuntimeException {
    women = new HashMap<String, String>();
    men = new HashMap<String, String>();
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
  
  @Override public void shutdown() {
    // Nothing to do.
  }

  @Override public void start() throws RuntimeException {
	application = configuration.getString("application");
    login = configuration.getString("login");
    password = configuration.getString("password");
    serviceRoot = URI.create(configuration.getString("service-root"));
    cache = new DiskCache(configuration.getString("cache-path"));
    loadVoices();
  }
  
  @Override public URI synthesize(final String text, final String gender, final String language)
	      throws SpeechSynthesizerException {
	final String key = buildKey(text, gender, language);
    if(cache.contains(key, soundFileType)) {
      return cache.get(key, soundFileType);
    } else {
      return getSpeech(text, gender, language);
    }
  }
}
