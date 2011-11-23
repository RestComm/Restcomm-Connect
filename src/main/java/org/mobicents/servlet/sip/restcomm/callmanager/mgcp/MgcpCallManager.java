package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import javax.servlet.sip.SipServlet;

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;

public final class MgcpCallManager extends SipServlet implements CallManager {
  private static final long serialVersionUID = 4758133818077979879L;

  @Override public Call createCall(final String from, final String to) throws CallManagerException {
    return null;
  }
}
