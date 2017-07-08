/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.restcomm.connect.dao;

import java.util.List;

import org.restcomm.connect.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.Account;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface AccountsDao {
    void addAccount(Account account);

    Account getAccount(Sid sid);

    Account getAccount(String name);

    /**
     * Created to separate the method used to authenticate from
     * the method used to obtain an account from the database, using a ordinary
     * String parameter.
     * Once authentication cannot allow friendly name as username, this
     * method can be similar to getAccount(String name), but without
     * 'getAccountByFriendlyName' selector.
     * @param name
     * @return Account to authenticate
     */
    Account getAccountToAuthenticate(String name);

    List<Account> getChildAccounts(Sid parentSid);

    void removeAccount(Sid sid);

    void updateAccount(Account account);

    /**
     * Returns a list of all sub-accounts under a parent account. All nested sub-accounts in
     * any level will be returned. Note:
     * a) The parent account is not included in the results.
     * b) The sub-account order in the returned list follows a top-down logic. So, higher hierarchy account list elements
     *    always go before lower hierarchy accounts.
     *
     * It will return an empty array in case the parent has no children or the parent does
     * not exist.
     *
     * @param parentAccountSid
     * @return list of account sid or null
     */
    List<String> getSubAccountSidsRecursive(Sid parentAccountSid);

    /**
     * Returns a list of all the ancestor account SIDs of an Account all the way up to the
     * top-level account. It currently works in an iterative way digging through the parentSid property
     * until it reaches the top.
     *
     * The order of the returned list is significant starting with child accounts first and
     * ending with the top-level account.
     *
     * Note, the list does NOT contain the account passed as a parameter.
     *
     * Examples:
     *
     *   getAccountLineage(toplevelAccount) -> []
     *
     *   parentAccoun is the direct child of toplevelAccount:
     *   getAccontLineage(parentAccount) -> [toplevelAccount]
     *
     *   child@company.com is the child of parent@company.com:
     *   getAccountLineage(childAccount) -> [parent@company.comSID, admininstrator@compahy.comSID]
     *
     *   grantchild@company.com is the child of child@company.com:
     *   getAccountLineage(grandchildAccount) -> AccountHierarchyDepthCrossed exctption thrown
     *
     * @param accountSid
     * @return
     */
    List<String> getAccountLineage(Sid accountSid) throws AccountHierarchyDepthCrossed;

    /**
     * Overloaded version of getAccontLineage(Sid) that won't retrieve current account since
     * it's already there. Helps having cleaner concepts in the case when the starting child
     * account is already loaded.
     *
     * @param account
     * @return
     * @throws AccountHierarchyDepthCrossed
     */
    List<String> getAccountLineage(Account account) throws AccountHierarchyDepthCrossed;
}
