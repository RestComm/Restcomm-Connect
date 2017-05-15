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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * Gathers media attributes that are used to create a media session.
 *
 * @author guilherme.jansen@telestax.com
 */
@Immutable
public class MediaAttributes {

    private final MediaType mediaType;
    // video attributes
    private final VideoMode videoMode;
    private final VideoResolution videoResolution;
    private final VideoLayout videoLayout;
    private final String videoOverlay;

    /**
     * Constructor for audio-only media sessions
     */
    public MediaAttributes() {
        this.mediaType = MediaType.AUDIO_ONLY;
        this.videoMode = null;
        this.videoResolution = null;
        this.videoLayout = null;
        this.videoOverlay = null;
    }

    /**
     * Constructor for audio-video or video-only media sessions
     *
     * @param mediaType
     * @param videoMode
     * @param videoResolution
     * @param videoLayout
     * @param videoOverlay
     */
    public MediaAttributes(final MediaType mediaType, final VideoMode videoMode, final VideoResolution videoResolution,
            final VideoLayout videoLayout, final String videoOverlay) {
        if (MediaType.AUDIO_ONLY.equals(mediaType)) {
            throw new IllegalArgumentException("Informed mediaType is not compatible with video.");
        }
        this.mediaType = mediaType;
        this.videoMode = videoMode;
        this.videoResolution = videoResolution;
        this.videoLayout = videoLayout;
        this.videoOverlay = videoOverlay;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public VideoMode getVideoMode() {
        return videoMode;
    }

    public VideoResolution getVideoResolution() {
        return videoResolution;
    }

    public VideoLayout getVideoLayout() {
        return videoLayout;
    }

    public String getVideoOverlay() {
        return videoOverlay;
    }

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

    public enum VideoMode {
        MCU("mcu"), SFU("sfu");

        final String text;

        VideoMode(final String text) {
            this.text = text;
        }

        public static VideoMode getValueOf(final String text) {
            VideoMode[] values = values();
            for (final VideoMode value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid video mode.");
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum VideoResolution {
        CIF("CIF"), FOUR_CIF("4CIF"), SIXTEEN_CIF("16CIF"), QCIF("QCIF"), VGA("VGA"), SEVEN_TWENTY_P("720p");

        final String text;

        VideoResolution(final String text) {
            this.text = text;
        }

        public static VideoResolution getValueOf(final String text) {
            VideoResolution[] values = values();
            for (final VideoResolution value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid video resolution.");
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    public enum VideoLayout {
        LINEAR("linear"), TILE("tile");

        final String text;

        VideoLayout(final String text) {
            this.text = text;
        }

        public static VideoLayout getValueOf(final String text) {
            VideoLayout[] values = values();
            for (final VideoLayout value : values) {
                if (value.toString().equals(text)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(text + " is not a valid video layout.");
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

}
