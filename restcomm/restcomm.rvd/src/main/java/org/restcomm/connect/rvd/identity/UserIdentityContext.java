package org.restcomm.connect.rvd.identity;

import org.apache.commons.codec.binary.Base64;
import org.restcomm.connect.rvd.restcomm.RestcommAccountInfo;
import org.restcomm.connect.rvd.utils.RvdUtils;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.HashSet;

/**
 * Stores identity/auth information for current requesting user. A new object created for each request.
 *
 * Semantics of properties:
 *
 *  - basicCredentials: not null if a basic http auth header exists in the request. Being not-null does not mean being authenticated.
 *  - oauthTokenString: contains the Bearer authorization header in the request. It's not necessarily authenticated.
 *  - oauthToken: present if the oauthTokenString was present and verified.
 *  - accountInfo: contains an authenticated account either using http authentication of Bearer token. If both headers
 *    exist and are authenticated, basic-auth header is used to derive accountInfo.
 *  - accountRoles: It contains the roles from the account as a set. It's not null only if accountInfo is not null. It can be empty
 *    with valid accountInfo with null or empty 'roles' field.
 *  - basicAuthHeader: Is set if the authorizationHeader was a basic-http-auth header. We keep it for ease of use so that we don't re-calculate it from basicCredentials if needed.
 *
 * How to use it:
 *
 *  - check accountInfo to quickly decide if the request has been authenticated or not and an account is available.
 *
 * @author Orestis Tsakiridis
 */
public class UserIdentityContext {
    private String oauthTokenString;
    private BasicAuthCredentials basicCredentials; // HTTP basic auth credentials
    // the following fields are independent form the authorization type (Basic or Oauth)
    private String effectiveAuthHeader;
    private AuthType authType = AuthType.None;
    private RestcommAccountInfo accountInfo;
    private Set<String> accountRoles;

    public enum AuthType {
        Basic, Oauth, None
    }


    public UserIdentityContext(String authorizationHeader, AccountProvider accountProvider) {
        this.oauthTokenString = extractOauthTokenString(authorizationHeader);
        basicCredentials = extractBasicAuthCredentials(authorizationHeader);
        // try to initialize effective account using basic auth creds
        if (basicCredentials != null) {
            this.accountInfo = accountProvider.getAccount(basicCredentials.getUsername(), authorizationHeader).get();
            if (this.accountInfo != null) {
                authType = AuthType.Basic;
                effectiveAuthHeader = authorizationHeader;
            }
        }
        // set up roles
        if (this.accountInfo != null) {
            Set<String> accountRoles = new HashSet<String>();
            if ( ! RvdUtils.isEmpty(accountInfo.getRole()) )
                accountRoles.add(accountInfo.getRole());
            this.accountRoles = accountRoles;
        }
    }

    public RestcommAccountInfo getAccountInfo() {
        return accountInfo;
    }

    public String getAccountUsername() {
        if (accountInfo != null)
            return accountInfo.getEmail_address();
        return null;
    }

    /**
     * Return a (possibly empty) set of roles of the effective account (accountInfo) or null if no such account in place.
     *
     * @return
     */
    public Set<String> getAccountRoles() {
        return accountRoles;
    };

    // return effective authorization header
    public String getEffectiveAuthorizationHeader() {
        return this.effectiveAuthHeader;
    }

    /**
     * Parses an Bearer Authorization header of an oauth request and returns the Bearer token.
     *
     * @param authHeader
     * @return the Bearer token as a string or null
     */
    private String extractOauthTokenString(String authHeader) {
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Bearer")) {
                String tokenString = parts[1];
                return tokenString;
            }
        }
        return null;
    }

    /**
     * Parses the Authorization header of a basic-http-auth request and returns a username-password pair.
     *
     * @param authHeader
     * @return a username-password pair of null
     */
    private BasicAuthCredentials extractBasicAuthCredentials(String authHeader) {
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2) {
                    BasicAuthCredentials creds = new BasicAuthCredentials(values[0], values[1]);
                    return creds;
                }
            }
        }
        return null;
    }

}
