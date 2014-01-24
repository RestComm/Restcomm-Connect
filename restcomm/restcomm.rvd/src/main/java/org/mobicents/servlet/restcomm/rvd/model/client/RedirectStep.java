package org.mobicents.servlet.restcomm.rvd.model.client;

public class RedirectStep extends Step {
    String url;
    String method;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
