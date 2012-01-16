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

import java.util.List;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface Tag {
  public void addAttribute(Attribute attribute) throws UnsupportedAttributeException;
  public void addChild(Tag child) throws UnsupportedTagException;
  public boolean canBeRoot();
  public boolean canContainAttribute(String name);
  public boolean canContainChild(Tag tag);
  public Attribute getAttribute(String name);
  public List<Attribute> getAttributes();
  public List<Tag> getChildren();
  public String getName();
  public Tag getParent();
  public String getText();
  public boolean hasAttribute(String name);
  public boolean hasAttributes();
  public boolean hasChildren();
  public TagIterator iterator();
  public void setParent(Tag parent);
  public void setText(String text);
}
