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

package org.restcomm.connect.mscontrol.api;

import akka.actor.Props;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MediaServerControllerFactory {

    /**
     * Provides a new Media Server Controller Props for a Call.
     *
     * @return The media server controller props
     */
    Props provideCallControllerProps();

    /**
     * Provides a new Media Server Controller Props for a Conference.
     *
     * @return The media server controller props
     */
    Props provideConferenceControllerProps();

    /**
     * Provides a new Media Server Controller Props for a Bridge.
     *
     * @return The media server controller props
     */
    Props provideBridgeControllerProps();

}
