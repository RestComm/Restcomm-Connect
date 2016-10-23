/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.restcomm.connect.rvd.exceptions;

/**
 * @author guilherme.jansen@telestax.com
 */
public class AccessApiException extends RvdException {

    /**
     *
     */
    private static final long serialVersionUID = 8747659349945596519L;
    private Integer statusCode;

    public AccessApiException() {

    }

    public AccessApiException(String message) {
        super(message);
    }

    public AccessApiException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public AccessApiException setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

}
