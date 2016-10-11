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

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.extension.api.ExtensionConfigurationProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.restcomm.connect.dao.DaoUtils.readDateTime;

/**
 * Created by gvagenas on 11/10/2016.
 */
public class MybatisExtensionsConfigurationDao implements ExtensionsConfigurationDao {

    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ExtensionsConfigurationDao.";
    private final SqlSessionFactory sessions;

    public MybatisExtensionsConfigurationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public void addConfigurationProperty(final ExtensionConfigurationProperty extensionConfigurationProperty) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "addProperty", toMap(extensionConfigurationProperty));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public void updateConfigurationProperty(ExtensionConfigurationProperty extensionConfigurationProperty) {
        final SqlSession session = sessions.openSession();
        try {
            session.update(namespace + "updateProperty", toMap(extensionConfigurationProperty));
            session.commit();
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionConfigurationProperty getConfigurationProperty(String extension, String property) {
        final SqlSession session = sessions.openSession();
        try {
            ExtensionConfigurationProperty extensionConfigurationProperty = new ExtensionConfigurationProperty(extension, property);
            final Map<String, Object> result = session.selectOne(namespace + "getProperty", toMap(extensionConfigurationProperty));

            if (result != null) {
                return toExtensionConfigurationProperty(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public ExtensionConfigurationProperty getConfigurationPropertyByExtraParameter(String extension, String property, String extraParameter) {
        final SqlSession session = sessions.openSession();
        try {
            ExtensionConfigurationProperty extensionConfigurationProperty = new ExtensionConfigurationProperty(extension, property, extraParameter);
            final Map<String, Object> result = session.selectOne(namespace + "getPropertyByExtraParameter", toMap(extensionConfigurationProperty));

            if (result != null) {
                return toExtensionConfigurationProperty(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    @Override
    public List<ExtensionConfigurationProperty> getConfigurationByExtension(String extension) {
        final SqlSession session = sessions.openSession();

        try {
            final List<Map<String, Object>> results = session.selectList(namespace + "getConfigurationByExtension", extension);
            final List<ExtensionConfigurationProperty> properties = new ArrayList<ExtensionConfigurationProperty>();

            if (results != null && !results.isEmpty()) {
                for (final Map<String, Object> result : results) {
                    properties.add(toExtensionConfigurationProperty(result));
                }
            }
            return properties;
        } finally {
            session.close();
        }
    }

    private ExtensionConfigurationProperty toExtensionConfigurationProperty(final Map<String, Object> map) {
        final String extension = (String) map.get("extension");
        final String property = (String) map.get("property");
        final String extra_parameter = (String) map.get("extra_parameter");
        final String propertyValue = (String) map.get("getPropertyValue");
        final DateTime dateCreated = readDateTime(map.get("date_created"));
        final DateTime dateUpdated = readDateTime(map.get("date_updated"));
        return new ExtensionConfigurationProperty(extension, property, extra_parameter, propertyValue, dateCreated, dateUpdated);
    }

    private Map<String, Object> toMap(final ExtensionConfigurationProperty extensionConfigurationProperty) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("extension", extensionConfigurationProperty.getExtension());

        if (extensionConfigurationProperty.getProperty() != null)
            map.put("property", extensionConfigurationProperty.getProperty());
        if (extensionConfigurationProperty.getExtraParameter() != null)
            map.put("extra_parameter", extensionConfigurationProperty.getExtraParameter());
        if (extensionConfigurationProperty.getPropertyValue() != null)
            map.put("property_value", extensionConfigurationProperty.getPropertyValue());
        if (extensionConfigurationProperty.getDateCreated() != null)
            map.put("date_created", DaoUtils.writeDateTime(extensionConfigurationProperty.getDateCreated()));
        if (extensionConfigurationProperty.getDateUpdated() != null)
            map.put("date_updated", DaoUtils.writeDateTime(extensionConfigurationProperty.getDateUpdated()));
        return map;
    }
}
