package org.mobicents.servlet.restcomm.rvd.http;

import org.mobicents.servlet.restcomm.rvd.exceptions.ExceptionResult;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;

import com.google.gson.Gson;

public class RvdResponse {

    public enum Status { OK, INVALID, ERROR }
    Status rvdStatus; // ok - invalid - error
    ExceptionResult exception;

    public RvdResponse() {
        rvdStatus = Status.OK;
    }

    public RvdResponse(Status status) {
        rvdStatus =  status;
    }

    public RvdResponse setStatus(Status status) {
        rvdStatus = status;
        return this;
    }

    public RvdResponse setException(RvdException e) {
        if (e != null) {
        exception = e.getExceptionSummary();
        rvdStatus = Status.ERROR;
        }
        return this;
    }

    public String asJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }



}
