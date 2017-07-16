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
import org.apache.commons.lang.StringUtils;
import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.entities.Account;

/**
 * A per-request security context providing access to Oauth tokens or Account API Keys.
 * @author "Tsakiridis Orestis"
 *
 */
public class UserIdentityContext {

    final AccountKey accountKey;
    final Account effectiveAccount; // if oauthToken is set get the account that maps to it. Otherwise use account from accountKey
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
        this.accountKey = extractAccountKey(request, accountsDao);
        if (accountKey != null) {
            if (accountKey.isVerified()) {
                effectiveAccount = accountKey.getAccount();
            } else
                effectiveAccount = null;
        } else
            effectiveAccount = null;

        if (effectiveAccount != null)
            effectiveAccountRoles = extractAccountRoles(effectiveAccount);
    }

    public UserIdentityContext(Account effectiveAccount, AccountsDao accountsDao) {
        this.accountsDao = accountsDao;
        this.accountKey = null;
        this.effectiveAccount = effectiveAccount;
        if (effectiveAccount != null)
            effectiveAccountRoles = extractAccountRoles(effectiveAccount);
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

    public AccountKey getAccountKey() {
        return accountKey;
    }

    public Account getEffectiveAccount() {
        return effectiveAccount;
    }

    public Set<String> getEffectiveAccountRoles() {
        return effectiveAccountRoles;
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
