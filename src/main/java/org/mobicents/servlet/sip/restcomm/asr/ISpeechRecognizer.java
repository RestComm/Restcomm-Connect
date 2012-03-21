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
import java.io.Serializable;
import java.net.URI;
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
  
  @Override public void recognize(final URI audioFile, final String locale,
      final SpeechRecognizerObserver observer, final Serializable object) {
    recognize(new File(audioFile), locale, observer, object);
  }
  
  @Override public void recognize(final File audioFile, final String locale,
      final SpeechRecognizerObserver observer, final Serializable object) {
    try { queue.put(new SpeechRecognitionRequest(audioFile, locale, observer, object)); }
    catch(final InterruptedException ignored) { }
  }
  
  @Override public void run() {
    while(running) {
      SpeechRecognitionRequest request = null;
      try {
	    request = queue.take();
	    final iSpeechRecognizer recognizer = iSpeechRecognizer.getInstance(apiKey, production);
	    recognizer.setFreeForm(iSpeechRecognizer.FREEFORM_DICTATION);
	    recognizer.setLanguage(request.getLocale());
	    final SpeechResult results = recognizer.startFileRecognize("audio/x-wav", request.getFile(), this);
	    request.getObserver().succeeded(results.Text, request.getObject());
	  } catch(final InterruptedException ignored) {
	    // Nothing to do.
	  } catch(final Exception exception) {
        logger.error(exception);
        request.getObserver().failed(request.getObject());
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
    private final String locale;
    private final SpeechRecognizerObserver observer;
    private final Serializable object;
    
    private SpeechRecognitionRequest(final File file, final String locale, final SpeechRecognizerObserver observer,
        final Serializable object) {
      super();
      this.file = file;
      this.locale = locale;
      this.observer = observer;
      this.object = object;
    }

	private File getFile() {
	  return file;
	}

	private String getLocale() {
	  return locale;
	}

	private SpeechRecognizerObserver getObserver() {
	  return observer;
	}

	private Serializable getObject() {
	  return object;
	}
  }
}
