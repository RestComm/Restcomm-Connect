package org.restcomm.connect.rvd.validation.exceptions;

import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.validation.ValidationReport;

public class RvdValidationException extends RvdException {

    private ValidationReport report;

    public RvdValidationException(String message, ValidationReport report) {
        super(message);
    }

    public ValidationReport getReport() {
        return report;
    }

}
