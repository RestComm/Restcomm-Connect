package org.mobicents.servlet.restcomm.rvd.exceptions;

import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

public class ExceptionResult {

    String className;
    String message;
    ValidationReport report;

    public ExceptionResult(String className, String message) {
        this.className = className;
        this.message = message;
    }

    public ExceptionResult(String className, String message, ValidationReport report) {
        this.className = className;
        this.message = message;
        this.report = report;
    }

}
