package org.mobicents.servlet.restcomm.mscontrol;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * Instructs a {@link MediaSessionController} to setup a new Media Session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Immutable
public final class CreateMediaSession {
    
    private final String sdp;
    
    public CreateMediaSession(String sdp) {
        super();
        this.sdp = sdp;
    }
    
    public String getSdp() {
        return sdp;
    }

}
