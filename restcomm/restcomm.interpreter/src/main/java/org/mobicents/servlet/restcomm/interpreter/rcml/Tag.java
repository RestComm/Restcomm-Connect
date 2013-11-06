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
package org.mobicents.servlet.restcomm.interpreter.rcml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public class Tag {
    private final Map<String, Attribute> attributes;
    private final List<Tag> children;
    private final String name;
    private final Tag parent;
    private final String text;
    private final boolean iterable;

    private Tag(final String name, final Tag parent, final String text, final Map<String, Attribute> attributes,
            final List<Tag> children, final boolean iterable) {
        super();
        this.attributes = attributes;
        this.children = children;
        this.name = name;
        this.parent = parent;
        this.text = text;
        this.iterable = iterable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Attribute attribute(final String name) {
        return attributes.get(name);
    }

    public List<Attribute> attributes() {
        return new ArrayList<Attribute>(attributes.values());
    }

    public List<Tag> children() {
        return children;
    }

    public boolean isIterable() {
        return iterable;
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

    public Iterator<Tag> iterator() {
        return new TagIterator(this);
    }

    public String name() {
        return name;
    }

    public Tag parent() {
        return parent;
    }

    public String text() {
        return text;
    }

    @Override
    public final String toString() {
        return TagPrinter.print(this);
    }

    public static final class Builder {
        private final Map<String, Attribute> attributes;
        private final List<Tag> children;
        private String name;
        private Tag parent;
        private String text;
        private boolean iterable;

        private Builder() {
            super();
            attributes = new HashMap<String, Attribute>();
            children = new ArrayList<Tag>();
            name = null;
            parent = null;
            text = null;
            iterable = false;
        }

        public void addAttribute(final Attribute attribute) {
            attributes.put(attribute.name(), attribute);
        }

        public void addChild(final Tag child) {
            children.add(child);
        }

        public Tag build() {
            return new Tag(name, parent, text, attributes, children, iterable);
        }

        public void setIterable(final boolean iterable) {
            this.iterable = iterable;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setParent(final Tag parent) {
            this.parent = parent;
        }

        public void setText(final String text) {
            this.text = text;
        }
    }
}
