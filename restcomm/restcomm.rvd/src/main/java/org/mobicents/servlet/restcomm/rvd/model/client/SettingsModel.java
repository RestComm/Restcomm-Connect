package org.mobicents.servlet.restcomm.rvd.model.client;

public class SettingsModel {

    private String apiServerHost;
    private Integer apiServerRestPort; // null values should be allowed too
    private String apiServerUsername;
    private String apiServerPass;

    public SettingsModel(String apiServerHost, Integer apiServerRestPort) {
        super();
        this.apiServerHost = apiServerHost;
        this.apiServerRestPort = apiServerRestPort;
    }


    public SettingsModel(String apiServerHost, Integer apiServerRestPort, String apiServerUsername, String apiServerPass) {
        super();
        this.apiServerHost = apiServerHost;
        this.apiServerRestPort = apiServerRestPort;
        this.apiServerUsername = apiServerUsername;
        this.apiServerPass = apiServerPass;
    }


    public String getApiServerHost() {
        return apiServerHost;
    }

    public void setApiServerHost(String apiServerHost) {
        this.apiServerHost = apiServerHost;
    }

    public Integer getApiServerRestPort() {
        return apiServerRestPort;
    }

    public void setApiServerRestPort(Integer apiServerRestPort) {
        this.apiServerRestPort = apiServerRestPort;
    }


    public String getApiServerUsername() {
        return apiServerUsername;
    }


    public void setApiServerUsername(String apiServerUsername) {
        this.apiServerUsername = apiServerUsername;
    }


    public String getApiServerPass() {
        return apiServerPass;
    }


    public void setApiServerPass(String apiServerPass) {
        this.apiServerPass = apiServerPass;
    }

}
