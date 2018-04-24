/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.dao.mybatis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ExtensionsRulesDao;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionRules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.restcomm.connect.dao.DaoUtils.readBoolean;
import static org.restcomm.connect.dao.DaoUtils.readDateTime;

/**
 * Created by gvagenas on 11/10/2016.
 */
public class MybatisExtensionsRulesDao implements ExtensionsRulesDao {

    private static Logger logger = Logger.getLogger(MybatisExtensionsRulesDao.class);
    private static final String namespace = "org.restcomm.connect.dao.ExtensionsRulesDao.";
    private final SqlSessionFactory sessions;

    public MybatisExtensionsRulesDao (final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addExtensionRules (ExtensionRules extensionRules) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionRules != null && extensionRules.getConfigurationData() != null) {
                if (validate(extensionRules)) {
                    session.insert(namespace + "addExtensionRules", toMap(extensionRules));
                    session.commit();
                } else {
                    throw new ConfigurationException("Exception trying to add new configuration, validation failed. configuration type: "
                            + extensionRules.getConfigurationType());
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void updateExtensionRules (ExtensionRules extensionRules) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionRules != null && extensionRules.getConfigurationData() != null) {
                if (validate(extensionRules)) {
                    session.update(namespace + "updateExtensionRules", toMap(extensionRules));
                } else {
                    throw new ConfigurationException("Exception trying to update configuration, validation failed. configuration type: "
                            + extensionRules.getConfigurationType());
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionRules getExtensionRulesByName (String extensionName) {
        final SqlSession session = sessions.openSession();
        ExtensionRules extensionRules = null;
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getExtensionRulesByName", extensionName);
            if (result != null) {
                extensionRules = toExtensionConfiguration(result);
            }
            return extensionRules;
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionRules getExtensionRulesBySid (Sid extensionSid) {
        final SqlSession session = sessions.openSession();
        ExtensionRules extensionRules = null;
        try {
            final Map<String, Object> result  = session.selectOne(namespace + "getExtensionRulesBySid", extensionSid.toString());
            if (result != null) {
                extensionRules = toExtensionConfiguration(result);
            }
            return extensionRules;
        } finally {
            session.close();
        }
    }

    @Override
    public List<ExtensionRules> getAllExtensionRules () {
        final SqlSession session = sessions.openSession();
        ExtensionRules extensionRules = null;
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllExtensionRules");
            final List<ExtensionRules> confs = new ArrayList<ExtensionRules>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    confs.add(toExtensionConfiguration(result));
                }
            }
            return confs;
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteExtensionRulesByName (String extensionName) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "deleteExtensionRulesByName", extensionName);
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteExtensionRulesBySid (Sid extensionSid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "deleteExtensionRulesBySid", extensionSid.toString());
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public boolean isLatestVersionByName(String extensionName, DateTime dateTime) {
        final SqlSession session = sessions.openSession();
        boolean result = false;
        int comp;
        try {
            final DateTime dateUpdated = new DateTime(session.selectOne(namespace + "getDateUpdatedByName", extensionName));
            if (dateUpdated != null) {
                comp = DateTimeComparator.getInstance().compare(dateTime, dateUpdated);
                if (comp < 0) {
                    //Negative value means that given dateTime is less than dateUpdated, which means that DB
                    //has a newer cnfiguration
                    result = true;
                }
            }

        } finally {
            session.close();
        }
        return result;
    }

    @Override
    public boolean isLatestVersionBySid(Sid extensionSid, DateTime dateTime) {
        final SqlSession session = sessions.openSession();
        boolean result = false;
        int comp;
        try {
            final DateTime dateUpdated = new DateTime(session.selectOne(namespace + "getDateUpdatedBySid", extensionSid.toString()));
            if (dateUpdated != null) {
                comp = DateTimeComparator.getInstance().compare(dateTime, dateUpdated);
                if (comp < 0) {
                    //Negative value means that given dateTime is less than dateUpdated, which means that DB
                    //has a newer cnfiguration
                    result = true;
                }
            }

        } finally {
            session.close();
        }
        return result;
    }

    @Override
    public boolean validate(ExtensionRules extensionRules) {
        ExtensionRules.configurationType configurationType = extensionRules.getConfigurationType();
        if (configurationType.equals(ExtensionRules.configurationType.JSON)) {
            Gson gson = new Gson();
            try {
                Object o = gson.fromJson((String) extensionRules.getConfigurationData(), Object.class);
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(o);
                return (json != null || !json.isEmpty());
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid json format, exception: "+e);
                }
            } finally {
                gson = null;
            }
        } else if (configurationType.equals(ExtensionRules.configurationType.XML)) {
            Configuration xml = null;
            try {
                XMLConfiguration xmlConfiguration = new XMLConfiguration();
                xmlConfiguration.setDelimiterParsingDisabled(true);
                xmlConfiguration.setAttributeSplittingDisabled(true);
                InputStream is = IOUtils.toInputStream(extensionRules.getConfigurationData().toString());
                xmlConfiguration.load(is);
                xml = xmlConfiguration;
                return (xml != null || !xml.isEmpty());
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid xml document, exception: "+e);
                }
            } finally {
                xml = null;
            }
        }
        return false;
    }

