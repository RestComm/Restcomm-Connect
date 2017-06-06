package org.restcomm.connect.extension.api;

public interface IExtensionRequest {
    String getAccountSid();
    boolean isAllowed();
    void setAllowed(boolean allowed);
    //boolean isFromApi();
}
