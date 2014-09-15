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

    @Override
    public String toString() {
        return (new StringBuffer("StandardResponse [succeeded=").append(succeeded).append(", cause=").append(cause)
                .append(", message=").append(message).append(", object=").append(object).append("]")).toString();
    }
}
