package org.restcomm.connect.extension.api;

import org.apache.commons.configuration.Configuration;

public interface IExtensionCreateSmsSessionRequest extends IExtensionRequest {
    void setConfiguration(Configuration configuration);
    Configuration getConfiguration();
}
