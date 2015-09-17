package org.mobicents.servlet.restcomm.identity;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * Represents authorization information for an Account API Key i.e. SID, key, roles etc.
 * @author "Tsakiridis Orestis"
 *
 */
public class AccountKey {

    private Sid sid;
    private String key;
    private Set<String> roles = new HashSet<String>();
    private Account account;
    private boolean verified = false;


    public AccountKey(String sid, String key, AccountsDao dao) {
        super();
        this.sid = new Sid(sid);
        this.key = key;
        verify(dao);
    }

    private void verify(AccountsDao dao) {
        account = dao.getAccount(sid);
        if ( account != null ) {
            if ( key != null && key.equals(account.getAuthToken()) ) {
                verified = true;
                String role = account.getRole();
                if ( ! StringUtils.isEmpty(role) )
                    roles.add(role);
            }
        }
    }

    public Sid getSid() {
        return sid;
    }

    public String getKey() {
        return key;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Account getAccount() {
        return account;
    }

    public boolean isVerified() {
        return verified;
    }



}
