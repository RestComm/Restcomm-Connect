package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import jain.protocol.ip.mgcp.message.parms.CallIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.mobicents.servlet.sip.restcomm.callmanager.MediaSession;

public final class MgcpMediaSession implements MediaSession {
  private final CallIdentifier callId;
  private final MgcpServer server;
  
  private final List<EndPoint> endpoints;
  private final ReentrantLock endpointsLock;
  
  public MgcpMediaSession(final MgcpServer server, final int id) {
    super();
    this.server = server;
    this.callId = new CallIdentifier(Integer.toString(id));
    this.endpoints = new ArrayList<EndPoint>();
    this.endpointsLock = new ReentrantLock();
  }
  
  private void addEndPoint(final EndPoint endpoint) {
    endpointsLock.lock();
    try {
      endpoints.add(endpoint);
    } finally {
      endpointsLock.unlock();
    }
  }
  
  public ConferenceEndPoint createConferenceEndPoint() {
	final ConferenceEndPoint endpoint = new ConferenceEndPoint(server, this);
	addEndPoint(endpoint);
    return endpoint;
  }
  
  public IvrEndPoint createIvrEndPoint() {
	final IvrEndPoint endpoint = new IvrEndPoint(server, this);
	addEndPoint(endpoint);
    return endpoint;
  }
  
  public PacketRelayEndPoint createPacketRelayEndPoint() {
	final PacketRelayEndPoint endpoint = new PacketRelayEndPoint(server, this);
	addEndPoint(endpoint);
    return endpoint;
  }
  
  public void destoryEndPoint(final EndPoint endpoint) {
	endpointsLock.lock();
	try {
      endpoints.remove(endpoint);
	} finally {
	  endpointsLock.unlock();
	}
  }
  
  public CallIdentifier getCallId() {
    return callId;
  }
  
  public void release() {
	// Release all the media resources.
	endpointsLock.lock();
	try {
      for(final EndPoint endpoint : endpoints) {
        endpoint.release();
      }
	} finally {
	  endpointsLock.unlock();
	}
	// Destroy this media session.
    server.destroyMediaSession(this);
  }
}
