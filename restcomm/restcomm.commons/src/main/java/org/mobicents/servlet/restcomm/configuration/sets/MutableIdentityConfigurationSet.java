package org.mobicents.servlet.restcomm.configuration.sets;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class MutableIdentityConfigurationSet extends ConfigurationSet {

    public static final String MODE_KEY = "identity.mode";
    public static final String RESTCOMM_CLIENT_SECRET_KEY = "identity.restcomm-client-secret";
    public static final String INSTANCE_ID_KEY = "identity.instance-id";

    private final String mode;
    private final String restcommClientSecret;
    private final String instanceId;

    private static final String MODE_DEFAULT = "init";

    // other static stuff to keep them all in a single place
    private static final String ADMINISTRATOR_ROLE = "Administrator";

    public MutableIdentityConfigurationSet(ConfigurationSource source) {
        this(source,null);
    }

    // copy-constructor-like method
    public MutableIdentityConfigurationSet(ConfigurationSet oldSet) {
        this(oldSet.source,oldSet.updateListeners);
    }

    public MutableIdentityConfigurationSet(ConfigurationSource source, List<ConfigurationUpdateListener> listeners) {
        super(source,listeners);
        // mode option
        String mode = source.getProperty(MODE_KEY);
        if (StringUtils.isEmpty(mode))
            this.mode = MODE_DEFAULT;
        else
        if (validateMode(mode))
            this.mode = mode;
        else
            throw new RuntimeException("Error initializing '" + MODE_KEY + "' configuration setting. Invalid value: " + mode);
        // restcommClientSecret option
        this.restcommClientSecret = source.getProperty(RESTCOMM_CLIENT_SECRET_KEY);
        // instanceId option
        this.instanceId = source.getProperty(INSTANCE_ID_KEY);

        this.reloaded();
    }

    private boolean validateMode(String mode) {
        if (!StringUtils.isEmpty(mode)) {
            if (mode.equals("init") || mode.equals("cloud") || mode.equals("standalone"))
                return true;
        }
        return false;
    }

    public String getMode() {
        return mode;
    }

    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    public String getInstanceId() {
        return instanceId;
    }

    // hardcoded getters

    public Boolean getAutoImportUsers() {
        return true;
    }

    // static getters

    public static String getAdministratorRole() {
        return ADMINISTRATOR_ROLE;
    }

}
