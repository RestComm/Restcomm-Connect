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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.asr.SpeechRecognizer;
import org.mobicents.servlet.sip.restcomm.asr.SpeechRecognizerObserver;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.util.WavUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.BooleanAttribute;
import org.mobicents.servlet.sip.restcomm.xml.IntegerAttribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.UriAttribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Action;
import org.mobicents.servlet.sip.restcomm.xml.rcml.FinishOnKey;
import org.mobicents.servlet.sip.restcomm.xml.rcml.MaxLength;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.PlayBeep;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Timeout;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Transcribe;
import org.mobicents.servlet.sip.restcomm.xml.rcml.TranscribeCallback;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RecordTagStrategy extends RcmlTagStrategy implements SpeechRecognizerObserver {
  private static final List<URI> emptyAnnouncement = new ArrayList<URI>();
  
  private final String baseRecordingsPath;
  private final String baseRecordingsUri;
  private final List<URI> beepAudioFile;
  private final SpeechRecognizer speechRecognizer;
  
  private URI action;
  private String method;
  private int timeout;
  private String finishOnKey;
  private int maxLength;
  private boolean transcribe;
  private URI transcribeCallback;
  private boolean playBeep;
  
  public RecordTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    baseRecordingsPath = StringUtils.addSuffixIfNotPresent(configuration.getString("recordings-path"), "/");
    baseRecordingsUri = StringUtils.addSuffixIfNotPresent(configuration.getString("recordings-uri"), "/");
    beepAudioFile = new ArrayList<URI>();
    beepAudioFile.add(URI.create("file://" + configuration.getString("beep-audio-file")));
    speechRecognizer = services.get(SpeechRecognizer.class);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    try {
      if(playBeep) {
        call.play(beepAudioFile, 1);
      }
      // Record something.
      final Sid sid = Sid.generate(Sid.Type.RECORDING);
      final URI recording = toPath(sid);
      call.playAndRecord(emptyAnnouncement, recording, timeout, maxLength, finishOnKey);
      // Transcribe the recording.
      if(transcribe || (transcribeCallback != null)) {
        final Map<String, Object> information = new HashMap<String, Object>();
        information.put("interpreterContext", context);
        information.put("recordingUri", toUri(sid));
        information.put("transcribeCallback", transcribeCallback);
        speechRecognizer.recognize(recording, "en-US", this, (Serializable)information);
      }
      // Redirect to action URI.
      final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
      parameters.add(new BasicNameValuePair("RecordingUrl", toUri(sid)));
      final String duration = Double.toString(WavUtils.getAudioDuration(recording));
      parameters.add(new BasicNameValuePair("RecordingDuration", duration));
      parameters.add(new BasicNameValuePair("Digits", call.getDigits()));
      interpreter.loadResource(action, method, parameters);
      interpreter.redirect();
    } catch(final Exception exception) {
      interpreter.failed();
      throw new TagStrategyException("There was an error while recording audio.", exception);
    }
  }
  
  @Override public void failed(final Serializable object) {
	handleTranscription(false, null, object);
  }
  
  private int getMaxLength(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) {
    final Attribute attribute = tag.getAttribute(MaxLength.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
    	final int result = Integer.parseInt(value);
        if(result >= 1) {
          return result;
        }
      }
    }
    notify(interpreter, context, tag, Notification.WARNING, 13612);
    return 3600;
  }
  
  private boolean getPlayBeep(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) {
    final Attribute attribute = tag.getAttribute(PlayBeep.NAME);
    if(attribute != null) {
	  final String value = attribute.getValue();
      if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
        return Boolean.parseBoolean(value);
      }
    }
    return true;
  }
  
  private boolean getTranscribe(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) {
    final Attribute attribute = tag.getAttribute(Transcribe.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
        return Boolean.parseBoolean(value);
      }
    }
    notify(interpreter, context, tag, Notification.ERROR, 21503);
    return false;
  }
  
  private URI getTranscribeCallback(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(TranscribeCallback.NAME);
    if(attribute != null) {
      try {
        return URI.create(attribute.getValue());
      } catch(final IllegalArgumentException exception) {
        notify(interpreter, context, tag, Notification.ERROR, 11100);
      }
    }
    return null;
  }
  
  private void handleTranscription(final boolean success, final String text, final Serializable object) {
    @SuppressWarnings("unchecked")
	final Map<String, Object> information = (HashMap<String, Object>)object;
    final RcmlInterpreterContext context = (RcmlInterpreterContext)information.get("interpreterContext");
    final String recordingUri = (String)information.get("recordingUri");
    final URI transcribeCallback = (URI)information.get("transcribeCallback");
    
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final Tag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    action = getAction(interpreter, context, tag);
    method = getMethod(interpreter, context, tag);
    if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      notify(interpreter, context, tag, Notification.WARNING, 13610);
      method = "POST";
    }
    timeout = getTimeout(interpreter, context, tag);
    if(timeout == -1 || timeout == 0) {
      notify(interpreter, context, tag, Notification.WARNING, 13611);
      timeout = 5;
    }
    finishOnKey = getFinishOnKey(interpreter, context, tag);
    if(finishOnKey == null) {
      notify(interpreter, context, tag, Notification.WARNING, 13613);
      finishOnKey = "1234567890*#";
    }
    maxLength = getMaxLength(interpreter, context, tag);
    transcribe = getTranscribe(interpreter, context, tag);
    transcribeCallback = getTranscribeCallback(interpreter, context, tag);
    playBeep = getPlayBeep(interpreter, context, tag);
  }
  
  @Override public void succeeded(final String text, final Serializable object) {
    handleTranscription(true, text, object);
  }
  
  private URI toPath(final Sid sid) {
    final StringBuilder path = new StringBuilder();
    path.append("file://").append(baseRecordingsPath).append(sid.toString()).append(".wav");
    return URI.create(path.toString());
  }
  
  private String toUri(final Sid sid) {
    final StringBuilder uri = new StringBuilder();
    uri.append(baseRecordingsUri).append(sid.toString()).append(".wav");
    return uri.toString();
  }
}
