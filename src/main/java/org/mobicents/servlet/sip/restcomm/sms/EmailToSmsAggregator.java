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

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

public final class EmailToSmsAggregator implements SmsAggregator {
  private static final Logger LOGGER = Logger.getLogger(EmailToSmsAggregator.class);
  
  private String domain;
  private String gateway;
  private String server;
  
  public EmailToSmsAggregator() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    domain = configuration.getString("domain");
    gateway = configuration.getString("gateway");
    server = configuration.getString("smtp");
  }

  @Override public void initialize() throws RuntimeException {
    // Nothing to do.
  }
  
  public void send(final String sender, final String recipient, final String body) throws SmsAggregatorException {
	try {
      Properties properties = new Properties();
      properties.put("mail.smtp.host", server);
      Session session = Session.getDefaultInstance(properties, null);
      Message message = new MimeMessage(session);
      final String from = new StringBuilder()
          .append("restcomm@").append(domain).toString();
      message.setFrom(new InternetAddress(from));  
      InternetAddress to[] = new InternetAddress[1];
      final String recipientAddress = new StringBuilder()
          .append(recipient).append("@").append(gateway).toString();
      to[0] = new InternetAddress(recipientAddress);
      message.setRecipients(Message.RecipientType.TO, to);
      message.setContent(body, "text/plain");
      Transport.send(message);
      if(LOGGER.isDebugEnabled()) {
	    final StringBuilder buffer = new StringBuilder();
	    buffer.append("Sent SMS message from ").append(from).append(" to ").append(recipientAddress);
	    LOGGER.debug(buffer.toString());
	  }
    } catch(final MessagingException exception) {
	  throw new SmsAggregatorException(exception);
	}
  }

  @Override public void shutdown() {
    // Nothing to do.
  }
}
