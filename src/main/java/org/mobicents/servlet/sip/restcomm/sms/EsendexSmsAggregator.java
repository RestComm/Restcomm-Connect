package org.mobicents.servlet.sip.restcomm.sms;

import org.apache.commons.configuration.Configuration;

import com.esendex.sdk.ems.soapinterface.EsendexHeader;
import com.esendex.sdk.ems.soapinterface.MessageType;
import com.esendex.sdk.ems.soapinterface.SendServiceLocator;
import com.esendex.sdk.ems.soapinterface.SendServiceSoap_BindingStub;

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

  @Override public void initialize() throws RuntimeException {
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
