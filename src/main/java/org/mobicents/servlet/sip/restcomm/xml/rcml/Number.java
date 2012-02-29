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
import java.util.regex.Pattern;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.TagVisitor;
import org.mobicents.servlet.sip.restcomm.xml.UnsupportedTagException;
import org.mobicents.servlet.sip.restcomm.xml.VisitorException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class Number extends RcmlTag {
  public static final String NAME = "Number";
  private static final Set<String> ATTRIBUTES;
  static {
    ATTRIBUTES = new HashSet<String>();
    ATTRIBUTES.add(SendDigits.NAME);
    ATTRIBUTES.add(Url.NAME);
  }
  private static final Pattern E164 = Pattern.compile("\\+?\\d{10,15}");
  private static final Pattern US_1 = Pattern.compile("\\d{3}\\-\\d{3}\\-\\d{4}");
  private static final Pattern US_2 = Pattern.compile("\\(\\d{3}\\)\\d{3}\\-\\d{4}");
  
  public Number() {
    super();
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
  
  @Override public void setText(final String text) {
    if(E164.matcher(text).matches() || US_1.matcher(text).matches() || US_2.matcher(text).matches()) {
      super.setText(text);
    } else {
      throw new IllegalArgumentException(text + " is not a valid phone number for the <" + NAME + "> tag.");
    }
  }
}
