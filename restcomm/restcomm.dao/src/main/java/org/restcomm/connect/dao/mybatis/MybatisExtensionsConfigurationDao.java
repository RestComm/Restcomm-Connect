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
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
import org.restcomm.connect.extension.api.ExtensionConfiguration.configurationType;

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
public class MybatisExtensionsConfigurationDao implements ExtensionsConfigurationDao {

    private static Logger logger = Logger.getLogger(MybatisExtensionsConfigurationDao.class);
    private static final String namespace = "org.restcomm.connect.dao.ExtensionsConfigurationDao.";
    private final SqlSessionFactory sessions;

    public MybatisExtensionsConfigurationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addConfiguration(ExtensionConfiguration extensionConfiguration) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    session.insert(namespace + "addConfiguration", toMap(extensionConfiguration));
                    session.commit();
                } else {
                    throw new ConfigurationException("Exception trying to add new configuration, validation failed. configuration type: "
                            + extensionConfiguration.getConfigurationType());
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void updateConfiguration(ExtensionConfiguration extensionConfiguration) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    session.update(namespace + "updateConfiguration", toMap(extensionConfiguration));
                } else {
                    throw new ConfigurationException("Exception trying to update configuration, validation failed. configuration type: "
                            + extensionConfiguration.getConfigurationType());
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionConfiguration getConfigurationByName(String extensionName) {
        final SqlSession session = sessions.openSession();
        ExtensionConfiguration extensionConfiguration = null;
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getConfigurationByName", extensionName);
            if (result != null) {
                extensionConfiguration = toExtensionConfiguration(result);
            }
            return extensionConfiguration;
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionConfiguration getConfigurationBySid(Sid extensionSid) {
        final SqlSession session = sessions.openSession();
        ExtensionConfiguration extensionConfiguration = null;
        try {
            final Map<String, Object> result  = session.selectOne(namespace + "getConfigurationBySid", extensionSid.toString());
            if (result != null) {
                extensionConfiguration = toExtensionConfiguration(result);
            }
            return extensionConfiguration;
        } finally {
            session.close();
        }
    }

    @Override
    public List<ExtensionConfiguration> getAllConfiguration() {
        final SqlSession session = sessions.openSession();
        ExtensionConfiguration extensionConfiguration = null;
        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllConfiguration");
            final List<ExtensionConfiguration> confs = new ArrayList<ExtensionConfiguration>();
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
    public List<ExtensionConfiguration> getAllConfigurationByType(ExtensionConfiguration.configurationType type) {
        final SqlSession session = sessions.openSession();

        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getAllConfigurationByType", type.name());
            final List<ExtensionConfiguration> confs = new ArrayList<ExtensionConfiguration>();
            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    confs.add(toExtensionConfiguration(result));
                }
            }
            return confs;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteConfigurationByName(String extensionName) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "deleteConfigurationByName", extensionName);
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteConfigurationBySid(Sid extensionSid) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "deleteConfigurationBySid", extensionSid.toString());
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
    public boolean validate(ExtensionConfiguration extensionConfiguration) {
        ExtensionConfiguration.configurationType configurationType = extensionConfiguration.getConfigurationType();
        if (configurationType.equals(ExtensionConfiguration.configurationType.JSON)) {
            Gson gson = new Gson();
            try {
                Object o = gson.fromJson((String) extensionConfiguration.getConfigurationData(), Object.class);
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(o);
                return (json != null || !json.isEmpty());
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid json format, exception: "+e);
                }
            } finally {
                gson = null;
            }
        } else if (configurationType.equals(ExtensionConfiguration.configurationType.XML)) {
            Configuration xml = null;
            try {
                XMLConfiguration xmlConfiguration = new XMLConfiguration();
                xmlConfiguration.setDelimiterParsingDisabled(true);
                xmlConfiguration.setAttributeSplittingDisabled(true);
                InputStream is = IOUtils.toInputStream(extensionConfiguration.getConfigurationData().toString());
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

    private ExtensionConfiguration toExtensionConfiguration(final Map<String, Object> map) {
        final Sid sid = new Sid((String)map.get("sid"));
        final String extension = (String) map.get("extension");
        boolean enabled = true;
        if (readBoolean(map.get("enabled")) != null)
            enabled = readBoolean(map.get("enabled"));
        final Object confData = map.get("configuration_data");
        final ExtensionConfiguration.configurationType confType =
                ExtensionConfiguration.configurationType.valueOf((String)map.get("configuration_type"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        return new ExtensionConfiguration(sid, extension, enabled, confData, confType, dateCreated, dateUpdated);
    }

    private ExtensionConfiguration toAccountsExtensionConfiguration(final Map<String, Object> map) {
        final Sid sid = new Sid((String)map.get("extension"));
        final String extension = (String) map.get("extension");
        final Object confData = map.get("configuration_data");
        return new ExtensionConfiguration(sid, extension, true, confData, configurationType.JSON, null, null);
    }

    private Map<String, Object> toMap(final ExtensionConfiguration extensionConfiguration) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sid", DaoUtils.writeSid(extensionConfiguration.getSid()));
        map.put("extension", extensionConfiguration.getExtensionName());

        if (extensionConfiguration.getConfigurationData() != null)
            map.put("configuration_data", extensionConfiguration.getConfigurationData());
        if (extensionConfiguration.getConfigurationType() != null)
            map.put("configuration_type", extensionConfiguration.getConfigurationType().toString());
        if (extensionConfiguration.getDateCreated() != null)
            map.put("date_created", DaoUtils.writeDateTime(extensionConfiguration.getDateCreated()));
        if (extensionConfiguration.getDateUpdated() != null)
            map.put("date_updated", DaoUtils.writeDateTime(extensionConfiguration.getDateUpdated()));

        map.put("enabled", extensionConfiguration.isEnabled());
        return map;
    }

    @Override
    public ExtensionConfiguration getAccountExtensionConfiguration(String accountSid, String extensionSid) {
        final SqlSession session = sessions.openSession();
        ExtensionConfiguration extensionConfiguration = null;
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("account_sid", accountSid.toString());
            params.put("extension_sid", extensionSid.toString());
            final Map<String, Object> result  = session.selectOne(namespace + "getAccountExtensionConfiguration", params);
            if (result != null) {
                extensionConfiguration = toAccountsExtensionConfiguration(result);
            }
            return extensionConfiguration;
        } finally {
            session.close();
        }
    }

    @Override
    public void addAccountExtensionConfiguration(ExtensionConfiguration extensionConfiguration, Sid accountSid) throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("account_sid", DaoUtils.writeSid(accountSid));
                    map.put("extension_sid", DaoUtils.writeSid(extensionConfiguration.getSid()));

                    if (extensionConfiguration.getConfigurationData() != null)
                        map.put("configuration_data", extensionConfiguration.getConfigurationData());

                    session.insert(namespace + "addAccountExtensionConfiguration", map);
                    session.commit();
                } else {
                    throw new ConfigurationException("Exception trying to add new configuration, validation failed. configuration type: "
                            + extensionConfiguration.getConfigurationType());
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void updateAccountExtensionConfiguration(ExtensionConfiguration extensionConfiguration, Sid accountSid)
            throws ConfigurationException {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("account_sid", DaoUtils.writeSid(accountSid));
                    map.put("extension_sid", DaoUtils.writeSid(extensionConfiguration.getSid()));

                    if (extensionConfiguration.getConfigurationData() != null)
                        map.put("configuration_data", extensionConfiguration.getConfigurationData());
                    session.update(namespace + "updateAccountExtensionConfiguration", map);
                } else {
                    throw new ConfigurationException("Exception trying to update configuration, validation failed. configuration type: "
                            + extensionConfiguration.getConfigurationType());
                }
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void deleteAccountExtensionConfiguration(String accountSid, String extensionSid) {
        final SqlSession session = sessions.openSession();
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("account_sid", accountSid.toString());
            params.put("extension_sid", extensionSid.toString());
            session.delete(namespace + "deleteAccountExtensionConfiguration", params);
            session.commit();
        } finally {
            session.close();
        }
    }
}
