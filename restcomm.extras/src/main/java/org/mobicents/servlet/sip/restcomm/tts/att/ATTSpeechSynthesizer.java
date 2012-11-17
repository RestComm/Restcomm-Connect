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
package org.mobicents.servlet.sip.restcomm.tts.att;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.tts.AbstractSpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizerException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ATTSpeechSynthesizer extends AbstractSpeechSynthesizer {
  private static final Logger logger = Logger.getLogger(ATTSpeechSynthesizer.class);

  private static final String extension = "wav";
  
  private String host;
  private int port;
  private String directory;
  
  private Map<String, String> women;
  private Map<String, String> men;
  
  public ATTSpeechSynthesizer() {
    super();
  }
  
  private String buildPath(final String name, final String extension) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(directory).append(name).append(".").append(extension);
    return buffer.toString();
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
    women.put("en", configuration.getString("speakers.english.female"));
    // Initialize male voices.
    men.put("en", configuration.getString("speakers.english.male"));
  }

  @Override public void start() throws RuntimeException {
    host = configuration.getString("host");
    port = configuration.getInt("port");
    directory = configuration.getString("directory");
    if(!directory.endsWith("/")) { directory = directory + "/"; }
    loadVoices();
  }

  @Override public URI synthesize(final String text, final String gender,
      final String language) throws SpeechSynthesizerException {
	// Create a request.
    final String path = buildPath(buildKey(text, gender, language), extension);
	final Map<String, Object> request = new HashMap<String, Object>();
	request.put("code", 0);
	request.put("path", path);
	request.put("ssml", 1);
	request.put("text", text);
	request.put("voice", findSpeaker(gender, language));
	Socket socket = null;
	BufferedReader in = null;
	PrintWriter out = null;
	try {
	  // Create a socket to the TTS bridge.
	  socket = new Socket(host, port);
	  in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	  out = new PrintWriter(socket.getOutputStream(), true);
	  final Gson gson = new Gson();
	  out.write(gson.toJson(request));
	  out.write("\r\n");
	  out.flush();
	  // Read the response from the socket.
	  final String data = in.readLine();
	  @SuppressWarnings("unchecked")
	  final Map<String, Object> result = gson.fromJson(data, Map.class);
	  final int code = ((Number)result.get("code")).intValue();
	  if(code != 0) {
	    throw new SpeechSynthesizerException("Could not synthesize text to speech. Please see the TTS bridge log.");
	  }
	  return URI.create("file://" + path);
	} catch(final IOException exception) {
	  logger.error(exception);
      throw new SpeechSynthesizerException(exception);
    } finally {
      // Clean up.
      try {
        in.close();
  	    out.close();
  	    socket.close();
      } catch(final IOException exception) {
        logger.error(exception);
        throw new SpeechSynthesizerException(exception);
      }
    }
  }
}
