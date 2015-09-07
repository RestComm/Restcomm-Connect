package org.mobicents.servlet.restcomm.dao;

public interface ConfigurationDao {
    String getValue(String key);
    void setValue(String key, String value);
}
