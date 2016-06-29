/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.identity;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;

/**
 * A per-request security context providing access to an effective account
 * authorization tokens and credentials extracted from the request. When keycloak-based
 * auth is used an identity instance reference should also be present.
 *
 * @author "Tsakiridis Orestis"
 */
public class UserIdentityContext {
    protected Logger logger = Logger.getLogger(UserIdentityContext.class);

    AccountKey accountKey;
    Account effectiveAccount; // if oauthToken is set get the account that maps to it. Otherwise use account from accountKey
    Set<String> effectiveAccountRoles;
    // keycloak related properties
    final String oauthTokenString;
    final AccessToken oauthToken; // verified token
    final KeycloakDeployment keycloakDeployment;

    /**
     * After successfull creation of a UserIdentityContext object the following stands:
     * - if an oauth token was present and verified *oauthToken* will contain it. Otherwise it will be null
     * - TODO If a *linked* account exists for the oauth token username *effectiveAccount* will be set
     * - if BASIC http credentials were present *accountKey* will contain them. Check accountKey.isVerified()
     * - if BASIC http credentials were verified effective account will be set to this account.
     * - if both oauthToken and accountKey are set and verified, effective account will be set to the account indicated by accountKey.
     * @param request
     * @param accountsDao
     */
    public UserIdentityContext(KeycloakDeployment keycloakDeployment, HttpServletRequest request, AccountsDao accountsDao) {
        // identityContext should be there for any oauth-related operation to take place
        this.keycloakDeployment = keycloakDeployment;
        if (keycloakDeployment == null) {
            oauthTokenString = null;
            oauthToken = null;
        } else {
            // try to initialize oauth token from request
            final String tokenString = extractOauthTokenString(request);
            if (!StringUtils.isEmpty(tokenString)) {
                this.oauthToken = verifyToken(tokenString, keycloakDeployment);
                this.oauthTokenString = tokenString;
                if (this.oauthToken != null) {
                    //ok, we have a verified token, let's try to load the account
                    this.effectiveAccount = accountsDao.getAccountToAuthenticate(oauthToken.getPreferredUsername());
                }

            } else {
                this.oauthToken = null;
                this.oauthTokenString = null;
            }
        }
        // if we failed getting an effective account using oauth tokens, try basic auth too
        if (this.effectiveAccount == null) {
            this.accountKey = extractAccountKey(request, accountsDao);
            if (accountKey != null) {
                if (accountKey.isVerified()) {
                    effectiveAccount = accountKey.getAccount();
                } else
                    effectiveAccount = null;
            } else
                effectiveAccount = null;
        }
        // setup roles for effective account
        if (effectiveAccount != null)
            effectiveAccountRoles = extractAccountRoles(effectiveAccount);
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

    private Set<String> extractAccountRoles(Account account) {
        if (account == null)
            return null;
        Set<String> roles = new HashSet<String>();
        if (!StringUtils.isEmpty(account.getRole())) {
            roles.add(account.getRole());
        }
        return roles;
    }

    private AccessToken verifyToken(String tokenString, KeycloakDeployment deployment) {
        AccessToken token;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl());
            return token;
        } catch (VerificationException e) {
            logger.error("Cannot verity token", e);
            return null;
        }
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
                if (values.length >= 2) {
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

    public Set<String> getEffectiveAccountRoles() {
        return effectiveAccountRoles;
    }

}
