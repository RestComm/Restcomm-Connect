package org.restcomm.connect.extension.api;

import org.restcomm.connect.commons.dao.Sid;

public interface IExtensionFeatureAccessRequest extends IExtensionRequest{

    void setAccountSid (Sid accountSid);

    String getDestinationNumber();

    void setDestinationNumber(String destinationNumber);


}
