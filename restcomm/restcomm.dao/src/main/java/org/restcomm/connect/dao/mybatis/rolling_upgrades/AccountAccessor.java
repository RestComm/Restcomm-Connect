package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.restcomm.connect.dao.entities.Account;

import java.util.Map;

/**
 * Base interface to abstract account translation from/to maps
 *
 * Created by gvagenas on 29/11/2016.
 */
public interface AccountAccessor {
    Map<String, Object> toMap(final Account account);
    Account toAccount(final Map<String, Object> map);
}
