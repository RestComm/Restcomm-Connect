package org.restcomm.connect.rvd.http;

import org.restcomm.connect.rvd.exceptions.ExceptionResult;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.jsonvalidation.exceptions.ValidationException;
import org.restcomm.connect.rvd.validation.ValidationReport;

import com.google.gson.Gson;

/**
 * A generic response model object
 * @author "Tsakiridis Orestis"
 *
 */
public class RvdResponse {

    public enum Status { OK, INVALID, ERROR, NOT_FOUND }
    Status rvdStatus; // ok - invalid - error
    ExceptionResult exception;
    ValidationReport report; // this may be reduntant data since there is always such a field inside ExceptionResult-exception
    Object payload; // for OK responses that are meant for data retrieval

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

    public RvdResponse setValidationException(ValidationException e) {
        if (e != null) {
            this.exception = new ExceptionResult( e.getClass().getSimpleName(), e.getMessage(), e.getValidationResult());
            rvdStatus = Status.INVALID;
        }
        return this;
    }

    public RvdResponse setException(RvdException e) {
        if (e != null) {
            exception = e.getExceptionSummary();
            //rvdStatus = Status.ERROR;
        }
        return this;
    }

    public RvdResponse setReport(ValidationReport report) {
        this.report = report;
        return this;
    }

    public RvdResponse setOkPayload(Object payload) {
        this.payload = payload;
        rvdStatus = Status.OK;
        return this;
    }

    public String asJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
