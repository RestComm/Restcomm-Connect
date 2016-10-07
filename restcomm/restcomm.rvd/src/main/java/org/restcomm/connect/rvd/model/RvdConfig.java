package org.restcomm.connect.rvd.model;

public class RvdConfig {
    private String workspaceLocation;
    private String workspaceBackupLocation;
    private String sslMode;
    private String restcommBaseUrl;

    public RvdConfig() {
    }

    public RvdConfig(String workspaceLocation, String workspaceBackupLocation, String restcommPublicIp, String sslMode) {
        super();
        this.workspaceLocation = workspaceLocation;
        this.workspaceBackupLocation = workspaceBackupLocation;
        this.sslMode = sslMode;
    }

    public String getWorkspaceLocation() {
        return workspaceLocation;
    }

    public String getWorkspaceBackupLocation() {
        return workspaceBackupLocation;
    }

    public String getSslMode() {
        return sslMode;
    }

    public String getRestcommBaseUrl() {
        return restcommBaseUrl;
    }
}
