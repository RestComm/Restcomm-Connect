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

package org.restcomm.connect.identity;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.mobicents.servlet.restcomm.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.commons.security.PasswordAlgorithm;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.identity.passwords.PasswordUtils;

/**
 * A per-request security context providing access to Oauth tokens or Account API Keys.
 * @author "Tsakiridis Orestis"
 *
 */
public class UserIdentityContext {

    String basicAuthUsername;
    String basicAuthPass;
    boolean basicAuthVerified = false;
    AuthType authType = null;

    Account effectiveAccount; // if oauthToken is set get the account that maps to it. Otherwise use account from accountKey
    Set<String> effectiveAccountRoles;
    List<String> accountLineage = null; // list of all parent account Sids up to the lop level account. It's initialized in a lazy way.

    AccountsDao accountsDao;

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
    public UserIdentityContext(HttpServletRequest request, AccountsDao accountsDao) {
        this.accountsDao = accountsDao;
        Account authorizedAccount = authorizeRequest(request.getHeader("Authorization"), accountsDao);
        if (authorizedAccount != null) {
            effectiveAccount = authorizedAccount;
            effectiveAccountRoles = extractAccountRoles(effectiveAccount);
        }
    }

    /**
     * Authorizes the request checking various types of credentials and returns the account representing
     * the authorized entity that made the request.
     *
     * @param authHeader
     * @param accountsDao
     * @return
     */
    Account authorizeRequest(String authHeader, AccountsDao accountsDao) {
        if (authHeader != null) {
            processBasicAuthHeader(authHeader);
            if (basicAuthUsername != null) { // do we have basic authorization header ?
                Account account = accountsDao.getAccountToAuthenticate(basicAuthUsername);
                if (account != null) { // ok, the account is there.
                    if (basicAuthPass != null) {
                        // Try to match against both password and AuthToken
                        if ( PasswordUtils.verifyPassword(basicAuthPass, account.getPassword(), account.getPasswordAlgorithm() ) ) {
                            authType = AuthType.Password;
                            basicAuthVerified = true;
                            return account;
                        } else if ( basicAuthPass.equals(account.getAuthToken())) {
                            authType = AuthType.AuthToken;
                            basicAuthVerified = true;
                            return account;
                        }
                        // TODO does it make any difference whether the Account SID or email is used ?
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts credentials from a Basic authorization header and populates basicAuthUsername/Pass properties.
     *
     * @param authHeader
     * @return
     */
    void processBasicAuthHeader(String authHeader) {
        if (authHeader != null) {
            String[] parts = authHeader.split(" ");
            if (parts.length >= 2 && parts[0].equals("Basic")) {
                String base64Credentials = parts[1].trim();
                String credentials = new String(Base64.decodeBase64(base64Credentials), Charset.forName("UTF-8"));
                // credentials = username:password
                final String[] values = credentials.split(":",2);
                if (values.length >= 2) {
                    this.basicAuthUsername = values[0];
                    this.basicAuthPass = values[1];
                }
            }
        }
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

    public Account getEffectiveAccount() {
        return effectiveAccount;
    }

    public Set<String> getEffectiveAccountRoles() {
        return effectiveAccountRoles;
    }

    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Returns the list of ancestors for the effective (the one specified in the credentials) account
     * in a lazy way.
     *
     * @return
     */
    public List<String> getEffectiveAccountLineage() {
        if (accountLineage == null) {
            if (effectiveAccount != null) {
                try {
                    accountLineage = accountsDao.getAccountLineage(effectiveAccount);
                } catch (AccountHierarchyDepthCrossed e) {
                    throw new RuntimeException("Logged account has a very big line of ancestors. Something seems wrong. Account sid: " + effectiveAccount.getSid().toString(), e);
                }
            }
        }
        return accountLineage;
    }
}
