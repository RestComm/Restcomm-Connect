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
package org.mobicents.servlet.sip.restcomm.callmanager.mgcp.au;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public final class AdvancedAudioParametersBuilder {
  private final List<URI> announcement;
  private final List<URI> initialPrompt;
  private int iterations;
  private boolean clearDigitBuffer;
  private int maxNumberOfDigits;
  private int minNumberOfDigits;
  private String digitPattern;
  private long firstDigitTimer;
  private long interDigitTimer;
  private long preSpeechTimer;
  private long postSpeechTimer;
  private long recordingLength;
  private String endInputKey;
  private URI recordId;
  
  public AdvancedAudioParametersBuilder() {
    super();
    announcement = new ArrayList<URI>();
    initialPrompt = new ArrayList<URI>();
    iterations = -1;
    clearDigitBuffer = false;
    maxNumberOfDigits = -1;
    minNumberOfDigits = -1;
    digitPattern = null;
    firstDigitTimer = -1;
    interDigitTimer = -1;
    preSpeechTimer = -1;
    postSpeechTimer = -1;
    recordingLength = -1;
    endInputKey = null;
    recordId = null;
  }
  
  public void addAnnouncement(final URI uri) {
    announcement.add(uri);
  }
  
  public void addInitialPrompt(final URI uri) {
    initialPrompt.add(uri);
  }
  
  private void appendTo(final StringBuilder buffer, final String string) {
    if(string != null) {
      final boolean isEmpty = buffer.length() == 0 ? true : false;
      if(!isEmpty) {
        buffer.append(" ");
      }
      buffer.append(string);
    }
  }
  
  public String build() {
	final StringBuilder buffer = new StringBuilder();
	appendTo(buffer, buildAnnouncement());
	appendTo(buffer, buildInitialPrompt());
	appendTo(buffer, buildIterations());
	appendTo(buffer, buildClearDigitBuffer());
	appendTo(buffer, buildMaxNumberOfDigits());
	appendTo(buffer, buildMinNumberOfDigits());
	appendTo(buffer, buildDigitPattern());
	appendTo(buffer, buildFirstDigitTimer());
	appendTo(buffer, buildInterDigitTimer());
	appendTo(buffer, buildPreSpeechTimer());
	appendTo(buffer, buildPostSpeechTimer());
	appendTo(buffer, buildRecordingLength());
	appendTo(buffer, buildEndInputKey());
	appendTo(buffer, buildRecordId());
    return buffer.toString();
  }
  
  private String buildAnnouncement() {
	if(!announcement.isEmpty()) {
	  final StringBuilder buffer = new StringBuilder();
      buffer.append("an=");
      final int size = announcement.size();
      for(int index = 0; index < size; index++) {
        buffer.append(announcement.get(index));
        if(index < (size - 1)) {
          buffer.append(";");
        }
      }
      return buffer.toString();
    } else {
      return null;	
    }
  }
  
  private String buildInitialPrompt() {
    if(!initialPrompt.isEmpty()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("ip=");
      final int size = initialPrompt.size();
      for(int index = 0; index < size; index++) {
        buffer.append(initialPrompt.get(index));
        if(index < (size - 1)) {
          buffer.append(";");
        }
      }
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildIterations() {
	if(iterations > 0) {
	  final StringBuilder buffer = new StringBuilder();
      buffer.append("it=").append(iterations);
      return buffer.toString();
	} else {
	  return null;
	}
  }
  
  private String buildClearDigitBuffer() {
    if(clearDigitBuffer) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("cb=").append("true");
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildMaxNumberOfDigits() {
    if(maxNumberOfDigits > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("mx=").append(maxNumberOfDigits);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildMinNumberOfDigits() {
    if(minNumberOfDigits > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("mn=").append(minNumberOfDigits);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildDigitPattern() {
    if(digitPattern != null && !digitPattern.isEmpty()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("dp=").append(digitPattern);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildFirstDigitTimer() {
    if(firstDigitTimer > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("fdt=").append(firstDigitTimer * 10);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildInterDigitTimer() {
    if(interDigitTimer > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("idt=").append(interDigitTimer * 10);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildPreSpeechTimer() {
    if(preSpeechTimer > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("prt=").append(preSpeechTimer * 10);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildPostSpeechTimer() {
    if(postSpeechTimer > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("pst=").append(postSpeechTimer * 10);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildRecordingLength() {
    if(recordingLength > 0) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("rlt=").append(recordingLength * 1000);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildEndInputKey() {
    if(endInputKey != null && !endInputKey.isEmpty()) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("eik=").append(endInputKey);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  private String buildRecordId() {
    if(recordId != null) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("ri=").append(recordId);
      return buffer.toString();
    } else {
      return null;
    }
  }
  
  public void setClearDigitBuffer(final boolean clearDigitBuffer) {
    this.clearDigitBuffer = clearDigitBuffer;
  }
  
  public void setDigitPattern(final String digitPattern) {
    this.digitPattern = digitPattern;
  }
  
  public void setEndInputKey(final String endInputKey) {
    this.endInputKey = endInputKey;
  }
  
  public void setFirstDigitTimer(final long firstDigitTimer) {
    this.firstDigitTimer = firstDigitTimer;
  }
  
  public void setInterDigitTimer(final long interDigitTimer) {
    this.interDigitTimer = interDigitTimer;
  }
  
  public void setIterations(final int iterations) {
    this.iterations = iterations;
  }
  
  public void setMaxNumberOfDigits(final int maxNumberOfDigits) {
    this.maxNumberOfDigits = maxNumberOfDigits;
  }
  
  public void setMinNumberOfDigits(final int minNumberOfDigits) {
    this.minNumberOfDigits = minNumberOfDigits;
  }
  
  public void setRecordingLength(final long recordingLength) {
    this.recordingLength = recordingLength;
  }
  
  public void setPreSpeechTimer(final long preSpeechTimer) {
    this.preSpeechTimer = preSpeechTimer;
  }
  
  public void setPostSpeechTimer(final long postSpeechTimer) {
    this.postSpeechTimer = postSpeechTimer;
  }
  
  public void setRecordId(final URI recordId) {
    this.recordId = recordId;
  }
}
