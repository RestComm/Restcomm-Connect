package org.mobicents.servlet.restcomm.configuration.sets;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.configuration.ConfigurationUpdateListener;
import org.mobicents.servlet.restcomm.configuration.sources.ConfigurationSource;

public class MutableIdentityConfigurationSetImpl extends ConfigurationSetImpl implements MutableIdentityConfigurationSet {

    private final String mode;
    private final String restcommClientSecret;
    private final String instanceId;

    static final String MODE_DEFAULT = "init";
    static final String ADMINISTRATOR_ROLE = "Administrator";

    public MutableIdentityConfigurationSetImpl(ConfigurationSource source) {
        this(source,null);
    }

    // copy-constructor-like method
    public MutableIdentityConfigurationSetImpl(ConfigurationSet oldSet) {
        this(oldSet.getSource(),oldSet.getUpdateListeners());
    }

    public MutableIdentityConfigurationSetImpl(ConfigurationSource source, List<ConfigurationUpdateListener> listeners) {
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

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    // hardcoded getters

    @Override
    public Boolean getAutoImportUsers() {
        return true;
    }

    // static getters

    public static String getAdministratorRole() {
        return ADMINISTRATOR_ROLE;
    }

}
