package org.mobicents.servlet.restcomm.http;

import java.net.URI;

import org.joda.time.DateTime;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * Common functionality needed to both AccountsEndpoint and IdentityEndpoint.
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class AccountsCommonEndpoint extends SecuredEndpoint {

    public AccountsCommonEndpoint() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Creates an account object from the details contained in an AccessToken. No db persistence is done here.
     * @param token
     * @return
     */
    protected Account accountFromAccessToken(AccessToken token) {

        final DateTime now = DateTime.now();
        final String username = token.getPreferredUsername();

        // Issue 108: https://bitbucket.org/telestax/telscale-restcomm/issue/108/account-sid-could-be-a-hash-of-the
        final Sid sid = Sid.generate(Sid.Type.ACCOUNT, username );

        // Use keycloak firstname/lastname as a friendly name if available. Otherwise fall back to keycloak username.
        String friendlyName = token.getName(); // e.g. Orestis Tsakiridis
        String emailAddress = token.getPreferredUsername(); // TODO what do we do here? Restcomm treats emailAddress as username

        final Account.Type type = Account.Type.FULL;
        Account.Status status = Account.Status.ACTIVE;

        String rootUri = configuration.getString("root-uri");
        rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(getApiVersion(null)).append("/Accounts/").append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        //return new Account(sid, now, now, emailAddress, friendlyName, accountSid, type, status, authToken, role, uri);
        return new Account(sid, now, now, emailAddress, friendlyName, null, type, status, null, getDefaultApiKeyRole(), uri);
    }

    protected String getDefaultApiKeyRole() {
        return "Developer";
    }

}
