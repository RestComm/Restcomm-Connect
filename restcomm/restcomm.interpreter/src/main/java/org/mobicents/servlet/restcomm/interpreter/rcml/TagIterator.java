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

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe
public final class TagIterator implements Iterator<Tag> {
    private final Stack<Tag> stack;

    public TagIterator(final Tag tag) {
        super();
        stack = new Stack<Tag>();
        stack.push(tag);
    }

    @Override
    public boolean hasNext() {
        return stack.isEmpty() ? false : true;
    }

    @Override
    public Tag next() {
        if (stack.isEmpty()) {
            return null;
        }
        final Tag tag = stack.pop();
        if (tag.hasChildren()) {
            final List<Tag> children = tag.children();
            for (int index = (children.size() - 1); index >= 0; index--) {
                stack.push(children.get(index));
            }
        }
        return tag;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove() is not a supported operation.");
    }
}
