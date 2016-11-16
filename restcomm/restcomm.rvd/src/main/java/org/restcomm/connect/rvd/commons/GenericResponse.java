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

package org.restcomm.connect.rvd.commons;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class GenericResponse<T> {
    private boolean succeeded = false;
    private Throwable cause = null;
    private String message = null;
    private T object = null;
    private Integer httpFailureStatus = null;

    public GenericResponse() {
        this.succeeded = true;
    }

    public GenericResponse(final T object) {
        this.succeeded = true;
        this.object = object;
    }

    public GenericResponse(final Throwable cause) {
        this.cause = cause;
    }

    public GenericResponse(final Throwable cause, final String message) {
        this.cause = cause;
        this.message = message;
    }

    public GenericResponse(Integer httpFailureStatus) {
        this.httpFailureStatus = httpFailureStatus;
    }

    public GenericResponse(Integer httpFailureStatus, String message) {
        this.httpFailureStatus = httpFailureStatus;
        this.message = message;
    }

    public GenericResponse(String message) {
        this.message = message;
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

    public Integer getHttpFailureStatus() {
        return httpFailureStatus;
    }

    @Override
    public String toString() {
        return "GenericResponse{" +
                "succeeded=" + succeeded +
                ", cause=" + cause +
                ", message='" + message + '\'' +
                ", object=" + object +
                ", httpFailureStatus=" + httpFailureStatus +
                '}';
    }
}
