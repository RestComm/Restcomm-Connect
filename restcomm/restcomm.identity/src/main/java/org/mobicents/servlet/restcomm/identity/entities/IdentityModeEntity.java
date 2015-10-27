package org.mobicents.servlet.restcomm.identity.entities;

import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSet.IdentityMode;

public class IdentityModeEntity {
    private IdentityMode mode;
    private String authServerUrlBase;

    public IdentityModeEntity() {
        // TODO Auto-generated constructor stub
    }

    public IdentityMode getMode() {
        return mode;
    }

    public void setMode(IdentityMode mode) {
        this.mode = mode;
    }

    public String getAuthServerUrlBase() {
        return authServerUrlBase;
    }

    public void setAuthServerUrlBase(String authServerUrlBase) {
        this.authServerUrlBase = authServerUrlBase;
    }

}
