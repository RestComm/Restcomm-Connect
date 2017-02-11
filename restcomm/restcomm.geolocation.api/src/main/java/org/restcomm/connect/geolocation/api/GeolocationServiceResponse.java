/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.geolocation.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.patterns.StandardResponse;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
@Immutable
public final class GeolocationServiceResponse<T> extends StandardResponse<T> {

    /*****************************************************/
    /*** Phase II: internetworking with GMLC through SIP */
    /*****************************************************/
    public GeolocationServiceResponse(final T object) {
        super(object);
    }

    public GeolocationServiceResponse(final Throwable cause, final String message) {
        super(cause, message);
    }

    public GeolocationServiceResponse(final Throwable cause) {
        super(cause);
    }
}
