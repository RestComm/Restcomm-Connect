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
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 * Throw when an RVD project is not synched with Applications i.e. has no respective application entity on the restcomm side.
 * The exception was defined to handle case where such errors should be ignored for old applications until a workspace
 * upgrade was in place.
 */
public class ApplicationApiNotSynchedException extends ApplicationsApiSyncException {

    public ApplicationApiNotSynchedException() {
        // TODO Auto-generated constructor stub
    }

    public ApplicationApiNotSynchedException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ApplicationApiNotSynchedException(String message, Throwable throwable) {
        super(message, throwable);
        // TODO Auto-generated constructor stub
    }

}
