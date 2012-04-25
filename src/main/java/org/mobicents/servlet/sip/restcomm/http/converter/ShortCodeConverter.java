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

import org.mobicents.servlet.sip.restcomm.ShortCode;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ShortCodeConverter extends AbstractConverter {
  public ShortCodeConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return ShortCode.class.equals(klass);
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final ShortCode shortCode = (ShortCode)object;
    writeSid(shortCode.getSid(), writer);
    writeDateCreated(shortCode.getDateCreated(), writer);
    writeDateUpdated(shortCode.getDateUpdated(), writer);
    writeFriendlyName(shortCode.getFriendlyName(), writer);
    writeAccountSid(shortCode.getAccountSid(), writer);
    writeShortCode(Integer.toString(shortCode.getShortCode()), writer);
    writeApiVersion(shortCode.getApiVersion(), writer);
    writeSmsUrl(shortCode.getSmsUrl(), writer);
    writeSmsMethod(shortCode.getSmsMethod(), writer);
    writeSmsFallbackUrl(shortCode.getSmsFallbackUrl(), writer);
    writeSmsFallbackMethod(shortCode.getSmsFallbackMethod(), writer);
    writeUri(shortCode.getUri(), writer);
  }

  @Override public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
    return null;
  }
  
  private void writeShortCode(final String shortCode, final HierarchicalStreamWriter writer) {
    writer.startNode("ShortCode");
    writer.setValue(shortCode);
    writer.endNode();
  }
}
