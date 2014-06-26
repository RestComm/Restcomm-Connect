package org.mobicents.servlet.restcomm.rvd.security;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class RvdSecurityContext implements SecurityContext {

    private final RvdUser user;

    public RvdSecurityContext(RvdUser user) {
        this.user = user;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isSecure() {
        if ( user != null )
            return true;
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }

    @Override
    public String toString() {
        return "SecurityContext - " + (user == null ? "no user logged in" : ("user " + user.getName() + " logged in" ));
    }

}
