package org.restcomm.connect.provisioning.number.vi;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

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
