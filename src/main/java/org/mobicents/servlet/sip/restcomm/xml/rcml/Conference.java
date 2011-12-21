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

import java.util.HashSet;
import java.util.Set;

import static org.mobicents.servlet.sip.restcomm.http.RequestMethod.*;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.UnsupportedAttributeException;
import org.mobicents.servlet.sip.restcomm.xml.UnsupportedTagException;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;

public final class Conference extends RCMLTag {
  public static final String NAME = "Conference";
  private static final Set<String> ATTRIBUTES;
  static {
    ATTRIBUTES = new HashSet<String>();
    ATTRIBUTES.add(Muted.NAME);
    ATTRIBUTES.add(Beep.NAME);
    ATTRIBUTES.add(StartConferenceOnEnter.NAME);
    ATTRIBUTES.add(EndConferenceOnExit.NAME);
    ATTRIBUTES.add(WaitUrl.NAME);
    ATTRIBUTES.add(WaitMethod.NAME);
    ATTRIBUTES.add(MaxParticipants.NAME);
  }
  
  public Conference() {
    super();
    final Muted muted = new Muted();
    muted.setBooleanValue(false);
    final Beep beep = new Beep();
    beep.setBooleanValue(true);
    final StartConferenceOnEnter startConferenceOnEnter = new StartConferenceOnEnter();
    startConferenceOnEnter.setBooleanValue(true);
    final EndConferenceOnExit endConferenceOnExit = new EndConferenceOnExit();
    endConferenceOnExit.setBooleanValue(false);
    final WaitMethod waitMethod = new WaitMethod();
    waitMethod.setValue(POST);
    final MaxParticipants maxParticipants = new MaxParticipants();
    maxParticipants.setIntegerValue(40);
    try {
      addAttribute(muted);
      addAttribute(beep);
      addAttribute(startConferenceOnEnter);
      addAttribute(endConferenceOnExit);
      addAttribute(waitMethod);
      addAttribute(maxParticipants);
    } catch(final UnsupportedAttributeException ignored) {
      // Will never happen.
    }
  }
  
  @Override public void accept(final TagVisitor visitor) throws VisitorException {
    visitor.visit(this);
  }
  
  @Override public void addChild(final Tag child) throws UnsupportedTagException {
	throw new UnsupportedOperationException("The <" + NAME + "> tag may not have any children.");
  }

  @Override public boolean canContainAttribute(final String name) {
    return ATTRIBUTES.contains(name);
  }

  @Override public boolean canContainChild(final Tag tag) {
    return false;
  }

  @Override public String getName() {
    return NAME;
  }
  
  @Override public boolean isNoun() {
    return true;
  }
  
  @Override public boolean isVerb() {
    return false;
  }
}
