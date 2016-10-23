package org.restcomm.connect.rvd.jsonvalidation;

import org.restcomm.connect.rvd.jsonvalidation.exceptions.ValidationFrameworkException;

public interface Validator {
    ValidationResult validate(String json) throws ValidationFrameworkException;
}
