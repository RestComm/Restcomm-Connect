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
package org.mobicents.servlet.restcomm.cache;

import java.net.URI;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.patterns.StandardResponse;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class DiskCacheResponse extends StandardResponse<URI> {
    public DiskCacheResponse(final Throwable cause, final String message) {
        super(cause, message);
    }

    public DiskCacheResponse(final Throwable cause) {
        super(cause);
    }

    public DiskCacheResponse(final URI object) {
        super(object);
    }

    @Override
    public String toString() {
        return "DiskCacheResponse [" + super.toString() + "]";
    }
}
