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
package org.mobicents.servlet.restcomm.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class WavUtils {
    private WavUtils() {
        super();
    }

    public static double getAudioDuration(final URI wavFile) throws UnsupportedAudioFileException, IOException {
        return getAudioDuration(new File(wavFile));
    }

    public static double getAudioDuration(final File wavFile) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audio = null;
        try {
            if (wavFile != null && wavFile.exists()) {
                audio = AudioSystem.getAudioInputStream(wavFile);
                final AudioFormat format = audio.getFormat();
                return wavFile.length() / format.getSampleRate() / (format.getSampleSizeInBits() / 8.0) / format.getChannels();
            }
            return 0;
        } catch (UnsupportedAudioFileException exception) {
            // Return calculation based on MMS defaults
            int sampleRate = 8000;
            int sampleSize = 16;
            int channels = 1;
            return wavFile.length() / (sampleRate / 8.0) / sampleSize / channels;
        } finally {
            if (audio != null) {
                audio.close();
            }
        }
    }
}
