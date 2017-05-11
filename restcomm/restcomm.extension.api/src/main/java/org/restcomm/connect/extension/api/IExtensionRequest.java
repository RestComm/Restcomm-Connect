package org.restcomm.connect.extension.api;

import org.restcomm.connect.commons.telephony.CreateCallType;

public interface IExtensionRequest {
    CreateCallType getType();
}
