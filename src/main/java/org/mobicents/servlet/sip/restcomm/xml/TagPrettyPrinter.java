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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;

public final class TagPrettyPrinter {
  private static final int TAB_SPACES = 2;
  
  private TagPrettyPrinter() {
    super();
  }
  
  public static String print(final Tag tag) {
    final StringBuilder buffer = new StringBuilder();
    final TagIterator iterator = tag.iterator();
    final Stack<Tag> parents = new Stack<Tag>();
    // Use an iterator to print the tag and its children.
    while(iterator.hasNext()) {
      final Tag current = iterator.next();
      // Close the previous tag's parent if the current tag doesn't share the same parent.
      if(!parents.isEmpty() && parents.peek() != current.getParent()) {
        final Tag parent = parents.pop();
        final int depth = getTagDepth(parent);
        appendClosingTag(buffer, depth, parent);
      }
      // Append the current tag to the buffer.
      final int depth = getTagDepth(current);
      appendOpeningTag(buffer, depth, current);
      if(!current.hasChildren()) {
        appendText(buffer, depth, current);
        appendClosingTag(buffer, depth, current);
      } else {
        parents.push(current);
        continue;
      }
    }
    // Close the root tag if necessary.
    if(!parents.isEmpty()) {
      final Tag parent = parents.pop();
      final int depth = getTagDepth(parent);
      appendClosingTag(buffer, depth, parent);
    }
    return buffer.toString();
  }
  
  private static final void appendAttributes(final StringBuilder buffer, final Tag tag) {
    final List<Attribute> attributes = tag.getAttributes();
    for(int index = 0; index < attributes.size(); index++) {
      final Attribute attribute = attributes.get(index);
      buffer.append(attribute.getName()).append("=\"") .append(attribute.getValue()).append("\"");
      if(index < (attributes.size() - 1)) {
        buffer.append(" ");
      }
    }
  }
  
  private static final void appendOpeningTag(final StringBuilder buffer, final int tabs, final Tag tag) {
    appendTabs(buffer, tabs);
    buffer.append("<").append(tag.getName());
    if(tag.hasAttributes()) {
	  buffer.append(" ");
      appendAttributes(buffer, tag);
    }
    if(tag.hasChildren() || tag.getText() != null) {
      buffer.append(">");
    } else {
      buffer.append("/>");
    }
    buffer.append("\n");
  }
  
  private static final void appendClosingTag(final StringBuilder buffer, final int tabs, final Tag tag) {
    if(tag.hasChildren() || tag.getText() != null ) {
      appendTabs(buffer, tabs);
      buffer.append("</").append(tag.getName()).append(">");
      buffer.append("\n");
    }
  }
  
  private static final void appendTabs(final StringBuilder buffer, final int tabs) {
    final int spaces = tabs * TAB_SPACES;
    for(int counter = 0; counter < spaces; counter++) {
      buffer.append(" ");
    }
  }
  
  private static final void appendText(final StringBuilder buffer, final int tabs, final Tag tag) {
    final String text = tag.getText();
    if(text != null && !text.isEmpty()) {
      final ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes());
      final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      try {
        String line = reader.readLine();
        while(line != null) {
          appendTabs(buffer, tabs + 1);
          buffer.append(line);
          buffer.append("\n");
          line = reader.readLine();
        }
      } catch(final IOException ignored) {
        // Will never happen.
      }
    }
  }
  
  private static final int getTagDepth(final Tag tag) {
    int depth = 0;
    Tag parent = tag.getParent();
    while(parent != null) {
      depth += 1;
      parent = parent.getParent();
    }
    return depth;
  }
}
