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
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.extension.api.ExtensionConfiguration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.restcomm.connect.dao.DaoUtils.readDateTime;

/**
 * Created by gvagenas on 11/10/2016.
 */
public class MybatisExtensionsConfigurationDao implements ExtensionsConfigurationDao {

    private static final String namespace = "org.restcomm.connect.dao.ExtensionsConfigurationDao.";
    private final SqlSessionFactory sessions;

    public MybatisExtensionsConfigurationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addConfiguration(ExtensionConfiguration extensionConfiguration) {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    session.insert(namespace + "addConfiguration", toMap(extensionConfiguration));
                    session.commit();
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void updateConfiguration(ExtensionConfiguration extensionConfiguration) {
        final SqlSession session = sessions.openSession();
        try {
            if (extensionConfiguration != null && extensionConfiguration.getConfigurationData() != null) {
                if (validate(extensionConfiguration)) {
                    session.update(namespace + "updateConfiguration", toMap(extensionConfiguration));
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
    public boolean validate(ExtensionConfiguration extensionConfiguration) {
        ExtensionConfiguration.configurationType configurationType = extensionConfiguration.getConfigurationType();
        if (configurationType.equals(ExtensionConfiguration.configurationType.JSON)) {
            Gson gson = new Gson();
            try {
                Object o = gson.fromJson((String) extensionConfiguration.getConfigurationData(), Object.class);
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(o);
                return (json != null || !json.isEmpty());
            } catch (Exception e) {
                System.out.println("invalid json format, exception: "+e);
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
                System.out.println("invalid xml document, exception: "+e);
            } finally {
                xml = null;
            }
        }
        return false;
    }

    private ExtensionConfiguration toExtensionConfiguration(final Map<String, Object> map) {
        final Sid sid = new Sid((String)map.get("sid"));
        final String extension = (String) map.get("extension");
        final Object confData = map.get("configuration_data");
        final ExtensionConfiguration.configurationType confType =
                ExtensionConfiguration.configurationType.valueOf((String)map.get("configuration_type"));
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        return new ExtensionConfiguration(sid, extension, confData, confType, dateCreated, dateUpdated);
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
        return map;
    }
}
