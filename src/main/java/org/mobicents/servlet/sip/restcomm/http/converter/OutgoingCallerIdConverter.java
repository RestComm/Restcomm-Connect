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

import org.mobicents.servlet.sip.restcomm.OutgoingCallerId;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class OutgoingCallerIdConverter extends AbstractConverter {
  public OutgoingCallerIdConverter() {
    super();
  }

  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return OutgoingCallerId.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final OutgoingCallerId outgoingCallerId = (OutgoingCallerId)object;
    writeSid(outgoingCallerId.getSid(), writer);
    writeAccountSid(outgoingCallerId.getAccountSid(), writer);
    writeFriendlyName(outgoingCallerId.getFriendlyName(), writer);
    writePhoneNumber(outgoingCallerId.getPhoneNumber(), writer);
    writeDateCreated(outgoingCallerId.getDateCreated(), writer);
    writeDateUpdated(outgoingCallerId.getDateUpdated(), writer);
    writeUri(outgoingCallerId.getUri(), writer);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
    return null;
  }
}
