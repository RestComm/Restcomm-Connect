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
package org.mobicents.servlet.restcomm.patterns;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
@NotThreadSafe
public abstract class StandardResponse<T> {
    private final boolean succeeded;
    private final Throwable cause;
    private final String message;
    private final T object;

    public StandardResponse(final T object) {
        super();
        this.succeeded = true;
        this.cause = null;
        this.message = null;
        this.object = object;
    }

    public StandardResponse(final Throwable cause) {
        super();
        this.succeeded = false;
        this.cause = cause;
        this.message = null;
        this.object = null;
    }

    public StandardResponse(final Throwable cause, final String message) {
        super();
        this.succeeded = false;
        this.cause = cause;
        this.message = message;
        this.object = null;
    }

    public Throwable cause() {
        return cause;
    }

    public String error() {
        return message;
    }

    public T get() {
        return object;
    }

    public boolean succeeded() {
        return succeeded;
    }
}
