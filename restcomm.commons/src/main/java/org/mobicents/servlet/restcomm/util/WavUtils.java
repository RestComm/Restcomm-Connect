/*
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
package org.mobicents.servlet.restcomm.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class WavUtils {
  private WavUtils() {
    super();
  }
  
  public static double getAudioDuration(final URI wavFile)
      throws MalformedURLException, UnsupportedAudioFileException, IOException {
    return getAudioDuration(new File(wavFile));
  }
  
  public static double getAudioDuration(final File wavFile)
      throws MalformedURLException, UnsupportedAudioFileException, IOException {
	AudioInputStream audio = null;
    try {
      if(wavFile != null && wavFile.exists()) {
        audio = AudioSystem.getAudioInputStream(wavFile);
        final AudioFormat format = audio.getFormat();
        return wavFile.length() / format.getSampleRate() / (format.getSampleSizeInBits() / 8.0) / format.getChannels();
      }
      return 0;
    } finally {
      if(audio != null) {
        audio.close();
      }
    }
  }
}
