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
package org.mobicents.servlet.restcomm.asr;

import java.util.Map;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.patterns.StandardResponse;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class AsrResponse<T> extends StandardResponse<T> {
    private final Map<String, Object> attributes;

    public AsrResponse(final T object, final Map<String, Object> attributes) {
        super(object);
        this.attributes = attributes;
    }

    public AsrResponse(T object) {
        this(object, null);
    }

    public AsrResponse(final Throwable cause, final Map<String, Object> attributes) {
        super(cause);
        this.attributes = attributes;
    }

    public AsrResponse(final Throwable cause) {
        this(cause, null);
    }

    public AsrResponse(final Throwable cause, final String message, final Map<String, Object> attributes) {
        super(cause, message);
        this.attributes = attributes;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
