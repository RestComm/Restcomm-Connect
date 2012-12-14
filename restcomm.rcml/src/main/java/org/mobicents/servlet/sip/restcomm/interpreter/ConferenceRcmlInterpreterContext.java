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
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.media.api.Conference;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ConferenceRcmlInterpreterContext extends RcmlInterpreterContext {
  private final Conference conference;
  private final URI waitUrl;
  private final String waitMethod;

  public ConferenceRcmlInterpreterContext(final Sid accountSid, final String apiVersion, final URI waitUrl,
      final String waitMethod, final Conference conference) {
    super(accountSid, apiVersion, null, null);
    this.conference = conference;
    this.waitUrl = waitUrl;
    this.waitMethod = waitMethod;
  }

  public Conference getConference() {
    return conference;
  }

  @Override public String getFrom() {
    return null;
  }

  @Override public List<NameValuePair> getRcmlRequestParameters() {
	final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
	parameters.add(new BasicNameValuePair("AccountSid", accountSid.toString()));
	parameters.add(new BasicNameValuePair("ApiVersion", apiVersion));
	parameters.add(new BasicNameValuePair("FriendlyName", getConferenceName(conference.getName())));
    return parameters;
  }

  @Override public String getTo() {
    return null;
  }
  
  public String getWaitMethod() {
    return waitMethod;
  }
  
  public URI getWaitUrl() {
    return waitUrl;
  }
  
  private String getConferenceName(final String text) {
    final String[] tokens = text.split(":");
    if(tokens.length == 2) {
      return tokens[1];
    } else if(tokens.length == 1) {
      return tokens[0];
    } else {
      return null;
    }
  }
}
