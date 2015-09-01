package org.mobicents.servlet.restcomm.identity.configuration;

public interface IdentityConfigurationSource {
    String loadMode();
    String loadInstanceId();
    String loadRestcommClientSecret();
    String loadAuthServerUrlBase();
    void saveMode(String mode);
    void saveInstanceId(String instanceId);
    void saveRestcommClientSecret(String secret);
    void saveAuthServerUrlBase(String urlBase);
}
