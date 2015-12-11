package org.mobicents.servlet.restcomm.identity.migration;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

public class MockAccountsDao implements AccountsDao {

    public List<Account> accounts = new ArrayList<Account>();
    
    public MockAccountsDao() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void addAccount(Account account) {
        accounts.add(account);
    }

    @Override
    public Account getAccount(Sid sid) {
        for (Account account : accounts) {
            if (sid.equals(account.getSid()))
                return account;
        }
        return null;
    }

    @Override
    public Account getAccount(String name) {
        for (Account account: accounts) {
            if (name.equals(account.getEmailAddress()))
                return account;
            if (name.equals(account.getSid().toString()))
                return account;
        }
        return null;
    }

    @Override
    public Account getAccountByEmail(String arg0) {
        throw new UnsupportedOperationException();
    }



    @Override
    public List<Account> getAccounts() {
        return accounts;
    }

    public Account buildTestAccount(Sid accountSid, String emailAddress, String friendlyName, String authToken, Sid parentAccountSid  ) {
        final DateTime now = DateTime.now();
        if (accountSid == null)
            accountSid = Sid.generate(Sid.Type.ACCOUNT);
        final Account.Type type = Account.Type.FULL;
        final Account.Status status = Account.Status.ACTIVE;
        return new Account(accountSid, now, now, emailAddress, friendlyName, parentAccountSid, type, status, authToken, null, null);
    }

    @Override
    public List<Account> getAccounts(Sid arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAccount(Sid arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAccount(Account account) {
        for (int i = 0; i < accounts.size(); i++) {
            Account anyaccount = accounts.get(i);
            if (account.getSid().equals(anyaccount.getSid())) {
                accounts.set(i, account);
                return;
            }
        }
    }

}
