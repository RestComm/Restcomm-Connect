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

import java.io.InputStream;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.mobicents.servlet.sip.restcomm.ObjectInstantiationException;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class RcmlDocumentBuilder {
  private final TagFactory factory;
  
  public RcmlDocumentBuilder(final TagFactory factory) {
    super();
    this.factory = factory;
  }
  
  public RcmlDocument build(final InputStream input) throws RcmlDocumentBuilderException {
    try {
      final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
      final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(input);
      return parse(xmlStreamReader);
    } catch(final XMLStreamException exception) {
      throw new RcmlDocumentBuilderException(exception);
    } catch(final ObjectInstantiationException exception) {
      throw new RcmlDocumentBuilderException(exception);
	}
  }
  
  private void doCharacters(final XMLStreamReader reader, final Stack<Tag> tagStack) {
	if(!reader.isWhiteSpace()) {
	  String text = reader.getText();
	  text = text.trim();
	  final Tag tag = tagStack.peek();
	  tag.setText(text);
	}
  }
  
  private void doStartElement(final XMLStreamReader xmlStreamReader, final Stack<Tag> tagStack)
      throws ObjectInstantiationException {
    final String tagName = xmlStreamReader.getLocalName();
    final Tag tag = factory.getTagInstance(tagName);
    // Process the current element's attributes.
    final int attributeCount = xmlStreamReader.getAttributeCount();
    if(attributeCount > 0) {
      for(int index = 0; index < attributeCount; index++) {
        final String attributeName = xmlStreamReader.getAttributeLocalName(index);
        final String attributeValue = xmlStreamReader.getAttributeValue(index);
      	final Attribute attribute = factory.getAttributeInstance(attributeName);
      	attribute.setValue(attributeValue);
      	tag.addAttribute(attribute);
      }
    }
    tagStack.push(tag);
  }
  
  private void doEndElement(final XMLStreamReader xmlStreamReader, final Stack<Tag> tagStack) {
    final Tag tag = tagStack.pop();
    if(!tagStack.isEmpty()) {
      tagStack.peek().addChild(tag);
    } else {
      tagStack.push(tag);
    }
  }
  
  private RcmlDocument parse(final XMLStreamReader xmlStreamReader) throws XMLStreamException, ObjectInstantiationException {
    final Stack<Tag> tagStack = new Stack<Tag>();
    Tag tag = null;
    while(xmlStreamReader.hasNext()) {
  	  switch(xmlStreamReader.next()) {
  	    case XMLStreamConstants.START_ELEMENT: {
  	  	  doStartElement(xmlStreamReader, tagStack);
  	      continue;
  	    }
  	    case XMLStreamConstants.CHARACTERS: {
  	      doCharacters(xmlStreamReader, tagStack);
  	      continue;
  	    }
  	    case XMLStreamConstants.END_ELEMENT: {
  	      doEndElement(xmlStreamReader, tagStack);
  	      continue;
  	    }
  	    case XMLStreamConstants.END_DOCUMENT: {
  	      if(!tagStack.isEmpty() && tagStack.size() == 1) {
  	    	tag = tagStack.pop();
  	      }
  	      continue;
  	    }
  	    default: {
  	      continue;
  	    }
  	  }
  	}
    return (RcmlDocument)tag;
  }
}
