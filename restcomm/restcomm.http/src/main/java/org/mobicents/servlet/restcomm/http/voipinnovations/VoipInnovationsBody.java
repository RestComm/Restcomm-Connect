package org.mobicents.servlet.restcomm.http.voipinnovations;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

@Immutable
public final class VoipInnovationsBody {
    private final Object content;

    public VoipInnovationsBody(final Object content) {
        super();
        this.content = content;
    }

    public Object content() {
        return content;
    }
}
