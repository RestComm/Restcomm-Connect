package org.mobicents.servlet.sip.restcomm.callmanager;

import javax.media.mscontrol.MsControlFactory;

public final class Jsr309MediaServer {
  private MsControlFactory factory;
  
  public Jsr309MediaServer(final MsControlFactory factory) {
    super();
    this.factory = factory;
  }
  
  public MsControlFactory getMsControlFactory() {
    return factory;
  }
}
