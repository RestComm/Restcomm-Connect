package org.mobicents.servlet.restcomm.rvd.validation;

import org.mobicents.servlet.restcomm.rvd.validation.exceptions.ValidationFrameworkException;

public interface Validator {
    ValidationResult validate(String json) throws ValidationFrameworkException;
}
