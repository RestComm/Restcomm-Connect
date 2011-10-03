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
package org.mobicents.servlet.sip.restcomm.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTag implements Tag {
  protected final Map<String, Attribute> attributes;
  protected final List<Tag> children;
  protected Tag parent;
  protected String text;
  
  public AbstractTag() {
    super();
    attributes = new HashMap<String, Attribute>();
    children = new ArrayList<Tag>();
    parent = null;
    text = null;
  }

  public void addAttribute(final Attribute attribute) throws UnsupportedAttributeException {
	final String attributeName = attribute.getName();
    if(canContainAttribute(attributeName)) {
      attributes.put(attribute.getName(), attribute);
    } else {
      final String attributeValue = attribute.getValue();
      throw new UnsupportedAttributeException("The <" + getName() + "> tag does not support an attribute named "
          + attributeName + " with a value of " + attributeValue);
    }
  }
  
  public void addChild(final Tag child) throws UnsupportedTagException {
    if(canContainChild(child)) {
      child.setParent(this);
      children.add(child);
    } else {
      throw new UnsupportedTagException("The <" + getName() + "> tag does not support <" + child.getName()
          + "> as a child tag.");
    }
  }
  
  public boolean canBeRoot() {
    return false;
  }
  
  public abstract boolean canContainAttribute(String name);

  public abstract boolean canContainChild(Tag tag);

  public Attribute getAttribute(final String name) {
    return attributes.get(name);
  }

  public List<Attribute> getAttributes() {
    return new ArrayList<Attribute>(attributes.values());
  }
  
  public List<Tag> getChildren() {
    return children;
  }

  public abstract String getName();
  
  public Tag getParent() {
    return parent;
  }

  public String getText() {
    return text;
  }
  
  public boolean hasAttribute(final String name) {
    return attributes.containsKey(name);
  }
  
  public boolean hasAttributes() {
    return attributes.isEmpty() ? false : true;
  }
  
  public boolean hasChildren() {
    return children.isEmpty() ? false : true;
  }
  
  public TagIterator iterator() {
    return new PreOrderTagIterator(this);
  }
  
  public void setParent(final Tag parent) {
    this.parent = parent;
  }

  public void setText(final String text) {
    this.text = text;
  }
  
  @Override public final String toString() {
    return TagPrettyPrinter.print(this);
  }
}
