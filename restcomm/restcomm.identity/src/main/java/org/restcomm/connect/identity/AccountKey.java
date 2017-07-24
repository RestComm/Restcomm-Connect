package org.restcomm.connect.identity;

import org.mindrot.jbcrypt.BCrypt;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.entities.Account;

/**
 * Represents authorization information for an Account. When a request initially arrives carrying basic HTTP auth
 * credentials an AccountKey is created. It carries the challenged credentials and the verification result.
 *
 * - use isVerified() to check the verification result.
 *  -use getAccount() to check if the account in the credentials actually exists (may not be verified)
 *
 * @author "Tsakiridis Orestis"
 */
public class AccountKey {

    private String challengedSid;
    private String challengedKey;
    private Account account;    // Having this set does not mean it is verified. It just means that the (account) challengedSid exists.
    private boolean verified = false;


    public AccountKey(String sid, String key, AccountsDao dao) {
        this.challengedSid = sid; // store there for future reference, maybe we need the raw data
        this.challengedKey = key;
        account = dao.getAccountToAuthenticate(sid); // We don't just retrieve an account, we're authenticating. Friendly names as authentnication tokens should be prevented
        verify(dao);
    }

    private void verify(AccountsDao dao) {
        if ( account != null ) {
            if ( challengedKey != null )
                // Compare both the plaintext version of the token and md5'ed version of it
                if ( challengedKey.equals(account.getPassword()) || BCrypt.checkpw(challengedKey, account.getPassword())) {
                    verified = true;
                }
        }
    }

    public Account getAccount() {
        return account;
    }

    public boolean isVerified() {
        return verified;
    }

}
