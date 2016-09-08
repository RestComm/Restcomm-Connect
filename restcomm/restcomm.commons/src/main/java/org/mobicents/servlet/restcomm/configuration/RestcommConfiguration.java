/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.configuration.sets.impl.ConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.impl.MainConfigurationSetImpl;
import org.mobicents.servlet.restcomm.configuration.sources.ApacheConfigurationSource;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;

/**
 * Singleton like class that provides access to ConfigurationSets.
 * Use get+() functions to access configuration sets.
 *
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 *
 */
public class RestcommConfiguration {

    private final Map<String,ConfigurationSet> sets = new ConcurrentHashMap<String,ConfigurationSet>();

    public RestcommConfiguration() {
        // No ConfigurationSets added. You'll have to it manually with addConfigurationSet().
    }

    public RestcommConfiguration(Configuration apacheConf) {
        // addConfigurationSet("main", new MainConfigurationSet( new ApacheConfigurationSource(apacheConf)));
        ApacheConfigurationSource apacheCfgSrc = new ApacheConfigurationSource(apacheConf);

        addConfigurationSet("main", new MainConfigurationSetImpl(apacheCfgSrc));
        // addConfigurationSet("identity", new IdentityConfigurationSet( new DbConfigurationSource(dbConf)));
        // ...
    }

    public void addConfigurationSet(String setKey, ConfigurationSet set ) {
        sets.put(setKey, set);
    }
    public <T extends ConfigurationSet> T get(String key, Class <T> type) {
        return type.cast(sets.get(key));
    }

    public MainConfigurationSet getMain() {
        return (MainConfigurationSet) sets.get("main");
    }
    /*
    public void reloadMain() {
        MainConfigurationSet oldMain = getMain();
        MainConfigurationSet newMain = new MainConfigurationSet(oldMain.getSource());
        sets.put("main", newMain);
    }
    */

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
