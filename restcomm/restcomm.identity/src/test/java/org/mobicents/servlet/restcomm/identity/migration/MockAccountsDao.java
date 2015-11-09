package org.mobicents.servlet.restcomm.identity.migration;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

public class MockAccountsDao implements AccountsDao {

    public List<Account> accounts;
    
    public MockAccountsDao() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void addAccount(Account arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account getAccount(Sid arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account getAccount(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account getAccountByEmail(String arg0) {
        throw new UnsupportedOperationException();
    }



    @Override
    public List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(buildTestAccount(null, "account1@company.com", "Account1", "account1", null));
        accounts.add(buildTestAccount(null, "account2@company.com", "Account2", "account2", null));
        accounts.add(buildTestAccount(null, "account3@company.com", "Account3", "account3", null));

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
    public void updateAccount(Account arg0) {
        throw new UnsupportedOperationException();
    }

}
