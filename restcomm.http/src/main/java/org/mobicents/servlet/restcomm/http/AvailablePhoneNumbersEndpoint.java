package org.mobicents.servlet.restcomm.http;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

@ThreadSafe public final class AvailablePhoneNumbersEndpoint extends AbstractEndpoint {
  @Context protected ServletContext context;
  protected Configuration configuration;

  public AvailablePhoneNumbersEndpoint() {
    super();
  }
  
  @PostConstruct
  public void init() {
	configuration = (Configuration)context.getAttribute(Configuration.class.getName());
	configuration = configuration.subset("runtime-settings");
    super.init(configuration);
  }
}
