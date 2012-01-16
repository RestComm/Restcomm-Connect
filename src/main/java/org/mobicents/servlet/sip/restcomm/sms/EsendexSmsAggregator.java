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
package org.mobicents.servlet.sip.restcomm.sms;

import org.apache.commons.configuration.Configuration;

import com.esendex.sdk.ems.soapinterface.EsendexHeader;
import com.esendex.sdk.ems.soapinterface.MessageType;
import com.esendex.sdk.ems.soapinterface.SendServiceLocator;
import com.esendex.sdk.ems.soapinterface.SendServiceSoap_BindingStub;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class EsendexSmsAggregator implements SmsAggregator {
  private String account;
  private String user;
  private String password;
  
  public EsendexSmsAggregator() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    account = configuration.getString("account");
    user = configuration.getString("user");
    password = configuration.getString("password");
  }

  @Override public void start() throws RuntimeException {
    // Nothing to do.
  }

  @Override public void send(final String from, final String to, final String body)
      throws SmsAggregatorException {
    try {
	  final EsendexHeader header = new EsendexHeader(user, password, account);
	  final SendServiceLocator locator = new SendServiceLocator();
      final SendServiceSoap_BindingStub service = (SendServiceSoap_BindingStub)locator.getSendServiceSoap();
      service.setHeader(header);
	  service.sendMessage(to, body, MessageType.Text);
	} catch(final Exception exception) {
	  throw new SmsAggregatorException(exception);
	}
  }
  
  @Override public void shutdown() {
    // Nothing to do.
  }
}
