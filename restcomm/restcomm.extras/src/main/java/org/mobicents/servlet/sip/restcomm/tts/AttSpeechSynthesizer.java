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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class AttSpeechSynthesizer implements SpeechSynthesizer {
  private Configuration configuration;
  private Socket socket;
  private String host;
  private int port;
  private String cache;
  
  private Map<String, String> women;
  private Map<String, String> men;

  public AttSpeechSynthesizer() {
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
  
  private void loadVoices() throws RuntimeException {
    women = new HashMap<String, String>();
    men = new HashMap<String, String>();
    // Initialize female voices.
    women.put("en", configuration.getString("speakers.english.female"));
    // Initialize male voices.
    men.put("en", configuration.getString("speakers.english.male"));
  }

  @Override public void start() throws RuntimeException {
    loadVoices();
    host = configuration.getString("host");
    port = configuration.getInt("port");
  }

  @Override public void shutdown() {
    
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

  @Override public URI synthesize(final String text, final String gender, final String language)
      throws SpeechSynthesizerException {
	try {
	  try { socket = new Socket(host, port); }
      catch(final IOException exception){
        throw new RuntimeException(exception);
      }
	  StringBuilder buffer = new StringBuilder();
	  buffer.append("0").append(":").append(text).append(":").append(cache);
      if(!cache.endsWith("/")) { buffer.append("/"); }
      buffer.append(buildKey(text, gender, language)).append(".wav");
      final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      out.write(buffer.toString());
      out.flush();
      final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      final String result = in.readLine();
      if("OK".equals(result)) {
    	buffer = new StringBuilder();
    	buffer.append("file://").append(cache);
        if(!cache.endsWith("/")) { buffer.append("/"); }
        buffer.append(buildKey(text, gender, language)).append(".wav");
        return URI.create(buffer.toString());
      } else {
        throw new SpeechSynthesizerException("Failed to synthesize text to speech.");
      }
	} catch(final IOException exception) {
	  throw new SpeechSynthesizerException(exception);
	}
  }
}
