package org.restcomm.connect.dao.mybatis.rolling_upgrades;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
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
 * Used for the second step of the upgrade
 * DB: NEW version
 * Restcomm nodes: only NEW version Restcomm nodes
 * Created by gvagenas on 29/11/2016.
 */
public class AccountTransformations_001 implements AccountTransformations {
	
	@Override
	public Account toAccount(final Map<String, Object> map) {
		final Sid sid = readSid(map.get("sid"));
		final DateTime dateCreated = readDateTime(map.get("date_created"));
		final DateTime dateUpdated = readDateTime(map.get("date_updated"));
		final String emailAddress = readString(map.get("email_address"));
		final String friendlyName = readString(map.get("friendly_name"));
		final Sid parentSid = readSid(map.get("parent_sid"));
		final Account.Type type = readAccountType(map.get("type"));
		final Account.Status status = readAccountStatus(map.get("status"));
		final String authToken = readString(map.get("auth_token"));
		final String password = readString(map.get("password"));
		Account.PasswordAlgorithm passwordAlgorithm = DaoUtils.readAccountPasswordAlgorithm(map.get("password_algorithm"));
		final String role = readString(map.get("role"));
		final URI uri = readUri(map.get("uri"));
		return new Account(sid, dateCreated, dateUpdated, emailAddress, friendlyName, parentSid, type, status, password, passwordAlgorithm, authToken,
				role, uri);
	}
	
	@Override
	public Map<String, Object> toMap(final Account account) {
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
		map.put("auth_token", account.getAuthToken());
		map.put("role", account.getRole());
		map.put("uri", writeUri(account.getUri()));
		return map;
	}
}