    private ExtensionRules toExtensionConfiguration(final Map<String, Object> map) {
        final Sid sid = new Sid((String)map.get("sid"));
        final String extension = (String) map.get("extension");
        boolean enabled = true;
        if (readBoolean(map.get("enabled")) != null)
            enabled = readBoolean(map.get("enabled"));
        final Object confData = map.get("configuration_data");
        final ExtensionRules.configurationType confType =
                ExtensionRules.configurationType.valueOf((String)map.get("configuration_type"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        return new ExtensionRules(sid, extension, enabled, confData, confType, dateCreated, dateUpdated);
    }

    private ExtensionRules toAccountsExtensionConfiguration(final Map<String, Object> map) {
        final Sid sid = new Sid((String)map.get("extension"));
        final String extension = (String) map.get("extension");
        final Object confData = map.get("configuration_data");
        return new ExtensionRules(sid, extension, true, confData, null, null, null);
    }

    private Map<String, Object> toMap(final ExtensionRules extensionRules) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(extensionRules.getSid()));
        map.put("extension", extensionRules.getExtensionName());

        if (extensionRules.getConfigurationData() != null)
            map.put("configuration_data", extensionRules.getConfigurationData());
        if (extensionRules.getConfigurationType() != null)
            map.put("configuration_type", extensionRules.getConfigurationType().toString());
        if (extensionRules.getDateCreated() != null)
            map.put("date_created", DaoUtils.writeDateTime(extensionRules.getDateCreated()));
        if (extensionRules.getDateUpdated() != null)
            map.put("date_updated", DaoUtils.writeDateTime(extensionRules.getDateUpdated()));

        map.put("enabled", extensionRules.isEnabled());
        return map;
    }

    @Override
    public ExtensionRules getAccountExtensionRules (String accountSid, String extensionSid) {
        final SqlSession session = sessions.openSession();
        ExtensionRules extensionRules = null;
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("account_sid", accountSid.toString());
            params.put("extension_sid", extensionSid.toString());
            final Map<String, Object> result  = session.selectOne(namespace + "getAccountExtensionRules", params);
            if (result != null) {
                extensionRules = toAccountsExtensionConfiguration(result);
            }
            return extensionRules;
        } finally {
            session.close();
        }
    }

    @Override
    public void addAccountExtensionRules (ExtensionRules extensionRules, Sid accountSid) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionRules != null && extensionRules.getConfigurationData() != null) {
                if (validate(extensionRules)) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("account_sid", DaoUtils.writeSid(accountSid));
                    map.put("extension_sid", DaoUtils.writeSid(extensionRules.getSid()));

                    if (extensionRules.getConfigurationData() != null)
                        map.put("configuration_data", extensionRules.getConfigurationData());

                    session.insert(namespace + "addAccountExtensionRules", map);
                    session.commit();
                } else {
                    throw new ConfigurationException("Exception trying to add new configuration, validation failed. configuration type: "
                            + extensionRules.getConfigurationType());
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void updateAccountExtensionRules (ExtensionRules extensionRules, Sid accountSid)
            throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionRules != null && extensionRules.getConfigurationData() != null) {
                if (validate(extensionRules)) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("account_sid", DaoUtils.writeSid(accountSid));
                    map.put("extension_sid", DaoUtils.writeSid(extensionRules.getSid()));

                    if (extensionRules.getConfigurationData() != null)
                        map.put("configuration_data", extensionRules.getConfigurationData());
                    session.update(namespace + "updateAccountExtensionRules", map);
                } else {
                    throw new ConfigurationException("Exception trying to update configuration, validation failed. configuration type: "
                            + extensionRules.getConfigurationType());
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteAccountExtensionRules (String accountSid, String extensionSid) {
        final SqlSession session = sessions.openSession();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("account_sid", accountSid.toString());
            params.put("extension_sid", extensionSid.toString());
            session.delete(namespace + "deleteAccountExtensionRules", params);
            session.commit();
        } finally {
            session.close();
        }
    }
}
