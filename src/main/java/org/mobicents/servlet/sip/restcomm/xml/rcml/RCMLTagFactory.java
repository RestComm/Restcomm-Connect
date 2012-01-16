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
package org.mobicents.servlet.sip.restcomm.xml.rcml;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.servlet.sip.restcomm.ObjectInstantiationException;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagFactory;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RCMLTagFactory implements TagFactory {
  private static final Map<String, Class<? extends Attribute>> ATTRIBUTES;
  private static final Map<String, Class<? extends Tag>> TAGS;
  static {
    ATTRIBUTES = new HashMap<String, Class<? extends Attribute>>();
    ATTRIBUTES.put(Action.NAME, Action.class);
    ATTRIBUTES.put(Beep.NAME, Beep.class);
    ATTRIBUTES.put(CallerId.NAME, CallerId.class);
    ATTRIBUTES.put(EndConferenceOnExit.NAME, EndConferenceOnExit.class);
    ATTRIBUTES.put(FinishOnKey.NAME, FinishOnKey.class);
    ATTRIBUTES.put(From.NAME, From.class);
    ATTRIBUTES.put(HangupOnStar.NAME, HangupOnStar.class);
    ATTRIBUTES.put(Language.NAME, Language.class);
    ATTRIBUTES.put(Length.NAME, Length.class);
    ATTRIBUTES.put(Loop.NAME, Loop.class);
    ATTRIBUTES.put(MaxLength.NAME, MaxLength.class);
    ATTRIBUTES.put(MaxParticipants.NAME, MaxParticipants.class);
    ATTRIBUTES.put(Method.NAME, Method.class);
    ATTRIBUTES.put(Muted.NAME, Muted.class);
    ATTRIBUTES.put(NumDigits.NAME, NumDigits.class);
    ATTRIBUTES.put(PlayBeep.NAME, PlayBeep.class);
    ATTRIBUTES.put(Reason.NAME, Reason.class);
    ATTRIBUTES.put(SendDigits.NAME, SendDigits.class);
    ATTRIBUTES.put(StartConferenceOnEnter.NAME, StartConferenceOnEnter.class);
    ATTRIBUTES.put(StatusCallback.NAME, StatusCallback.class);
    ATTRIBUTES.put(TimeLimit.NAME, TimeLimit.class);
    ATTRIBUTES.put(Timeout.NAME, Timeout.class);
    ATTRIBUTES.put(To.NAME, To.class);
    ATTRIBUTES.put(Transcribe.NAME, Transcribe.class);
    ATTRIBUTES.put(TranscribeCallback.NAME, TranscribeCallback.class);
    ATTRIBUTES.put(Url.NAME, Url.class);
    ATTRIBUTES.put(Voice.NAME, Voice.class);
    ATTRIBUTES.put(WaitMethod.NAME, WaitMethod.class);
    ATTRIBUTES.put(WaitUrl.NAME, WaitUrl.class);
    
    TAGS = new HashMap<String, Class<? extends Tag>>();
    TAGS.put(Response.NAME, Response.class);
    TAGS.put(Say.NAME, Say.class);
    TAGS.put(Play.NAME, Play.class);
    TAGS.put(Gather.NAME, Gather.class);
    TAGS.put(Record.NAME, Record.class);
    TAGS.put(Sms.NAME, Sms.class);
    TAGS.put(Dial.NAME, Dial.class);
    TAGS.put(Client.NAME, Client.class);
    TAGS.put(Number.NAME, Number.class);
    TAGS.put(Conference.NAME, Conference.class);
    TAGS.put(Hangup.NAME, Hangup.class);
    TAGS.put(Redirect.NAME, Redirect.class);
    TAGS.put(Reject.NAME, Reject.class);
    TAGS.put(Pause.NAME, Pause.class);
  }
  
  public RCMLTagFactory() {
    super();
  }

  public Attribute getAttributeInstance(final String name) throws ObjectInstantiationException {
	if(name == null) {
	  throw new NullPointerException("Can not instantiate an attribute named null");
	} else if(!ATTRIBUTES.containsKey(name)) {
	  throw new ObjectInstantiationException("The attribute named " + name +
	      " can not be instantiated because it is not a supported RestComm attribute.");
	} else {
	  try {
	    return ATTRIBUTES.get(name).newInstance();
	  } catch(final InstantiationException exception) {
		throw new ObjectInstantiationException(exception);
	  } catch(final IllegalAccessException exception) {
		throw new ObjectInstantiationException(exception);
	  }
	}
  }

  public Tag getTagInstance(final String name) throws ObjectInstantiationException {
	if(name == null) {
      throw new NullPointerException("Can not instantiate a tag with a null name.");
	} else if(!TAGS.containsKey(name)) {
      throw new ObjectInstantiationException("The <" + name + "> tag can not be instantiated because it is not a supported RestComm tag.");
	} else {
	  try {
		return TAGS.get(name).newInstance();
	  } catch(final InstantiationException exception) {
		throw new ObjectInstantiationException(exception);
	  } catch(final IllegalAccessException exception) {
		throw new ObjectInstantiationException(exception);
	  }
	}
  }
}
