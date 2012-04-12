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
package org.mobicents.servlet.sip.restcomm.asr;

import com.iSpeech.SpeechResult;
import com.iSpeech.iSpeechRecognizer;
import com.iSpeech.iSpeechRecognizer.SpeechRecognizerEvent;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ISpeechRecognizer implements Runnable, SpeechRecognizer, SpeechRecognizerEvent {
  private static final Logger logger = Logger.getLogger(ISpeechRecognizer.class);
  private static final Map<String, String> languages = new HashMap<String, String>();
  static {
    languages.put("en", "en-US");
    languages.put("en-gb", "en-GB");
    languages.put("es", "es-ES");
    languages.put("it", "it-IT");
    languages.put("fr", "fr-FR");
    languages.put("pl", "pl-PL");
    languages.put("pt", "pt-PT");
  }
  
  private Configuration configuration;
  private String apiKey;
  private boolean production;
  
  private Thread worker;
  private volatile boolean running;
  private BlockingQueue<SpeechRecognitionRequest> queue;

  public ISpeechRecognizer() {
    super();
  }

  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  @Override public boolean isSupported(final String language) {
  	return languages.containsKey(language);
  }
  
  @Override public void recognize(final URI audioFile, final String language,
      final SpeechRecognizerObserver observer) {
    recognize(new File(audioFile), language, observer);
  }
  
  @Override public void recognize(final File audioFile, final String language,
      final SpeechRecognizerObserver observer) {
    try { queue.put(new SpeechRecognitionRequest(audioFile, languages.get(language), observer)); }
    catch(final InterruptedException ignored) { }
  }
  
  @Override public void run() {
    while(running) {
      SpeechRecognitionRequest request = null;
      try {
	    request = queue.take();
	    final iSpeechRecognizer recognizer = iSpeechRecognizer.getInstance(apiKey, production);
	    recognizer.setFreeForm(iSpeechRecognizer.FREEFORM_DICTATION);
	    recognizer.setLanguage(request.getLanguage());
	    final SpeechResult results = recognizer.startFileRecognize("audio/x-wav", request.getFile(), this);
	    request.getObserver().succeeded(results.Text);
	  } catch(final InterruptedException ignored) {
	    // Nothing to do.
	  } catch(final Exception exception) {
        logger.error(exception);
        request.getObserver().failed();
	  }
    }
  }
  
  @Override public void start() throws RuntimeException {
    apiKey = configuration.getString("api-key");
    production = configuration.getBoolean("api-key[@production]");
    queue = new LinkedBlockingQueue<SpeechRecognitionRequest>();
    worker = new Thread(this);
    worker.setName("iSpeech Recognizer Worker");
    worker.start();
  }

  @Override public void shutdown() {
    if(running) {
      running = false;
    }
  }

  @Override public void stateChanged(final int event, final int freeFormValue, final Exception lastException) {
    if(SpeechRecognizerEvent.RECORDING_ERROR == event) {
      logger.error(lastException);
    }
  }
  
  @Immutable private final class SpeechRecognitionRequest {
    private final File file;
    private final String language;
    private final SpeechRecognizerObserver observer;
    
    private SpeechRecognitionRequest(final File file, final String language, final SpeechRecognizerObserver observer) {
      super();
      this.file = file;
      this.language = language;
      this.observer = observer;
    }

	private File getFile() {
	  return file;
	}

	private String getLanguage() {
	  return language;
	}

	private SpeechRecognizerObserver getObserver() {
	  return observer;
	}
  }
}
