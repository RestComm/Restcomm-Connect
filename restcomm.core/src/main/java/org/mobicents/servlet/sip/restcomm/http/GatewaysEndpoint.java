package org.mobicents.servlet.sip.restcomm.http;

import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.GatewaysDao;

@ThreadSafe public final class GatewaysEndpoint extends AbstractEndpoint {
  private final GatewaysDao dao;
  protected final Gson gson;
  protected final XStream xstream;

  public GatewaysEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getGatewaysDao();
    gson = null;
    xstream = null;
  }
}
