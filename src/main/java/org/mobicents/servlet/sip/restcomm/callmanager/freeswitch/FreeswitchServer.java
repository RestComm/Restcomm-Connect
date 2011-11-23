package org.mobicents.servlet.sip.restcomm.callmanager.freeswitch;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.inbound.InboundConnectionFailure;
import org.freeswitch.esl.client.transport.event.EslEvent;

import org.mobicents.servlet.sip.restcomm.Configurable;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;

public final class FreeswitchServer implements Configurable, IEslEventListener, LifeCycle {
  // FreeSwitch event socket client connection.
  private final Client client;
  // FreeSwitch calls.
  private final Map<String, Call> calls;
  // Freeswitch configuration.
  private Configuration configuration;
  
  public FreeswitchServer() {
    super();
    client = new Client();
    client.addEventListener(this);
    calls = new HashMap<String, Call>();
  }
  
  @Override public void backgroundJobResultReceived(final EslEvent event) {
    
  }
  
  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  private void callAnswered(final EslEvent event) {
    final Map<String, String> headers = event.getEventHeaders();
    final String uuid = headers.get("Unique-ID");
    final FreeswitchCall call = (FreeswitchCall)calls.get(uuid);
    if(call != null) {
      
    }
  }
  
  private void callCreated(final EslEvent event) {
    final Map<String, String> headers = event.getEventHeaders();
    final String uuid = headers.get("Unique-ID");
    // Create a new call.
    final FreeswitchCall call = new FreeswitchCall(client, uuid);
    // Start ringing.
    call.alert();
    // Update calls.
    calls.put(uuid, call);
  }
  
  private void callDestroyed(final EslEvent event) {
	final Map<String, String> headers = event.getEventHeaders();
	final String uuid = headers.get("Unique-ID");
    calls.remove(uuid);
  }
  
  private void callParked(final EslEvent event) {
    final Map<String, String> headers = event.getEventHeaders();
    final String uuid = headers.get("Unique-ID");
    final FreeswitchCall call = (FreeswitchCall)calls.get(uuid);
    if(call != null) {
      
    }
  }
  
  @Override public void eventReceived(final EslEvent event) {
    if(event.getEventName().equals("CHANNEL_ANSWER")) {
      callAnswered(event);
    } else if(event.getEventName().equals("CHANNEL_CREATE")) {
      callCreated(event);
    } else if(event.getEventName().equals("CHANNEL_DESTROY")) {
      callDestroyed(event);
    } else if(event.getEventName().equals("CHANNEL_PARK")) {
      callParked(event);
    }
  }
  
  public Call getCall(final String uuid) {
    return calls.get(uuid);
  }

  @Override public void initialize() throws RuntimeException {
    final String address = configuration.getString("host");
    final int port = configuration.getInt("port");
    final String password = configuration.getString("password");
    final int timeout = configuration.getInt("timeout");
    try {
      client.connect(address, port, password, timeout);
      // Subscribe to all of the FreeSwitch events.
      client.setEventSubscriptions("plain", "all");
      // Filter-in the events we care about.
      client.addEventFilter("Event-Name", "CHANNEL_ANSWER");
      client.addEventFilter("Event-Name", "CHANNEL_BRIDGE");
      client.addEventFilter("Event-Name", "CHANNEL_UNBRIDGE");
      client.addEventFilter("Event-Name", "CHANNEL_CREATE");
      client.addEventFilter("Event-Name", "CHANNEL_DESTROY");
      client.addEventFilter("Event-Name", "CHANNEL_HANGUP");
      client.addEventFilter("Event-Name", "CHANNEL_PARK");
      client.addEventFilter("Event-Name", "CHANNEL_UNPARK");
      client.addEventFilter("Event-Name", "conference::maintenance");
      client.addEventFilter("Event-Name", "DETECTED_SPEECH");
      client.addEventFilter("Event-Name", "DTMF");
      client.addEventFilter("Event-Name", "PLAYBACK_START");
      client.addEventFilter("Event-Name", "PLAYBACK_STOP");
      client.addEventFilter("Event-Name", "RECORD_START");
      client.addEventFilter("Event-Name", "RECORD_STOP");
      client.addEventFilter("Event-Name", "spandsp::txfaxresult");
      client.addEventFilter("Event-Name", "spandsp::rxfaxresult");
    } catch(final InboundConnectionFailure exception) {
	  final StringBuilder buffer = new StringBuilder();
      buffer.append("Could not connect to the FreeSwitch server located at").append(" '").append(address)
          .append(":").append(port).append("' using ").append("password '").append(password).append("'");
      throw new RuntimeException(buffer.toString(), exception);
    }
  }

  @Override public void shutdown() {
	// Clean up all the calls.
	calls.clear();
	// Close the connection to the freeswitch server.
	client.cancelEventSubscriptions();
    client.close();
  }
}
