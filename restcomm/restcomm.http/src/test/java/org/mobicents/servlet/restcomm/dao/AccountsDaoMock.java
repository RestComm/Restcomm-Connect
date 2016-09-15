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

package org.mobicents.servlet.restcomm.dao;

import org.apache.commons.lang.NotImplementedException;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

import java.util.ArrayList;
import java.util.List;

/**
 * Elementary mocking for AccountsDao to be used for endpoint unit testing mostly.
 * Add further implementations if needed.
 *
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class AccountsDaoMock implements AccountsDao {

    List<Account> accounts = new ArrayList<Account>();

    public AccountsDaoMock(List<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public void addAccount(Account account) {
        throw new NotImplementedException();
    }

    @Override
    public Account getAccount(Sid sid) {
        for (Account account: accounts) {
            if (account.getSid().toString().equals(sid))
                return account;
        }
        return null;
    }

    @Override
    public Account getAccount(String name) {
        throw new NotImplementedException();
    }

    @Override
    public Account getAccountToAuthenticate(String name) {
        for (Account account: accounts) {
            if (account.getEmailAddress().equals(name)) {
                return account;
            }
        }
        return null;
    }

    @Override
    public List<Account> getAccounts(Sid sid) {
        throw new NotImplementedException();
    }

    @Override
    public void removeAccount(Sid sid) {
        throw new NotImplementedException();
    }

    @Override
    public void updateAccount(Account account) {
        throw new NotImplementedException();
    }

    @Override
    public List<String> getSubAccountSidsRecursive(Sid parentAccountSid) {
        throw new NotImplementedException();
    }
}
