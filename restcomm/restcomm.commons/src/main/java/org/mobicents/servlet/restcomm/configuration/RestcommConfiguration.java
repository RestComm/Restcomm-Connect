package org.mobicents.servlet.restcomm.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.configuration.sets.ConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sources.ApacheConfigurationSource;

public class RestcommConfiguration {

    private final Map<String,ConfigurationSet> sets = new ConcurrentHashMap<String,ConfigurationSet>();

    public RestcommConfiguration() {
        // No ConfigurationSets added. You'll have to it manually with addConfigurationSet().
    }

    public RestcommConfiguration(Configuration apacheConf) {
        addConfigurationSet("main", new MainConfigurationSet( new ApacheConfigurationSource(apacheConf)));
        // addConfigurationSet("identity", new IdentityConfigurationSet( new DbConfigurationSource(dbConf)));
        // ...
    }

    public void addConfigurationSet(String setKey, ConfigurationSet set ) {
        sets.put(setKey, set);
    }
    public <T extends ConfigurationSet> T get(String key, Class <T> type) {
        return type.cast(sets.get("key"));
    }

    public MainConfigurationSet getMain() {
        return (MainConfigurationSet) sets.get("main");
    }
    public void reloadMain() {
        MainConfigurationSet oldMain = getMain();
        MainConfigurationSet newMain = new MainConfigurationSet(oldMain.getSource());
        sets.put("main", newMain);
    }

    // define getters  for additional ConfigurationSets here
    // ...

    // singleton stuff
    private static RestcommConfiguration instance;
    public static RestcommConfiguration createOnce(Configuration apacheConf) {
        synchronized (RestcommConfiguration.class) {
            if (instance == null) {
                instance = new RestcommConfiguration(apacheConf);
            }
        }
        return instance;
    }
    public static RestcommConfiguration getInstance() {
        if (instance == null)
            throw new IllegalStateException("RestcommConfiguration has not been initialized.");
        return instance;
    }

}
