package org.restcomm.connect.dao.entities;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

@Immutable
public final class AuthToken {

    public Sid getSid() {
        return sid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getDescription() {
        return description;
    }

    private final Sid sid;
    private final String authToken;
    private final String description;

    public AuthToken(final Sid sid, final String authToken, final String description){
        super();
        this.sid = sid;
        this.authToken = authToken;
        this.description = description;
    }
    public static final class Builder {
        private Sid sid;
        private String authToken;
        private String description;
        private Builder() {
            super();
        }

        public AuthToken build() {
            return new AuthToken(sid, authToken, description);
        }
        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setAuthToken(final String authToken) {
            this.authToken =authToken;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

    }
}