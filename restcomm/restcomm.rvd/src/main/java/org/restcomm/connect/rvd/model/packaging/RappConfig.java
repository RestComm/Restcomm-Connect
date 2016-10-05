package org.restcomm.connect.rvd.model.packaging;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.connect.rvd.validation.ValidatableModel;
import org.restcomm.connect.rvd.validation.ValidationReport;

public class RappConfig extends ValidatableModel {
    public  class ConfigOption {
        public String name;
        public String label;
        public String defaultValue;
        public Boolean required;
        public String description;
        public Boolean isInitOption; // will this option be used for initializing the application ? i.e. sent to the backend scripts. Is missing it should be considered true
    }

    public String howTo;
    public Boolean allowInstanceCreation;
    public String provisioningUrl;
    public String bootstrapUrl;

    public List<ConfigOption> options = new ArrayList<ConfigOption>();

    public RappConfig() {

    }

    @Override
    public ValidationReport validate(ValidationReport report) {
        // TODO Auto-generated method stub
        return null;
    }
}
