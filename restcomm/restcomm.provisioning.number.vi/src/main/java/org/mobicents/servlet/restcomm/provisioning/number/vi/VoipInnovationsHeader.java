package org.mobicents.servlet.restcomm.provisioning.number.vi;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

@Immutable
public final class VoipInnovationsHeader {
    private final String id;

    public VoipInnovationsHeader(final String id) {
        super();
        this.id = id;
    }

    public String id() {
        return id;
    }
}
