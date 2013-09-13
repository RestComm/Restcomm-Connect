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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.URI;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.mobicents.servlet.restcomm.mgcp.PlayRecord;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public final class PlayRecordTest {
  public PlayRecordTest() {
    super();
  }
  
//  @Ignore
  @Test public void testFormatting() {
	// We will used the builder pattern to create this message.
    final PlayRecord.Builder builder = PlayRecord.builder();
    builder.addPrompt(URI.create("hello.wav"));
    builder.setRecordingId(URI.create("recording.wav"));
    builder.setClearDigitBuffer(true);
    builder.setPreSpeechTimer(1000);
    builder.setPostSpeechTimer(1000);
    builder.setRecordingLength(15000);
    builder.setEndInputKey("#");
    final PlayRecord playRecord = builder.build();
    final String result = playRecord.toString();
    System.out.println(result);
    assertTrue("ip=hello.wav ri=recording.wav cb=true prt=10 pst=10 rlt=150 eik=#".equals(result));
    System.out.println("Signal Parameters: " + result + "\n");
  }
  
  @Test public void testFormattingDefaults() {
	// We will used the builder pattern to create this message.
    final PlayRecord.Builder builder = PlayRecord.builder();
    builder.addPrompt(URI.create("hello.wav"));
    builder.setRecordingId(URI.create("recording.wav"));
    final PlayRecord playRecord = builder.build();
    final String result = playRecord.toString();
    assertTrue("ip=hello.wav ri=recording.wav".equals(result));
    System.out.println("Signal Parameters: " + result + "\n");
  }
  
//  @Ignore
  @Test public void testFormattingWithNoPrompts() {
	// We will used the builder pattern to create this message.
    final PlayRecord.Builder builder = PlayRecord.builder();
    builder.setRecordingId(URI.create("recording.wav"));
    builder.setClearDigitBuffer(true);
    builder.setPreSpeechTimer(1000);
    builder.setPostSpeechTimer(1000);
    builder.setRecordingLength(15000);
    builder.setEndInputKey("#");
    final PlayRecord playRecord = builder.build();
    final String result = playRecord.toString();
    assertTrue("ri=recording.wav cb=true prt=10 pst=10 rlt=150 eik=#".equals(result));
    System.out.println("Signal Parameters: " + result + "\n");
  }
  
//  @Ignore
  @Test public void testFormattingWithMultiplePrompts() {
	// We will used the builder pattern to create this message.
    final PlayRecord.Builder builder = PlayRecord.builder();
    builder.addPrompt(URI.create("hello.wav"));
    builder.addPrompt(URI.create("world.wav"));
    builder.setRecordingId(URI.create("recording.wav"));
    builder.setClearDigitBuffer(true);
    builder.setPreSpeechTimer(1000);
    builder.setPostSpeechTimer(1050);
    builder.setRecordingLength(15000);
    builder.setEndInputKey("#");
    final PlayRecord playRecord = builder.build();
    final String result = playRecord.toString();
    assertTrue("ip=hello.wav;world.wav ri=recording.wav cb=true prt=10 pst=11 rlt=150 eik=#".equals(result));
    System.out.println("Signal Parameters: " + result + "\n");
  }
}
