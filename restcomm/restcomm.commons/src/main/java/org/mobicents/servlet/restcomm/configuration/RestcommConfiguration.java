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


import org.mobicents.servlet.restcomm.configuration.sets.ConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MainConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSetImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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

    /*
    public RestcommConfiguration(Configuration apacheConf) {
        ApacheConfigurationSource apacheSource = new ApacheConfigurationSource(apacheConf);
        addConfigurationSet("main", new MainConfigurationSet(apacheSource));
        addConfigurationSet("identityMigration", new IdentityConfigurationSet(apacheSource));
        // addConfigurationSet("identity", new MutableIdentityConfigurationSet( new DbConfigurationSource(dbConf)));
        // ...
    }
    */

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

    public IdentityConfigurationSet getIdentity() {
        return (IdentityConfigurationSet) sets.get("identity");
    }

    public MutableIdentityConfigurationSet getMutableIdentity() {
        return (MutableIdentityConfigurationSet) sets.get("mutable-identity");
    }

    public MutableIdentityConfigurationSet reloadMutableIdentity() {
        MutableIdentityConfigurationSet oldSet = getMutableIdentity();
        MutableIdentityConfigurationSet newSet = new MutableIdentityConfigurationSetImpl(oldSet);
        sets.put("mutable-identity", newSet);
        return newSet;
    }

    // define getters  for additional ConfigurationSets here
    // ...

    // singleton stuff
    private static RestcommConfiguration instance;
    public static RestcommConfiguration createOnce() {
        synchronized (RestcommConfiguration.class) {
            if (instance == null) {
                instance = new RestcommConfiguration();
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
