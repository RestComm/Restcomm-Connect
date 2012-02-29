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

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.UnsupportedAttributeException;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class Dial extends RcmlTag {
  public static final String NAME = "Dial";
  private static final Set<String> ATTRIBUTES;
  private static final Set<String> CHILDREN;
  static {
    ATTRIBUTES = new HashSet<String>();
    ATTRIBUTES.add(Action.NAME);
    ATTRIBUTES.add(Method.NAME);
    ATTRIBUTES.add(Timeout.NAME);
    ATTRIBUTES.add(HangupOnStar.NAME);
    ATTRIBUTES.add(TimeLimit.NAME);
    ATTRIBUTES.add(CallerId.NAME);
    
    CHILDREN = new HashSet<String>();
    CHILDREN.add(Number.NAME);
    CHILDREN.add(Client.NAME);
    CHILDREN.add(Conference.NAME);
  }
  
  public Dial() {
    super();
    final Method method = new Method();
    method.setValue(POST);
    final Timeout timeout = new Timeout();
    timeout.setIntegerValue(30);
    final HangupOnStar hangupOnStar = new HangupOnStar();
    hangupOnStar.setBooleanValue(false);
    final TimeLimit timeLimit = new TimeLimit();
    timeLimit.setIntegerValue(14400);
    try {
      addAttribute(method);
      addAttribute(timeout);
      addAttribute(hangupOnStar);
      addAttribute(timeLimit);
    } catch(final UnsupportedAttributeException ignored) {
      // Will never happen.
    }
  }
  
  @Override public void accept(final TagVisitor visitor) throws VisitorException {
    visitor.visit(this);
  }

  @Override public boolean canContainAttribute(final String name) {
    return ATTRIBUTES.contains(name);
  }

  @Override public boolean canContainChild(final Tag tag) {
    return CHILDREN.contains(tag.getName());
  }

  @Override public String getName() {
    return NAME;
  }
  
  @Override public boolean isNoun() {
    return false;
  }
  
  @Override public boolean isVerb() {
    return true;
  }
}
