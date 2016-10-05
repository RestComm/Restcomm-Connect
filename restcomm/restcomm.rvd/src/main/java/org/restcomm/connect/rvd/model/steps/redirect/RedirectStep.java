package org.restcomm.connect.rvd.model.steps.redirect;

import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;

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

    public RcmlRedirectStep render(Interpreter interpreter ) {
        RcmlRedirectStep rcmlStep = new RcmlRedirectStep();
        rcmlStep.setUrl(interpreter.populateVariables(getUrl()));
        if ( getMethod() != null && !"".equals(getMethod()) )
            rcmlStep.setMethod(getMethod());
        return rcmlStep;
    }


}
