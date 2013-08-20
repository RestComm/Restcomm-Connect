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
package org.mobicents.servlet.restcomm.telephony;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable public final class Record {
  private static final List<URI> empty = new ArrayList<URI>(0);
  
  private final URI destination;
  private final List<URI> prompts;
  private final int timeout;
  private final int length;
  private final String endInputKey;
  
  public Record(final URI recordingId, final List<URI> prompts, final int timeout,
      final int length, final String endInputKey) {
    super();
    this.destination = recordingId;
    this.prompts = prompts;
    this.timeout = timeout;
    this.length = length;
    this.endInputKey = endInputKey;
  }
  
  public Record(final URI recordingId, final int timeout, final int length,
      final String endInputKey) {
    super();
    this.destination = recordingId;
    this.prompts = empty;
    this.timeout = timeout;
    this.length = length;
    this.endInputKey = endInputKey;
  }
  
  public URI destination() {
    return destination;
  }
  
  public List<URI> prompts() {
    return prompts;
  }
  
  public int timeout() {
    return timeout;
  }
  
  public int length() {
    return length;
  }
  
  public String endInputKey() {
    return endInputKey;
  }
}
