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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
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

public final class RecordTagStrategy extends RcmlTagStrategy {
  private static final List<URI> emptyAnnouncement = new ArrayList<URI>();
  private final String basePath;
  private final String baseUri;
  
  public RecordTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    basePath = addSuffix(configuration.getString("recordings-basePath"), "/");
    baseUri = addSuffix(configuration.getString("recordings-uri"), "/");
  }
  
  private String addSuffix(final String text, final String suffix) {
    if(text.endsWith(suffix)) {
      return text;
    } else {
      return text + suffix;
    }
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    answer(call);
    try {
      final boolean playBeep = ((BooleanAttribute)tag.getAttribute(PlayBeep.NAME)).getBooleanValue();
      if(playBeep) {
        call.play(playBeep(), 1);
      }
      // Record something.
      final Sid sid = Sid.generate(Sid.Type.RECORDING);
      final int timeout = ((IntegerAttribute)tag.getAttribute(Timeout.NAME)).getIntegerValue();
      final String finishOnKey = tag.getAttribute(FinishOnKey.NAME).getValue();
      final int maxLength = ((IntegerAttribute)tag.getAttribute(MaxLength.NAME)).getIntegerValue();
      call.playAndRecord(emptyAnnouncement, toPath(sid), timeout, maxLength, finishOnKey);
      // Redirect to action URI.
      final URI action = ((UriAttribute)tag.getAttribute(Action.NAME)).getUriValue();
      final String method = tag.getAttribute(Method.NAME).getValue();
      final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
      parameters.add(new BasicNameValuePair("RecordingUrl", toUri(sid)));
      parameters.add(new BasicNameValuePair("RecordingDuration", "-1"));
      parameters.add(new BasicNameValuePair("Digits", call.getDigits()));
      interpreter.loadResource(action, method, parameters);
      interpreter.redirect();
    } catch(final Exception exception) {
      interpreter.failed();
      throw new TagStrategyException("There was an error while recording audio.", exception);
    }
  }
  
  private List<URI> playBeep() {
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    final URI uri = URI.create(configuration.getString("beep-audio-file"));
    final List<URI> announcement = new ArrayList<URI>();
    announcement.add(uri);
    return announcement;
  }
  
  private URI toPath(final Sid sid) {
    final StringBuilder path = new StringBuilder();
    path.append(basePath).append(sid.toString()).append(".wav");
    return URI.create(path.toString());
  }
  
  private String toUri(final Sid sid) {
    final StringBuilder uri = new StringBuilder();
    uri.append(baseUri).append(sid.toString()).append(".wav");
    return uri.toString();
  }
}
