package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.CallIdentifier;

import org.mobicents.servlet.sip.restcomm.callmanager.MediaSession;

public final class MgcpMediaSession implements MediaSession {
  private final CallIdentifier callId;
  private final MgcpServer server;
  
  public MgcpMediaSession(final String id, final MgcpServer server) {
    super();
    this.callId = new CallIdentifier(id);
    this.server = server;
  }
  
  public CallIdentifier getCallId() {
    return callId;
  }
  
  public ConferenceEndPoint getConferenceEndPoint() {
    return null;
  }
  
  public IvrEndPoint getIvrEndPoint() {
    return null;
  }
  
  public PacketRelayEndPoint getPacketRelayEndPoint() {
    return null;
  }
  
  public void release() {
    
  }
}
