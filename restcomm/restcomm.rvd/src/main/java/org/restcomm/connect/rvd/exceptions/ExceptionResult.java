package org.restcomm.connect.rvd.exceptions;

import org.restcomm.connect.rvd.jsonvalidation.ValidationResult;
import org.restcomm.connect.rvd.validation.ValidationReport;

/**
 * Represents the exception fields that makes sense to return in the response.
 * @author "Tsakiridis Orestis"
 *
 */
public class ExceptionResult {

    String className;
    String message;
    ValidationReport report;
    ValidationResult jsonSchemaReport; // used when the json schema validation library is used

    public ExceptionResult(String className, String message) {
        this.className = className;
        this.message = message;
    }

    public ExceptionResult(String className, String message, ValidationReport report) {
        this.className = className;
        this.message = message;
        this.report = report;
    }

    public ExceptionResult(String className, String message, ValidationResult jsonReport) {
        this.className = className;
        this.message = message;
        this.jsonSchemaReport = jsonReport;
    }
}
