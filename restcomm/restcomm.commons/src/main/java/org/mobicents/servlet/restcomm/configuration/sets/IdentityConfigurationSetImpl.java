package org.mobicents.servlet.restcomm.configuration.sets;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class IdentityConfigurationSetImpl extends ConfigurationSetImpl implements IdentityConfigurationSet {
    private static Logger logger = Logger.getLogger(IdentityConfigurationSetImpl.class);

    // identity connectivity keys
    public static final String AUTH_SERVER_URL_KEY = "identity.auth-server-url";
    public static final String REALM_KEY = "identity.realm";
    public static final String REALM_KEY_KEY = "identity.realm-public-key";

    // identity connectivity variables
    private String authServerUrl;
    private String realm;
    private String realmkey;

    // other static stuff to keep them all in a single place
    public static final String ADMINISTRATOR_ROLE = "Administrator";

    public IdentityConfigurationSetImpl(ConfigurationSource source) {
        super(source,null);
        // authServerBaseUrl option
        this.authServerUrl = source.getProperty(AUTH_SERVER_URL_KEY);
        this.realm = source.getProperty(REALM_KEY);
        // realmKey option
        this.realmkey = source.getProperty(REALM_KEY_KEY);

        this.reloaded();
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public String getRealmkey() {
        return realmkey;
    }

}
