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
package org.mobicents.servlet.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.*;

import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.exceptions.ConstraintViolationException;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class MybatisAccountsDao implements AccountsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.AccountsDao.";
    private final SqlSessionFactory sessions;

    public MybatisAccountsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addAccount(final Account account) {
        final SqlSession session = sessions.openSession();
        try {
            if ( !checkEmailRestriction(account) )
                throw new ConstraintViolationException("Cannot add account '" + account.getSid() + "'. email_address '" + account.getEmailAddress() + "' already taken.");
            session.insert(namespace + "addAccount", toMap(account));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Account getAccount(final Sid sid) {
        return getAccount(namespace + "getAccount", sid.toString());
    }

    @Override
    public Account getAccount(final String name) {
        Account account = null;

        account = getAccount(namespace + "getAccountByFriendlyName", name);
        if (account == null){
            account = getAccount(namespace + "getAccountByEmail", name);
        }
        if (account == null) {
            account = getAccount(namespace + "getAccount", name);
        }

        return account;
    }

    @Override
    public Account getAccountByEmail(final String name) {
        Account account = null;

        account = getAccount(namespace + "getAccountByEmail", name);

        return account;
    }

    private Account getAccount(final String selector, final Object parameters) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(selector, parameters);
            if (result != null) {
                return toAccount(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<Account> getAccounts(final Sid accountSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAccounts", accountSid.toString());
            final List<Account> accounts = new ArrayList<Account>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    accounts.add(toAccount(result));
                }
            }
            return accounts;
        } finally {
            session.close();
        }
    }

    /**
     * Returns all available accounts.
     *
     * If toplevel is true it will return only the toplevel accounts.
     * @param toplevel
     * @return
     */
    @Override
    public List<Account> getAccounts() {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllAccounts");
            final List<Account> accounts = new ArrayList<Account>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    accounts.add(toAccount(result));
                }
            }
            return accounts;
        } finally {
            session.close();
        }
    }

    @Override
    public void removeAccount(final Sid sid) {
        removeAccount(namespace + "removeAccount", sid);
    }

    private void removeAccount(final String selector, final Sid sid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(selector, sid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateAccount(final Account account) {
        updateAccount(namespace + "updateAccount", account);
    }

    private void updateAccount(final String selector, final Account account) {
        final SqlSession session = sessions.openSession();
        try {
            if ( !checkEmailRestriction(account) )
                throw new ConstraintViolationException("Cannot add account '" + account.getSid() + "'. email_address '" + account.getEmailAddress() + "' already taken.");
            session.update(selector, toMap(account));
            session.commit();
        } finally {
            session.close();
        }
    }

    /**
     * Check if there is already a different account with the email address of addedAccount. If there such
     * a conflict, return false. Otherwise return true.
     *
     * This function enforces email uniqueness from the application layer (not the database layer). It's not
     * an atomic action so, beware.
     *
     * @param addedAccount
     * @return
     */
    private boolean checkEmailRestriction(Account addedAccount) {
        if ( StringUtils.isEmpty(addedAccount.getEmailAddress()) )
            return true; // email address is already empty so we are safe to proceed
        Account existingAccount = getAccountByEmail(addedAccount.getEmailAddress());
        if (existingAccount == null)
            return true; // no account found with that email address
        else
        if (existingAccount.getSid().equals(addedAccount.getSid()))
            return true; // ok, its the same account.
        else
            return false;
    }

    private Account toAccount(final Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String emailAddress = readString(map.get("email_address"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid accountSid = readSid(map.get("account_sid"));
        final Account.Type type = readAccountType(map.get("type"));
        final Account.Status status = readAccountStatus(map.get("status"));
        final String authToken = readString(map.get("auth_token"));
        final String role = readString(map.get("role"));
        final URI uri = readUri(map.get("uri"));
        final Boolean linked = readBoolean(map.get("linked"));
        return new Account(sid, dateCreated, dateUpdated, emailAddress, friendlyName, accountSid, type, status, authToken,
                role, uri, linked);
    }

    private Map<String, Object> toMap(final Account account) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(account.getSid()));
        map.put("date_created", writeDateTime(account.getDateCreated()));
        map.put("date_updated", writeDateTime(account.getDateUpdated()));
        map.put("email_address", account.getEmailAddress());
        map.put("friendly_name", account.getFriendlyName());
        map.put("account_sid", writeSid(account.getAccountSid()));
        map.put("type", writeAccountType(account.getType()));
        map.put("status", writeAccountStatus(account.getStatus()));
        map.put("auth_token", account.getAuthToken());
        map.put("role", account.getRole());
        map.put("uri", writeUri(account.getUri()));
        map.put("linked", account.getLinked());
        return map;
    }
}
