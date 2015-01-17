package org.mobicents.servlet.restcomm.mscontrol;

import javax.servlet.sip.SipServletRequest;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * Instructs a {@link MediaSessionController} to setup a new Media Session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Immutable
public final class SetupMediaSession {
    
    // Define possible call directions.
    public static final String INBOUND = "inbound";
    public static final String OUTBOUND_API = "outbound-api";
    public static final String OUTBOUND_DIAL = "outbound-dial";
    
    private final SipServletRequest invite;
    private final String callDirection;
    
    public SetupMediaSession(SipServletRequest invite, String callDirection) {
        super();
        this.invite = invite;
        this.callDirection = callDirection;
    }
    
    public SipServletRequest getInvite() {
        return invite;
    }
    
    public String getCallDirection() {
        return callDirection;
    }
}
