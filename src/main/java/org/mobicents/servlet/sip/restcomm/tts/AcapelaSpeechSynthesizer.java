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
import org.mobicents.servlet.sip.restcomm.util.UriUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class AcapelaSpeechSynthesizer implements SpeechSynthesizer {
  private static final String CLIENT_VERSION = "0-01";
  private static final String ENVIRONMENT = "RESTCOMM_1.0.0";
  private static final String PROTOCOL_VERSION = "2";
  private static final String SOUND_FILE_TYPE = "WAV";
  private static final List<NameValuePair> DEFAULT_PARAMETERS;
  static {
    DEFAULT_PARAMETERS = new ArrayList<NameValuePair>(28);
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("prot_vers", PROTOCOL_VERSION));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("cl_env", ENVIRONMENT));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("cl_vers", CLIENT_VERSION));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_type", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_snd_id", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_vol", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_spd", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_vct", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_eq1", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_eq2", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_eq3", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_eq4", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_snd_type", SOUND_FILE_TYPE));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_snd_ext", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_snd_kbps", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_alt_snd_type", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_alt_snd_ext", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_alt_snd_kbps", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_wp", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_bp", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_mp", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_comment", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_start_time", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_timeout", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_asw_type", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_asw_as_alt_snd", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_err_as_id3", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_echo", null));
    DEFAULT_PARAMETERS.add(new BasicNameValuePair("req_asw_redirect_url", null));
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
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  private String findSpeaker(final String gender, final String language) throws IllegalArgumentException {
    String speaker = null;
	if("woman".equalsIgnoreCase(gender)) {
	  speaker = women.get(language);
	} else if("man".equalsIgnoreCase(gender)) {
	  speaker = men.get(language);
	} else {
	  throw new IllegalArgumentException(gender + " is not a valid gender.");
	}
	return speaker;
  }
  
  private void loadVoices() throws RuntimeException {
    women = new HashMap<String, String>();
    men = new HashMap<String, String>();
    women.put("en", configuration.getString("speakers.english.female"));
    women.put("en-gb", configuration.getString("speakers.british-english.female"));
    women.put("es", configuration.getString("speakers.spanish.female"));
    women.put("fr", configuration.getString("speakers.french.female"));
    women.put("de", configuration.getString("speakers.german.female"));
    men.put("en", configuration.getString("speakers.english.male"));
    men.put("en-gb", configuration.getString("speakers.british-english.male"));
    men.put("es", configuration.getString("speakers.spanish.male"));
    men.put("fr", configuration.getString("speakers.french.male"));
    men.put("de", configuration.getString("speakers.german.male"));
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
      throws IllegalArgumentException, SpeechSynthesizerException {
    final List<NameValuePair> parameters = new ArrayList<NameValuePair>(33);
    parameters.addAll(DEFAULT_PARAMETERS);
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
	    final Map<String, String> results = UriUtils.parseEntity(response.getEntity());
	    if("OK".equals(results.get("res"))) {
	      final URI uri = URI.create(results.get("snd_url"));
	      final StringBuilder key = new StringBuilder();
	      key.append(language).append(":").append(gender).append(":").append(text);
	      return cache.put(key.toString(), uri);
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
}
