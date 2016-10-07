package org.restcomm.connect.rvd.exceptions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RvdException extends Exception {

    public RvdException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RvdException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public RvdException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ExceptionResult getExceptionSummary() {
        return new ExceptionResult(getClass().getSimpleName(), getMessage());
    }

    public String asJson() {
        Gson gson = new Gson();
        JsonObject errorResponse = new JsonObject();
        ExceptionResult result = new ExceptionResult(getClass().getSimpleName(), getMessage());
        errorResponse.add("serverError", gson.toJsonTree(result));
        return gson.toJson(errorResponse);
    }

}
