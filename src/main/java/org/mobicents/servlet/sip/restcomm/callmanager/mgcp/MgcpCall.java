package org.mobicents.servlet.sip.restcomm.callmanager.mgcp;

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;
import org.mobicents.servlet.sip.restcomm.callmanager.DtmfDetector;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaPlayer;
import org.mobicents.servlet.sip.restcomm.callmanager.MediaRecorder;
import org.mobicents.servlet.sip.restcomm.callmanager.SpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.fsm.FSM;
import org.mobicents.servlet.sip.restcomm.fsm.State;

public final class MgcpCall extends FSM implements Call {
  //Call Directions.
  private static final String INBOUND = "inbound";
  private static final String OUTBOUND_DIAL = "outbound-dial";
  // Call states.
  private static final State IDLE = new State("idle");
  private static final State QUEUED = new State("queued");
  private static final State RINGING = new State("ringing");
  private static final State IN_PROGRESS = new State("in-progress");
  private static final State COMPLETED = new State("completed");
  private static final State BUSY = new State("busy");
  private static final State FAILED = new State("failed");
  private static final State NO_ANSWER = new State("no-answer");
  private static final State CANCELLED = new State("cancelled");
  static {
    IDLE.addTransition(RINGING);
    IDLE.addTransition(QUEUED);
    QUEUED.addTransition(IN_PROGRESS);
    QUEUED.addTransition(FAILED);
    RINGING.addTransition(IN_PROGRESS);
    RINGING.addTransition(FAILED);
    RINGING.addTransition(CANCELLED);
    IN_PROGRESS.addTransition(COMPLETED);
    IN_PROGRESS.addTransition(FAILED);
  }
  
  public MgcpCall() {
    super(IDLE);
  }
  
	@Override
	public void answer() throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void bridge(Call call) throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connect() throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void dial() throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public CallManager getCallManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOriginator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaPlayer getPlayer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRecipient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaRecorder getRecorder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DtmfDetector getSignalDetector() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpeechSynthesizer getSpeechSynthesizer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hangup() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isBridged() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInConference() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void join(Conference conference) throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void leave(Conference conference) throws CallException {
		// TODO Auto-generated method stub

	}

	@Override
	public void reject() {
		// TODO Auto-generated method stub

	}

	@Override
	public void unbridge(Call call) throws CallException {
		// TODO Auto-generated method stub

	}

}
