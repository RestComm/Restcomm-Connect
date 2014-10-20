package org.mobicents.servlet.restcomm.provisioning.number.bandwidth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="DisconnectTelephoneNumberOrder")
@XmlAccessorType(XmlAccessType.FIELD)
public class DisconnectTelephoneNumberOrder {
    @XmlElement(name="Name")
    private String name;

    @XmlElement(name="DisconnectTelephoneNumberOrderType")
    private DisconnectTelephoneNumberOrderType disconnectTelephoneNumberOrderType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DisconnectTelephoneNumberOrderType getDisconnectTelephoneNumberOrderType() {
        return disconnectTelephoneNumberOrderType;
    }

    public void setDisconnectTelephoneNumberOrderType(DisconnectTelephoneNumberOrderType disconnectTelephoneNumberOrderType) {
        this.disconnectTelephoneNumberOrderType = disconnectTelephoneNumberOrderType;
    }
}
