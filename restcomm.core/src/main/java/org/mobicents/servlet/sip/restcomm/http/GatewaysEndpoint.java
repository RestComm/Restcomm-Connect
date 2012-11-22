package org.mobicents.servlet.sip.restcomm.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.sip.restcomm.entities.Gateway;
import org.mobicents.servlet.sip.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.sip.restcomm.http.converter.GatewayConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.GatewayListConverter;
import org.mobicents.servlet.sip.restcomm.http.converter.RestCommResponseConverter;

@ThreadSafe public final class GatewaysEndpoint extends AbstractEndpoint {
  private final GatewaysDao dao;
  protected final Gson gson;
  protected final XStream xstream;

  public GatewaysEndpoint() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    dao = services.get(DaoManager.class).getGatewaysDao();
    final GatewayConverter converter = new GatewayConverter(configuration);
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Gateway.class, converter);
    builder.setPrettyPrinting();
    gson = builder.create();
    xstream = new XStream();
    xstream.alias("RestcommResponse", RestCommResponse.class);
    xstream.registerConverter(converter);
    xstream.registerConverter(new GatewayListConverter(configuration));
    xstream.registerConverter(new RestCommResponseConverter(configuration));
  }
}
