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

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class XmlDocumentBuilder {
  private final TagFactory factory;
  
  public XmlDocumentBuilder(final TagFactory factory) {
    super();
    this.factory = factory;
  }
  
  public XmlDocument build(final InputStream input) throws XmlDocumentBuilderException {
    try {
      final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
      final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(input);
      return parse(xmlStreamReader);
    } catch(final XMLStreamException exception) {
      throw new XmlDocumentBuilderException(exception);
    } catch(final ObjectInstantiationException exception) {
      throw new XmlDocumentBuilderException(exception);
	} catch(final UnsupportedTagException exception) {
	  throw new XmlDocumentBuilderException(exception);
	} catch(final UnsupportedAttributeException exception) {
	  throw new XmlDocumentBuilderException(exception);
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
      throws ObjectInstantiationException, UnsupportedTagException, UnsupportedAttributeException {
    final String tagName = xmlStreamReader.getLocalName();
    final Tag tag = factory.getTagInstance(tagName);
    // Process the current element's attributes.
    final int attributeCount = xmlStreamReader.getAttributeCount();
    if(attributeCount > 0) {
      for(int index = 0; index < attributeCount; index++) {
        final String attributeName = xmlStreamReader.getAttributeLocalName(index);
        if(tag.canContainAttribute(attributeName)) {
          final String attributeValue = xmlStreamReader.getAttributeValue(index);
      	  Attribute attribute = tag.getAttribute(attributeName);
      	  if(attribute == null) {
      	    attribute = factory.getAttributeInstance(attributeName);
      	    tag.addAttribute(attribute);
      	  }
          attribute.setValue(attributeValue);
        }
      }
    }
    tagStack.push(tag);
  }
  
  private void doEndElement(final XMLStreamReader xmlStreamReader, final Stack<Tag> tagStack) throws UnsupportedTagException {
    final Tag tag = tagStack.pop();
    if(!tagStack.isEmpty()) {
      final Tag parent = tagStack.peek();
      if(parent.canContainChild(tag)) {
        parent.addChild(tag);
      }
    } else {
      tagStack.push(tag);
    }
  }
  
  private XmlDocument parse(final XMLStreamReader xmlStreamReader) throws XMLStreamException, ObjectInstantiationException,
      UnsupportedTagException, UnsupportedAttributeException, XmlDocumentBuilderException {
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
  	    	if(!tag.canBeRoot()) {
  	    	  throw new XmlDocumentBuilderException("The tag <" + tag.getName() + "> can not be a RestComm XML resource root tag.");
  	    	}
  	      }
  	      continue;
  	    }
  	    default: {
  	      continue;
  	    }
  	  }
  	}
    return (XmlDocument)tag;
  }
}
