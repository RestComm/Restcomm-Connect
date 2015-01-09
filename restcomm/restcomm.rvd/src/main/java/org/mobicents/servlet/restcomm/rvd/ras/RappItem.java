package org.mobicents.servlet.restcomm.rvd.ras;

import org.mobicents.servlet.restcomm.rvd.packaging.model.RappInfo;

/**
 * A dto model to be used for populating app list in the UI
 * @author "Tsakiridis Orestis"
 *
 */
public class RappItem {
    public enum RappStatus {
        Installed, Configured, Unconfigured, Active, Inactive
    }

    RappInfo rappInfo;
    RappStatus[] status;
    String projectName;
    Boolean wasImported;
    Boolean hasPackaging;
    Boolean hasBootstrap;
    public RappInfo getRappInfo() {
        return rappInfo;
    }
    public void setRappInfo(RappInfo rappInfo) {
        this.rappInfo = rappInfo;
    }
    public RappStatus[] getStatus() {
        return status;
    }
    public void setStatus(RappStatus[] status) {
        this.status = status;
    }
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public Boolean getWasImported() {
        return wasImported;
    }
    public void setWasImported(Boolean wasImported) {
        this.wasImported = wasImported;
    }
    public Boolean getHasPackaging() {
        return hasPackaging;
    }
    public void setHasPackaging(Boolean hasPackaging) {
        this.hasPackaging = hasPackaging;
    }
    public Boolean getHasBootstrap() {
        return hasBootstrap;
    }
    public void setHasBootstrap(Boolean hasBootstrap) {
        this.hasBootstrap = hasBootstrap;
    }
}
