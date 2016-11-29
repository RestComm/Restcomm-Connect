package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.restcomm.connect.dao.entities.Account;

import java.util.Map;

/**
 * Created by gvagenas on 29/11/2016.
 */
public class AccountTransformations_001 implements AccountTransformations {
	@Override
	public Map<String, Object> toMap (Account account) {
		return null;
	}
	
	@Override
	public Account toAccount (Map<String, Object> map) {
		return null;
	}
}
