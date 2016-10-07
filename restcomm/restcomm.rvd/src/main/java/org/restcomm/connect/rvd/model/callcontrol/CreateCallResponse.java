package org.restcomm.connect.rvd.model.callcontrol;

public class CreateCallResponse {
    private CallControlStatus status;
    private CallControlAction action;
    private Object data;

    public CreateCallResponse() {
        // TODO Auto-generated constructor stub
    }

    public CreateCallResponse(CallControlStatus status, Object data) {
        super();
        this.status = status;
        this.data = data;
    }

    public CallControlStatus getStatus() {
        return status;
    }

    public CreateCallResponse setStatus(CallControlStatus status) {
        this.status = status;
        return this;
    }

    public Object getData() {
        return data;
    }

    public CreateCallResponse setData(Object data) {
        this.data = data;
        return this;
    }

    public CallControlAction getAction() {
        return action;
    }

    public CreateCallResponse setAction(CallControlAction action) {
        this.action = action;
        return this;
    }


}
