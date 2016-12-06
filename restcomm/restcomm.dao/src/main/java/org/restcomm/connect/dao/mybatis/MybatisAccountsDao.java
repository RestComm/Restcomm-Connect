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
package org.restcomm.connect.dao.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mobicents.servlet.restcomm.dao.exceptions.AccountHierarchyDepthCrossed;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.mybatis.rolling_upgrades.AccountAccessor;
import org.restcomm.connect.dao.mybatis.rolling_upgrades.DefaultAccountAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public class MybatisAccountsDao implements AccountsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.AccountsDao.";
    private Integer accountRecursionDepth = 3; // maximum value for recursive account queries
    private final SqlSessionFactory sessions;
    protected AccountAccessor accountAccessor;

    public MybatisAccountsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
        this.accountAccessor = new DefaultAccountAccessor();
    }

    public void setAccountRecursionDepth(Integer accountRecursionDepth) {
        this.accountRecursionDepth = accountRecursionDepth;
    }

    @Override
    public void addAccount(final Account account) {
        final SqlSession session = sessions.openSession();
        try {
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
        if (account == null) {
            account = getAccount(namespace + "getAccountByEmail", name);
        }
        if (account == null) {
            account = getAccount(namespace + "getAccount", name);
        }

        return account;
    }

    @Override
    public Account getAccountToAuthenticate(final String name) {
        Account account = null;

        account = getAccount(namespace + "getAccountByEmail", name);
        if (account == null) {
            account = getAccount(namespace + "getAccount", name);
        }

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
    public List<Account> getChildAccounts(final Sid parentSid) {
        final SqlSession session = sessions.openSession();
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getChildAccounts", parentSid.toString());
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

    @Override
    public List<String> getSubAccountSidsRecursive(Sid parentAccountSid) {
        List<String> parentList = new ArrayList<String>();
        parentList.add(parentAccountSid.toString());
        List<String> allChildren = new ArrayList<String>();

        int depth = 1;
        List<String> childrenList = getSubAccountsSids(parentList);
        while (childrenList != null && !childrenList.isEmpty() && depth <= accountRecursionDepth) {
            allChildren.addAll(childrenList);
            childrenList = getSubAccountsSids(childrenList); // retrieve children's children

            depth ++;
        }

        return allChildren;
    }

    @Override
    public List<String> getAccountLineage(Sid accountSid) throws AccountHierarchyDepthCrossed {
        if (accountSid == null)
            return null;
        List<String> ancestorList = new ArrayList<String>();
        Sid sid = accountSid;
        Account account = getAccount(sid);
        if (account == null)
            throw new IllegalArgumentException("Wrong accountSid is given to search for ancestor on it. This account does not even exist");
        int depth = 1; // already having one-level of accounts
        while ( true ) {
            Sid parentSid = account.getParentSid();
            if (parentSid != null) {
                depth ++;
                if (depth > accountRecursionDepth)
                    throw new AccountHierarchyDepthCrossed();
                ancestorList.add(parentSid.toString());
                Account parentAccount = getAccount(parentSid);
                if (parentAccount == null)
                    throw new IllegalStateException("Parent account " + parentSid.toString() + " does not exist although its child does " + account.getSid().toString());
                account = parentAccount;
            } else
                break;
        }
        return ancestorList;
    }

    @Override
    public List<String> getAccountLineage(Account account) throws AccountHierarchyDepthCrossed {
        if (account == null)
            return null;
        List<String> lineage = new ArrayList<String>();
        Sid parentSid = account.getParentSid();
        if (parentSid != null) {
            lineage.add(parentSid.toString());
            lineage.addAll(getAccountLineage(parentSid));
        }
        return lineage;
    }

    private List<String> getSubAccountsSids(List<String> parentAccountSidList) {
        final SqlSession session = sessions.openSession();
        try {
            final List<String> results = session.selectList(namespace + "getSubAccountSids", parentAccountSidList);
            return results;
        } finally {
            session.close();
        }
    }

    private void updateAccount(final String selector, final Account account) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(selector, toMap(account));
            session.commit();
        } finally {
            session.close();
        }
    }

    private Account toAccount(final Map<String, Object> map) {
        return accountAccessor.toAccount(map);
    }

    private Map<String, Object> toMap(final Account account) {
        return accountAccessor.toMap(account);
    }
}
