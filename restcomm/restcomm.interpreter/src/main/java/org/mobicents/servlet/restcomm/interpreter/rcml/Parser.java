/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.interpreter.rcml;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import static javax.xml.stream.XMLStreamConstants.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.mobicents.servlet.restcomm.interpreter.rcml.Verbs.*;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class Parser extends UntypedActor {
    private final Tag document;
    private final Iterator<Tag> iterator;

    private Tag current;

    public Parser(final InputStream input) throws IOException {
        this(new InputStreamReader(input));
    }

    public Parser(final Reader reader) throws IOException {
        super();
        final XMLInputFactory inputs = XMLInputFactory.newInstance();
        inputs.setProperty("javax.xml.stream.isCoalescing", true);
        XMLStreamReader stream = null;
        try {
            stream = inputs.createXMLStreamReader(reader);
            document = parse(stream);
            if (document == null) {
                throw new IOException("There was an error parsing the RCML.");
            }
            iterator = document.iterator();
        } catch (final XMLStreamException exception) {
            throw new IOException(exception);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final XMLStreamException nested) {
                    throw new IOException(nested);
                }
            }
        }
    }

    public Parser(final String text) throws IOException {
        this(new StringReader(text.trim().replaceAll("&([^;]+(?!(?:\\w|;)))", "&amp;$1")));
    }

    private void end(final Stack<Tag.Builder> builders, final XMLStreamReader stream) {
        if (builders.size() > 1) {
            final Tag.Builder builder = builders.pop();
            final Tag tag = builder.build();
            builders.peek().addChild(tag);
        }
    }

    private void start(final Stack<Tag.Builder> builders, final XMLStreamReader stream) {
        final Tag.Builder builder = Tag.builder();
        // Read the next tag.
        builder.setName(stream.getLocalName());
        // Read the attributes.
        final int limit = stream.getAttributeCount();
        for (int index = 0; index < limit; index++) {
            final String name = stream.getAttributeLocalName(index);
            final String value = stream.getAttributeValue(index).trim();
            final Attribute attribute = new Attribute(name, value);
            builder.addAttribute(attribute);
        }
        builders.push(builder);
    }

    private Tag next() {
        while (iterator.hasNext()) {
            final Tag tag = iterator.next();
            if (isVerb(tag)) {
                if (current != null && current.hasChildren()) {
                    final List<Tag> children = current.children();
                    if (children.contains(tag)) {
                        continue;
                    }
                }
                current = tag;
                return current;
            }
        }
        return null;
    }

    private Tag parse(final XMLStreamReader stream) throws IOException, XMLStreamException {
        final Stack<Tag.Builder> builders = new Stack<Tag.Builder>();
        while (stream.hasNext()) {
            switch (stream.next()) {
                case START_ELEMENT: {
                    start(builders, stream);
                    continue;
                }
                case CHARACTERS: {
                    text(builders, stream);
                    continue;
                }
                case END_ELEMENT: {
                    end(builders, stream);
                    continue;
                }
                case END_DOCUMENT: {
                    if (!builders.isEmpty()) {
                        return builders.pop().build();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        if (GetNextVerb.class.equals(klass)) {
            final Tag verb = next();
            if (verb != null) {
                sender.tell(verb, self);
            } else {
                final End end = End.instance();
                sender.tell(end, sender);
            }
        }
    }

    private void text(final Stack<Tag.Builder> builders, final XMLStreamReader stream) {
        if (!stream.isWhiteSpace()) {
            // Read the text.
            final Tag.Builder builder = builders.peek();
            final String text = stream.getText().trim();
            builder.setText(text);
        }
    }
}
