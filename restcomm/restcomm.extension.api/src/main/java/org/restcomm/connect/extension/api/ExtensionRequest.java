package org.restcomm.connect.extension.api;
import org.apache.commons.configuration.Configuration;

public class ExtensionRequest {
    private Object payload;
    private Configuration configuration;

    public ExtensionRequest() {}

    public Object getObject() {
        return payload;
    }

    public void setObject(Object object) {
        this.payload = object;
    }

    public void setConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
        this.configuration = configuration;
    }
    public Configuration getConfiguration() {
        // TODO Auto-generated method stub
        return this.configuration;
    }

}
