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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class EsendexSmsAggregator implements Runnable, SmsAggregator {
  private static final Logger logger = Logger.getLogger(EsendexSmsAggregator.class);

  private Configuration configuration;
  private String account;
  private String user;
  private String password;
  
  private Thread worker;
  private volatile boolean running;
  private BlockingQueue<SmsMessageRequest> queue;
  
  public EsendexSmsAggregator() {
    super();
  }
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  @Override public void run() {
    while(running) {
      SmsMessageRequest request = null;
      try { request = queue.take(); }
      catch(final InterruptedException ignored) { }
      if(request != null) {
        try {
        	final EsendexService service = new EsendexService(user, password, account);
        	service.sendMessage(request.getTo(), request.getBody());
//          final EsendexHeader header = new EsendexHeader(user, password, account);
//  	      final SendServiceLocator locator = new SendServiceLocator();
//          final SendServiceSoap_BindingStub service = (SendServiceSoap_BindingStub)locator.getSendServiceSoap();
//          service.setHeader(header);
//  	      service.sendMessage(request.getTo(), request.getBody(), MessageType.Text);
  	      if(request.getObserver() != null) {
  	        request.getObserver().succeeded();
  	      }
        } catch(final Exception exception) {
          logger.error(exception);
          if(request.getObserver() != null) {
            request.getObserver().failed();
          }
        }
      }
    }
  }

  @Override public void start() throws RuntimeException {
    account = configuration.getString("account");
    user = configuration.getString("user");
    password = configuration.getString("password");
    queue = new LinkedBlockingQueue<SmsMessageRequest>();
    worker = new Thread(this);
    worker.setName("Esendex Aggregator Worker");
    running = true;
    worker.start();
  }

  @Override public void send(final String from, final String to, final String body, final SmsAggregatorObserver observer)
      throws SmsAggregatorException {
    try { queue.put(new SmsMessageRequest(to, body, observer)); }
    catch(final InterruptedException ignored) { }
  }
  
  @Override public void shutdown() {
    if(running) {
      running = false;
      worker.interrupt();
    }
  }
  
  @Immutable private final class SmsMessageRequest {
    private final String to;
    private final String body;
    private final SmsAggregatorObserver observer;

    private SmsMessageRequest(final String to, final String body, final SmsAggregatorObserver observer) {
      super();
      this.to = to;
      this.body = body;
      this.observer = observer;
    }
    
    private String getTo() {
      return to;
    }
    
    private String getBody() {
      return body;
    }
    
    private SmsAggregatorObserver getObserver() {
      return observer;
    }
  }
}
