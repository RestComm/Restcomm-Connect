package org.mobicents.servlet.restcomm.rvd.packaging.model;

import java.util.ArrayList;
import java.util.List;

public class RappConfig {
    public  class ConfigOption {
        public String name;
        public String label;
        public String defaultValue;
        public Boolean required;
        public String description;
    }

    public List<ConfigOption> options = new ArrayList<ConfigOption>();

    public RappConfig() {

    }
}
