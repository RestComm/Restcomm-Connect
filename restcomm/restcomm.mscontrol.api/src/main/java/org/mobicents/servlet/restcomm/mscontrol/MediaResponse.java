/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.mobicents.servlet.restcomm.mscontrol;

import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.restcomm.patterns.StandardResponse;

/**
 * Generic response to a request made to the {@link MediaSessionController}.
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @param <T> The type of the object wrapped in the response.
 */
@NotThreadSafe
public class MediaResponse<T> extends StandardResponse<T> {

    public MediaResponse(T object) {
        super(object);
    }

    public MediaResponse(final Throwable cause) {
        super(cause);
    }

    public MediaResponse(final Throwable cause, final String message) {
        super(cause, message);
    }
}
