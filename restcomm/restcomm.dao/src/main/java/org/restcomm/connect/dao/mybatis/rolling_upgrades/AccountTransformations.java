package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.restcomm.connect.dao.entities.Account;

import java.util.Map;

/**
 * Created by gvagenas on 29/11/2016.
 */
public interface AccountTransformations {
	Map<String, Object> toMap(final Account account);
	Account toAccount(final Map<String, Object> map);
}
