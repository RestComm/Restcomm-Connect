package org.mobicents.servlet.restcomm.rvd.exceptions;

public class ExceptionResult {

    String className;
    String message;
    public ExceptionResult(String className, String message) {
        this.className = className;
        this.message = message;
    }

}
