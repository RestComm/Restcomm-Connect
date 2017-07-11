package org.restcomm.connect.provisioning.number.vi;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

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
