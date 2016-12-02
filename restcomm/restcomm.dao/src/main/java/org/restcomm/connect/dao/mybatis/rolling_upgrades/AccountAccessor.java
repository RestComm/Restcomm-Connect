package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.restcomm.connect.commons.rollingupgrades.RollingUpgradeStage;
import org.restcomm.connect.dao.entities.Account;

import java.util.Map;

/**
 * AccountAccessor is a rolling feature that translates accounts from/to maps in a different way
 * depending on the upgrage stage we're in.
 *
 * Created by gvagenas on 29/11/2016.
 */
public interface AccountAccessor extends RollingUpgradeStage {
    Map<String, Object> toMap(final Account account);
    Account toAccount(final Map<String, Object> map);
}
