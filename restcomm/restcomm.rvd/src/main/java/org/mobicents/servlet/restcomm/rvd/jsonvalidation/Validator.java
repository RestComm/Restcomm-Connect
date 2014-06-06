package org.mobicents.servlet.restcomm.rvd.jsonvalidation;

import org.mobicents.servlet.restcomm.rvd.jsonvalidation.exceptions.ValidationFrameworkException;

public interface Validator {
    ValidationResult validate(String json) throws ValidationFrameworkException;
}
