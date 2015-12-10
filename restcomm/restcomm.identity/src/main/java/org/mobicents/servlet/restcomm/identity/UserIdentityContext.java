package org.mobicents.servlet.restcomm.identity;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.identity.keycloak.IdentityContext;

/**
 * A per-request security context providing access to Oauth tokens or Account API Keys.
 * @author "Tsakiridis Orestis"
 *
 */
public class UserIdentityContext {

    final String oauthTokenString;
    final AccessToken oauthToken;
    final AccountKey accountKey;
    final Account effectiveAccount; // if oauthToken is set get the account that maps to it. Otherwise use account from accountKey

    public UserIdentityContext(IdentityContext identityContext, HttpServletRequest request, AccountsDao accountsDao) {
        if (identityContext == null) {
            oauthTokenString = null;
            oauthToken = null;
        } else {
            final String tokenString = extractOauthTokenString(request);
            if (!StringUtils.isEmpty(tokenString)) {
                this.oauthToken = verifyToken(tokenString, identityContext);
                this.oauthTokenString = tokenString;
            } else {
                this.oauthToken = null;
                this.oauthTokenString = null;
            }
        }
        this.accountKey = extractAccountKey(request, accountsDao);
        //updateEffectiveAccount(accountsDao);

        if (oauthToken != null) {
            effectiveAccount = accountsDao.getAccount(oauthToken.getPreferredUsername());
        } else
        if (accountKey != null) {
            effectiveAccount = accountKey.getAccount();
        } else
            effectiveAccount = null;
    }

    private String extractOauthTokenString(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Bearer")) {
                String tokenString = parts[1];
                return tokenString;
            }
        }
        return null;
    }

    private AccessToken verifyToken(String tokenString, IdentityContext identityContext) {
        return IdentityUtils.verifyToken(tokenString, identityContext.getDeployment());
    }

    private AccountKey extractAccountKey(HttpServletRequest request, AccountsDao dao) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2)
                    if ( Sid.valid(values[0]) ) {
                        AccountKey accountKey = new AccountKey(values[0], values[1], dao);
                        return accountKey;
                    }

            }
        }
        return null;
    }

    public AccessToken getOauthToken() {
        return oauthToken;
    }

    public String getOauthTokenString() {
        return oauthTokenString;
    }

    public AccountKey getAccountKey() {
        return accountKey;
    }

    public Account getEffectiveAccount() {
        return effectiveAccount;
    }

    // generates a random api key to be used for accessing an Account using Basic HTTP authentication
    public static String generateApiKey() {
        // TODO generate proper API key values. Not just UUIDs
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
