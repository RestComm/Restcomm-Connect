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
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.UnsupportedAttributeException;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;
import org.mobicents.servlet.sip.restcomm.xml.RcmlDocument;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class Response extends RcmlTag implements RcmlDocument {
  public static final String NAME = "Response";
  private static final Set<String> CHILDREN;
  static {
    CHILDREN = new HashSet<String>();
    CHILDREN.add(Say.NAME);
    CHILDREN.add(Play.NAME);
    CHILDREN.add(Gather.NAME);
    CHILDREN.add(Record.NAME);
    CHILDREN.add(Sms.NAME);
    CHILDREN.add(Dial.NAME);
    CHILDREN.add(Hangup.NAME);
    CHILDREN.add(Redirect.NAME);
    CHILDREN.add(Reject.NAME);
    CHILDREN.add(Pause.NAME);
  }
  
  public Response() {
    super();
  }
  
  @Override public void accept(final TagVisitor visitor) throws VisitorException {
	final List<Tag> children = getChildren();
	if(children.size() > 0) {
	  for(final Tag child : children) {
	    visitor.visit(child);
	  }
	}
  }
  
  @Override public void addAttribute(final Attribute attribute) throws UnsupportedAttributeException {
    throw new UnsupportedOperationException("The <" + NAME + "> tag may not have any attributes.");
  }
  
  @Override public boolean canBeRoot() {
    return true;
  }
  
  @Override public boolean canContainAttribute(final String name) {
    return false;
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
    return false;
  }
  
  @Override public void setText(final String text) {
    throw new UnsupportedOperationException("The <" + NAME + "> tag may not have any text.");
  }
}
