package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.rollingupgrades.RollingUpgrade;
import org.restcomm.connect.commons.rollingupgrades.RollingUpgradeState;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.entities.Account;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.restcomm.connect.dao.DaoUtils.readAccountStatus;
import static org.restcomm.connect.dao.DaoUtils.readAccountType;
import static org.restcomm.connect.dao.DaoUtils.readDateTime;
import static org.restcomm.connect.dao.DaoUtils.readSid;
import static org.restcomm.connect.dao.DaoUtils.readString;
import static org.restcomm.connect.dao.DaoUtils.readUri;
import static org.restcomm.connect.dao.DaoUtils.writeAccountStatus;
import static org.restcomm.connect.dao.DaoUtils.writeAccountType;
import static org.restcomm.connect.dao.DaoUtils.writeDateTime;
import static org.restcomm.connect.dao.DaoUtils.writeSid;
import static org.restcomm.connect.dao.DaoUtils.writeUri;

/**
 * Used for the first step of the upgrade
 * DB: NEW version
 * Restcomm nodes: both new and old version Restcomm nodes
 * Created by gvagenas on 29/11/2016.
 */
@RollingUpgrade(state = RollingUpgradeState.ACCOUNT_PASSWORD_000)
public class AccountAccessor_000 implements AccountAccessor {

    @Override
    public Map<String, Object> toMap (Account account) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", writeSid(account.getSid()));
        map.put("date_created", writeDateTime(account.getDateCreated()));
        map.put("date_updated", writeDateTime(account.getDateUpdated()));
        map.put("email_address", account.getEmailAddress());
        map.put("friendly_name", account.getFriendlyName());
        map.put("parent_sid", writeSid(account.getParentSid()));
        map.put("type", writeAccountType(account.getType()));
        map.put("password", account.getPassword());
        map.put("password_algorithm", DaoUtils.writeAccountPasswordAlgorithm(account.getPasswordAlgorithm()));
        map.put("status", writeAccountStatus(account.getStatus()));
        // DBUPGRADE_PATCH: We explicitly set auth_token to md5(password) to simulate older AuthToken semantics (when it wasn't random. That's for ugrading only
        //map.put("auth_token", account.getAuthToken());
        String md5password;
        if (Account.PasswordAlgorithm.plain == account.getPasswordAlgorithm()) {
            md5password = DigestUtils.md5Hex(account.getPassword());
        } else if (Account.PasswordAlgorithm.md5 == account.getPasswordAlgorithm()) {
            md5password = account.getPassword();
        } else {
            throw new IllegalStateException("Account password algorithm was expected to be either md5 or plain but was found " + account.getPasswordAlgorithm());
        }
        map.put("auth_token", md5password);
        map.put("role", account.getRole());
        map.put("uri", writeUri(account.getUri()));
        return map;
    }

    @Override
    public Account toAccount (Map<String, Object> map) {
        final Sid sid = readSid(map.get("sid"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        final String emailAddress = readString(map.get("email_address"));
        final String friendlyName = readString(map.get("friendly_name"));
        final Sid parentSid = readSid(map.get("parent_sid"));
        final Account.Type type = readAccountType(map.get("type"));
        final Account.Status status = readAccountStatus(map.get("status"));
        final String authToken = readString(map.get("auth_token"));
        String password;
        Account.PasswordAlgorithm passwordAlgorithm;
        if (map.containsKey("password")) {
            password = readString(map.get("password"));
            passwordAlgorithm = DaoUtils.readAccountPasswordAlgorithm(map.get("password_algorithm"));
        } else {
            password = authToken;
            passwordAlgorithm = Account.PasswordAlgorithm.md5;
        }
        final String role = readString(map.get("role"));
        final URI uri = readUri(map.get("uri"));
        return new Account(sid, dateCreated, dateUpdated, emailAddress, friendlyName, parentSid, type, status, password, passwordAlgorithm, authToken,
                role, uri);
    }
}
