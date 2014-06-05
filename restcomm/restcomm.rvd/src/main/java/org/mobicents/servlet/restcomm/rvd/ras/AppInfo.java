package org.mobicents.servlet.restcomm.rvd.ras;

/**
 * General information about the app. It is usually directly loaded or stored from info.xml file
 * @author "Tsakiridis Orestis"
 *
 */
public class AppInfo {

    private String name;
    private String description;
    private String appVersion;
    private String rvdAppVersion;

    public AppInfo() {
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


}
