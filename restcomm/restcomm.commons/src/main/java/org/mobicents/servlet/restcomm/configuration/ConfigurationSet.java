package org.mobicents.servlet.restcomm.configuration;

/**
 * Represents a group of configuration options with its own persistence layer.
 * Extend to define loaders and savers for the specific options you need.
 *
 * See IdentityConfigurationSet for more.
 *
 * @author "Tsakiridis Orestis"
 *
 */
public interface ConfigurationSet {
    void save();
    void load();
}
