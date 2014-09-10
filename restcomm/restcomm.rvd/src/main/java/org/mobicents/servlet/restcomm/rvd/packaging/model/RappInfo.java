package org.mobicents.servlet.restcomm.rvd.packaging.model;

import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.validation.ValidatableModel;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

/**
 * General information about the app. It is usually directly loaded or stored from info.xml file
 * @author "Tsakiridis Orestis"
 *
 */
public class RappInfo extends ValidatableModel {

    private String name;
    private String description;
    private String appVersion;
    private String rvdAppVersion = RvdConfiguration.getRvdProjectVersion();
    private String id;

    public RappInfo() {
        // TODO Auto-generated constructor stub
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getRvdAppVersion() {
        return rvdAppVersion;
    }

    public void setRvdAppVersion(String rvdAppVersion) {
        this.rvdAppVersion = rvdAppVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ValidationReport validate(ValidationReport report) {
        // TODO Auto-generated method stub
        return null;
    }


}
