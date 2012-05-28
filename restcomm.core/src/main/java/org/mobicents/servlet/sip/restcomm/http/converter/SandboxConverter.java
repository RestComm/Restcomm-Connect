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
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.SandBox;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class SandboxConverter extends AbstractConverter {
  public SandboxConverter() {
    super();
  }
  
  @SuppressWarnings("rawtypes")
  @Override public boolean canConvert(final Class klass) {
    return false;
  }

  @Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
      final MarshallingContext context) {
    final SandBox sandbox = (SandBox)object;
    writeDateCreated(sandbox.getDateCreated(), writer);
    writeDateUpdated(sandbox.getDateUpdated(), writer);
    writePin(sandbox.getPin(), writer);
    writeAccountSid(sandbox.getAccountSid(), writer);
    writePhoneNumber(sandbox.getPhoneNumber(), writer);
    writeApplicationSid(sandbox.getApplicationSid(), writer);
    writeApiVersion(sandbox.getApiVersion(), writer);
    writeVoiceUrl(sandbox.getVoiceUrl(), writer);
    writeVoiceMethod(sandbox.getVoiceMethod(), writer);
    writeSmsUrl(sandbox.getSmsUrl(), writer);
    writeSmsMethod(sandbox.getSmsMethod(), writer);
    writeStatusCallback(sandbox.getStatusCallback(), writer);
    writeStatusCallbackMethod(sandbox.getStatusCallbackMethod(), writer);
    writeUri(sandbox.getUri(), writer);
  }
  
  private void writeApplicationSid(final Sid applicationSid, final HierarchicalStreamWriter writer) {
    writer.startNode("ApplicationSid");
    writer.setValue(applicationSid.toString());
    writer.endNode();
  }
  
  private void writePin(final String pin, final HierarchicalStreamWriter writer) {
    writer.startNode("Pin");
    writer.setValue(pin);
    writer.endNode();
  }
}
