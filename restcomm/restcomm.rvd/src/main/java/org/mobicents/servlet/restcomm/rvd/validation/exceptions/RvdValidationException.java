package org.mobicents.servlet.restcomm.rvd.validation.exceptions;

import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.validation.ValidationReport;

public class RvdValidationException extends RvdException {

    private ValidationReport report;

    public RvdValidationException(String message, ValidationReport report) {
        super(message);
    }

    public ValidationReport getReport() {
        return report;
    }

}
