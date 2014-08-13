package org.mobicents.servlet.restcomm.rvd.model.client;

public class SettingsModel {

    private String apiServerHost;
    private Integer apiServerRestPort; // null values should be allowed too

    public SettingsModel(String apiServerHost, Integer apiServerRestPort) {
        super();
        this.apiServerHost = apiServerHost;
        this.apiServerRestPort = apiServerRestPort;
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

}
