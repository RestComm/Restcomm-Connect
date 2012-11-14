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
package org.mobicents.servlet.sip.restcomm.media.api;

import java.net.URI;
import java.util.List;

import org.joda.time.DateTime;

import org.mobicents.servlet.sip.restcomm.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface Call {
  public void addObserver(CallObserver observer);
  public void answer() throws CallException;
  public void cancel() throws CallException;
  public void dial() throws CallException;
  public DateTime getDateCreated();
  public DateTime getDateStarted();
  public DateTime getDatedEnded();
  public String getDigits();
  public Direction getDirection();
  public String getForwardedFrom();
  public Sid getSid();
  public String getOriginator();
  public String getOriginatorName();
  public String getRecipient();
  public Status getStatus();
  public void hangup();
  public boolean isMuted();
  public void mute();
  public void play(List<URI> announcements, int iterations) throws CallException;
  public void playAndCollect(List<URI> prompts, int maxNumberOfDigits, int minNumberOfDigits,
      long firstDigitTimer, long interDigitTimer, String endInputKey) throws CallException;
  public void playAndRecord(List<URI> prompts, URI recordId, long postSpeechTimer,
      long recordingLength, String patterns) throws CallException;
  public void reject();
  public void removeObserver(CallObserver observer);
  public void setExpires(int minutes);
  public void stopMedia();
  public void unmute();
  
  public enum Direction {
    INBOUND("inbound"),
    OUTBOUND_DIAL("outbound-dial");
    
    private final String text;
    
    private Direction(final String text) {
      this.text = text;
    }
    
    public static Direction getValueOf(final String text) {
      final Direction[] values = values();
      for(final Direction value : values) {
        if(value.toString().equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid direction.");
    }
    
    @Override public String toString() {
      return text;
    }
  }

  public enum Status {
    IDLE("idle"),
    QUEUED("queued"),
    RINGING("ringing"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed"),
    BUSY("busy"),
    FAILED("failed"),
    NO_ANSWER("no-answer"),
    CANCELLED("cancelled"),
    TRYING("trying");

    private final String text;
    
    private Status(final String text) {
      this.text = text;
    }
    
    public static Status getValueOf(final String text) {
      final Status[] values = values();
      for(final Status value : values) {
        if(value.toString().equals(text)) {
          return value;
        }
      }
      throw new IllegalArgumentException(text + " is not a valid status.");
    }
    
    @Override public String toString() {
      return text;
    }
  }
}
