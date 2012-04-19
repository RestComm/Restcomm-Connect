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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.Recording;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.Transcription;
import org.mobicents.servlet.sip.restcomm.Transcription.Status;
import org.mobicents.servlet.sip.restcomm.asr.SpeechRecognizer;
import org.mobicents.servlet.sip.restcomm.asr.SpeechRecognizerObserver;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.dao.RecordingsDao;
import org.mobicents.servlet.sip.restcomm.dao.TranscriptionsDao;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.util.StringUtils;
import org.mobicents.servlet.sip.restcomm.util.WavUtils;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.MaxLength;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.PlayBeep;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.Transcribe;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.TranscribeCallback;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.TranscribeLanguage;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RecordTagStrategy extends RcmlTagStrategy implements SpeechRecognizerObserver {
  private static final List<URI> emptyAnnouncement = new ArrayList<URI>();
  private static final Logger logger = Logger.getLogger(RecordTagStrategy.class);
  private static final Pattern finishOnKeyPattern = Pattern.compile("[\\*#0-9]{1,12}");
  
  private final String baseRecordingsPath;
  private final List<URI> beepAudioFile;
  private final SpeechRecognizer speechRecognizer;
  
  private URI action;
  private String method;
  private int timeout;
  private String finishOnKey;
  private int maxLength;
  private boolean transcribe;
  private URI transcribeCallback;
  private String transcribeLanguage;
  private boolean playBeep;
  
  private Recording recording;
  private RcmlInterpreterContext context;
  
  public RecordTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    speechRecognizer = services.get(SpeechRecognizer.class);
    final Configuration configuration = services.get(Configuration.class);
    final String path = configuration.getString("recordings-path");
    baseRecordingsPath = StringUtils.addSuffixIfNotPresent(path, "/");
    beepAudioFile = new ArrayList<URI>();
    final URI uri = URI.create("file://" + configuration.getString("beep-audio-file"));
    beepAudioFile.add(uri);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    this.context = context;
    final Call call = context.getCall();
    try {
      if(playBeep) {
        call.play(beepAudioFile, 1);
      }
      // Record something.
      final Sid sid = Sid.generate(Sid.Type.RECORDING);
      final URI path = toPath(sid);
      call.playAndRecord(emptyAnnouncement, path, timeout, maxLength, finishOnKey);
      final double duration = WavUtils.getAudioDuration(path);
      if(duration > 0) {
        recording = recording(sid, duration);
        final RecordingsDao dao = daos.getRecordingsDao();
        dao.addRecording(recording);
        // Transcribe the path.
        if(transcribe || (transcribeCallback != null)) {
          speechRecognizer.recognize(path, transcribeLanguage, this);
        }
        // Redirect to action URI.
        final List<NameValuePair> variables = new ArrayList<NameValuePair>();
        variables.add(new BasicNameValuePair("RecordingUrl", recording.getUri().toString()));
        variables.add(new BasicNameValuePair("RecordingDuration", recording.getDuration().toString()));
        variables.add(new BasicNameValuePair("Digits", call.getDigits()));
        interpreter.load(action, method, variables);
        interpreter.redirect();
      }
    } catch(final Exception exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 12400);
      throw new TagStrategyException(exception);
    }
  }
  
  @Override public void failed() {
	handleTranscription(false, null);
  }
  
  private int getMaxLength(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(MaxLength.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if(StringUtils.isPositiveInteger(value)) {
    	final int result = Integer.parseInt(value);
        if(result > 1) {
          return result;
        }
      }
    } else {
      return 3600;
    }
    interpreter.notify(context, Notification.WARNING, 13612);
    return 3600;
  }
  
  private boolean getPlayBeep(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
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
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Transcribe.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
        return Boolean.parseBoolean(value);
      }
    }
    interpreter.notify(context, Notification.ERROR, 21503);
    return false;
  }
  
  private URI getTranscribeCallback(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Attribute attribute = tag.getAttribute(TranscribeCallback.NAME);
    if(attribute != null) {
      try {
        return URI.create(attribute.getValue());
      } catch(final IllegalArgumentException exception) {
        interpreter.notify(context, Notification.ERROR, 11100);
      }
    }
    return null;
  }
  
  private String getTranscribeLanguage(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(TranscribeLanguage.NAME);
    if(attribute != null) {
      final String language = attribute.getValue();
      if(speechRecognizer.isSupported(language)) {
        return language;
      }
    }
    return "en";
  }
  
  private void handleTranscription(final boolean success, final String text) {
    final Transcription.Builder builder = Transcription.builder();
    final Sid sid = Sid.generate(Sid.Type.TRANSCRIPTION);
    builder.setAccountSid(context.getAccountSid());
    builder.setSid(sid);
    if(success) {
      builder.setStatus(Status.COMPLETED);
      builder.setTranscriptionText(text);
    } else {
      builder.setStatus(Status.FAILED);
    }
    builder.setRecordingSid(recording.getSid());
    builder.setDuration(recording.getDuration());
    builder.setPrice(new BigDecimal(0.00));
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(context.getApiVersion()).append("/Accounts/");
    buffer.append(context.getAccountSid().toString()).append("/Transcriptions/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    final Transcription transcription = builder.build();
    final TranscriptionsDao dao = daos.getTranscriptionsDao();
    dao.addTranscription(transcription);
    if(transcribeCallback != null) {
      final List<NameValuePair> variables = new ArrayList<NameValuePair>();
      variables.add(new BasicNameValuePair("TranscriptionText", transcription.getTranscriptionText()));
      variables.add(new BasicNameValuePair("TranscriptionStatus", transcription.getStatus().toString()));
      variables.add(new BasicNameValuePair("TranscriptionUrl", transcription.getUri().toString()));
      variables.add(new BasicNameValuePair("RecordingUrl", recording.getUri().toString()));
      transcribeCallback(transcribeCallback, variables);
    }
  }
  
  private void initAction(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    try {
      action = getAction(interpreter, context, tag);
      if(action == null) {
        action = interpreter.getCurrentResourceUri();
      }
    } catch(final IllegalArgumentException exception) {
      interpreter.failed();
      interpreter.notify(context, Notification.ERROR, 11100);
      throw new TagStrategyException(exception);
    }
  }
  
  private void initMethod(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    method = getMethod(interpreter, context, tag);
    if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
      interpreter.notify(context, Notification.WARNING, 13610);
      method = "POST";
    }
  }
  
  private void initTimeout(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Object object = getTimeout(interpreter, context, tag);
    if(object == null) {
      timeout = 5;
    } else {
      timeout = (Integer)object;
      if(timeout == -1) {
        interpreter.notify(context, Notification.WARNING, 13611);
        timeout = 5;
      }
    }
  }
  
  private void initFinishOnKey(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    finishOnKey = getFinishOnKey(interpreter, context, tag);
    if(finishOnKey == null) {
      finishOnKey = "1234567890*#";
    } else {
      if(!finishOnKeyPattern.matcher(finishOnKey).matches()) {
    	interpreter.notify(context, Notification.WARNING, 13613);
    	finishOnKey = "1234567890*#";
      }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    initAction(interpreter, context, tag);
    initMethod(interpreter, context, tag);
    initTimeout(interpreter, context, tag);
    initFinishOnKey(interpreter, context, tag);
    maxLength = getMaxLength(interpreter, context, tag);
    transcribe = getTranscribe(interpreter, context, tag);
    transcribeCallback = getTranscribeCallback(interpreter, context, tag);
    transcribeLanguage = getTranscribeLanguage(interpreter, context, tag);
    playBeep = getPlayBeep(interpreter, context, tag);
  }
  
  protected Recording recording(final Sid sid, final double duration) {
    final Recording.Builder builder = Recording.builder();
    builder.setSid(sid);
    builder.setAccountSid(context.getAccountSid());
    builder.setCallSid(context.getCall().getSid());
    builder.setDuration(duration);
    builder.setApiVersion(context.getApiVersion());
    final StringBuilder buffer = new StringBuilder();
    buffer.append(rootUri).append(context.getApiVersion()).append("/Accounts/");
    buffer.append(context.getAccountSid().toString()).append("/Recordings/");
    buffer.append(sid.toString());
    final URI uri = URI.create(buffer.toString());
    builder.setUri(uri);
    return builder.build();
  }
  
  @Override public void succeeded(final String text) {
    handleTranscription(true, text);
  }
  
  private URI toPath(final Sid sid) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("file://").append(baseRecordingsPath).append(sid.toString()).append(".wav");
    return URI.create(buffer.toString());
  }
  
  private void transcribeCallback(final URI uri, List<NameValuePair> additionalVariables) {
    final List<NameValuePair> variables = context.getRcmlRequestParameters();
    variables.addAll(additionalVariables);
    final HttpPost post = new HttpPost(uri);
    try { post.setEntity(new UrlEncodedFormEntity(variables)); }
    catch(final UnsupportedEncodingException ignored) { }
    final HttpClient client = new DefaultHttpClient();
	try {
	  client.execute(post);
	} catch(final Exception exception) {
	  logger.error(exception);
	}
  }
}
