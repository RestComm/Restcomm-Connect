package org.mobicents.servlet.restcomm.rvd.packaging.model;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.validation.ValidatableModel;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

public class RappConfig extends ValidatableModel {
    public  class ConfigOption {
        public String name;
        public String label;
        public String defaultValue;
        public Boolean required;
        public String description;
    }

    public String howTo;
    public Boolean allowInstanceCreation;
    public String configurationUrl;

    public List<ConfigOption> options = new ArrayList<ConfigOption>();

    public RappConfig() {

    }

    @Override
    public ValidationReport validate(ValidationReport report) {
        // TODO Auto-generated method stub
        return null;
    }
}
