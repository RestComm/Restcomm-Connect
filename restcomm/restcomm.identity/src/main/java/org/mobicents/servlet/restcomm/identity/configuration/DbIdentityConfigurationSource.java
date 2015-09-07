package org.mobicents.servlet.restcomm.identity.configuration;

import org.mobicents.servlet.restcomm.dao.ConfigurationDao;

public class DbIdentityConfigurationSource implements IdentityConfigurationSource {

    ConfigurationDao dao;

    public DbIdentityConfigurationSource(ConfigurationDao dao) {
        this.dao = dao;
    }

    @Override
    public String loadMode() {
        return dao.getValue("identity.mode");
    }

    @Override
    public String loadInstanceId() {
        return dao.getValue("identity.instance-id");
    }

    @Override
    public String loadRestcommClientSecret() {
        return dao.getValue("identity.restcomm-client-secret");
    }

    @Override
    public String loadAuthServerUrlBase() {
        return dao.getValue("identity.auth-server-url-base");
    }

    @Override
    public void saveMode(String mode) {
        dao.setValue("identity.mode", mode);

    }

    @Override
    public void saveInstanceId(String instanceId) {
        dao.setValue("identity.instance-id", instanceId);

    }

    @Override
    public void saveRestcommClientSecret(String secret) {
        dao.setValue("identity.restcomm-client-secret", secret);
    }

    @Override
    public void saveAuthServerUrlBase(String urlBase) {
        dao.setValue("identity.auth-server-url-base", urlBase);
    }

}
