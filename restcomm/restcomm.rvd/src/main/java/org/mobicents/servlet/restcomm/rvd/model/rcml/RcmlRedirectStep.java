package org.mobicents.servlet.restcomm.rvd.model.rcml;

public class RcmlRedirectStep extends RcmlStep {
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
