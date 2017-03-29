package org.restcomm.connect.extension.api;
import org.apache.commons.configuration.Configuration;
public class SessionExtensionResponse extends ExtensionResponse {

    public void setConfiguration(Configuration configuration){
        super.setObject(configuration);
    }
    public Configuration getConfiguration(){
        return (Configuration) super.getObject();
    }
}
