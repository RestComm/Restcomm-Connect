package org.mobicents.servlet.restcomm.configuration;

import org.mobicents.servlet.restcomm.configuration.sources.MutableConfigurationSource;
import org.mobicents.servlet.restcomm.dao.ConfigurationDao;

public class DatabaseConfigurationSource implements MutableConfigurationSource {

    private ConfigurationDao configurationDao;

    public DatabaseConfigurationSource(ConfigurationDao configDao) {
        this.configurationDao = configDao;
    }

    @Override
    public String getProperty(String key) {
        return configurationDao.getValue(key);
    }

    @Override
    public void setProperty(String key, String value) {
        configurationDao.setValue(key, value);
    }

}
