package org.mobicents.servlet.restcomm.dao.mybatis;

import static org.mobicents.servlet.restcomm.dao.DaoUtils.readString;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mobicents.servlet.restcomm.dao.ConfigurationDao;
import org.mobicents.servlet.restcomm.entities.ConfigurationEntry;

// @ThreadSafe
// Not really threadsafe. setValue() may throw exception if multiple creates are tried at the same time for the same (missing) copnfiguration key.
public class MyBatisConfigurationDao implements ConfigurationDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ConfigurationDao.";
    private final SqlSessionFactory sessions;

    public MyBatisConfigurationDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    /**
     * Updates a record having key = $key with the specified value. If $value is null the record is removed altogether. If there is no such record, nothing is done.
     */
    public void setValue(String key, String value) {
        Map<String,Object> params = toMap(key, value);
        if (value == null)
            dropValue(key);
        // TODO (?) The following operations are not atomic but should be. On the other hand configuration updates are not very common it's it may be an overkill to start locking...
        ConfigurationEntry existingEntry = getConfigurationEntry(key);
        if (existingEntry == null)
            createValue(params);
        else
            updateValue(params);
    }

    public String getValue(String key) {
        ConfigurationEntry entry = getConfigurationEntry(key);
        if ( entry != null )
            return entry.getValue();
        return null;
    }

    private void dropValue(String key) {
        final SqlSession session = sessions.openSession();
        try {
            session.delete(namespace + "dropValue", key);
            session.commit();
        } finally {
            session.close();
        }
    }

    private ConfigurationEntry getConfigurationEntry(String key) {
        final SqlSession session = sessions.openSession();
        try {
            final Map<String, Object> result = session.selectOne(namespace + "getValue", key);
            if (result != null) {
                return toConfigurationEntry(result);
            } else {
                return null;
            }
        } finally {
            session.close();
        }
    }

    public ConfigurationEntry toConfigurationEntry(Map<String, Object> map) {
        final String key = readString(map.get("key"));
        final String value = readString(map.get("value"));
        return new ConfigurationEntry(key, value);
    }

    private void createValue(Map<String, Object> params) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "createValue", params);
            session.commit();
        } finally {
            session.close();
        }
    }

    private void updateValue(Map<String, Object> params) {
        final SqlSession session = sessions.openSession();
        try {
            session.insert(namespace + "updateValue", params);
            session.commit();
        } finally {
            session.close();
        }
    }

    private Map<String, Object> toMap(String key, String value) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", key);
        map.put("value", value);
        return map;
    }

}
