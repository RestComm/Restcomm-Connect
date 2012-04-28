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
package org.mobicents.servlet.sip.restcomm.http.converter;

import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.Transcription;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class TranscriptionConverter extends AbstractConverter {
  public TranscriptionConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return Transcription.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final Transcription transcription = (Transcription)object;
    writeSid(transcription.getSid(), writer);
    writeDateCreated(transcription.getDateCreated(), writer);
    writeDateUpdated(transcription.getDateUpdated(), writer);
    writeAccountSid(transcription.getAccountSid(), writer);
    writeStatus(transcription.getStatus().toString(), writer);
    writeRecordingSid(transcription.getRecordingSid(), writer);
    writeDuration(transcription.getDuration(), writer);
    writeTranscriptionText(transcription.getTranscriptionText(), writer);
    writePrice(transcription.getPrice(), writer);
    writeUri(transcription.getUri(), writer);
  }
  
  private void writeRecordingSid(final Sid recordingSid, final HierarchicalStreamWriter writer) {
    writer.startNode("RecordingSid");
    writer.setValue(recordingSid.toString());
    writer.endNode();
  }
  
  private void writeTranscriptionText(final String transcriptionText, final HierarchicalStreamWriter writer) {
    writer.startNode("TranscriptionText");
    if(transcriptionText != null) {
      writer.setValue(transcriptionText);
    }
    writer.endNode();
  }
}
