/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.connect.dao.entities;

/**
 * @author guilherme.jansen@telestax.com
 */
public enum MediaType {
    AUDIO_ONLY("audio_only"), VIDEO_ONLY("video_only"), AUDIO_VIDEO("audio_video");

    private final String text;

    MediaType(final String text) {
        this.text = text;
    }

    public static MediaType getValueOf(final String text) {
        MediaType[] values = values();
        for (final MediaType value : values) {
            if (value.toString().equals(text)) {
                return value;
            }
        }
        throw new IllegalArgumentException(text + " is not a valid media type.");
    }

    @Override
    public String toString() {
        return text;
    }

}
