package org.mobicents.servlet.restcomm.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurators are high level wrappers of pieces of restcomm configuration. They can provide
 * validation, default values of high level configuration processings.
 * Implement this to make your own by defining getters/setters of individual
 * pieces of information.

 * @author "Tsakiridis Orestis"
 *
 **/
 public abstract class ConfiguratorBase implements Configurator {

     protected List<ConfigurationUpdateListener> updateListeners = new ArrayList<ConfigurationUpdateListener>();

     @Override
     public void registerUpdateListener(ConfigurationUpdateListener listener) {
         updateListeners.add(listener);
     }

     protected void notifyUpdateListeners() {
         for (ConfigurationUpdateListener listener : updateListeners) {
             listener.configurationUpdated(this);
         }
     }

}
